-- تطبيقاتي: مخطط قاعدة البيانات وسياسات الوصول
-- نفّذ هذا الملف في Supabase SQL Editor بعد إنشاء مشروع جديد.
create extension if not exists pgcrypto;

create table if not exists public.app_admins (
  user_id uuid primary key references auth.users(id) on delete cascade,
  created_at timestamptz not null default now()
);

create table if not exists public.apps (
  id uuid primary key default gen_random_uuid(),
  name text not null check (char_length(name) between 1 and 90),
  slug text not null unique check (slug ~ '^[a-z0-9-]+$'),
  category text not null check (category in ('free','plus','modified')),
  category_label text not null,
  description text not null check (char_length(description) between 1 and 500),
  version text not null default '1.0',
  size_label text not null default '—',
  icon_url text not null default '',
  download_url text not null,
  storage_path text not null,
  status text not null default 'draft' check (status in ('draft','published')),
  featured boolean not null default false,
  uploaded_by uuid references auth.users(id) on delete set null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists apps_public_catalog_idx on public.apps(status, featured desc, created_at desc);
alter table public.apps enable row level security;
alter table public.app_admins enable row level security;

create or replace function public.is_app_admin()
returns boolean language sql stable security definer set search_path = public
as $$ select exists (select 1 from public.app_admins where user_id = auth.uid()); $$;

-- Visitors can only read published apps. Admins can manage app records.
drop policy if exists "Public can read published apps" on public.apps;
create policy "Public can read published apps" on public.apps for select using (status = 'published' or public.is_app_admin());
drop policy if exists "Admins can insert apps" on public.apps;
create policy "Admins can insert apps" on public.apps for insert with check (public.is_app_admin() and uploaded_by = auth.uid());
drop policy if exists "Admins can update apps" on public.apps;
create policy "Admins can update apps" on public.apps for update using (public.is_app_admin()) with check (public.is_app_admin());
drop policy if exists "Admins can delete apps" on public.apps;
create policy "Admins can delete apps" on public.apps for delete using (public.is_app_admin());

-- Admin membership is managed only from the Supabase dashboard / SQL Editor.
drop policy if exists "Admins can read own admin membership" on public.app_admins;
create policy "Admins can read own admin membership" on public.app_admins for select using (user_id = auth.uid());

-- Private storage bucket is recommended. The application uses signed links for private files;
-- if you choose a public bucket, anyone with a file URL can download its contents.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('app-files','app-files',true,104857600,array['application/vnd.android.package-archive','image/png','image/jpeg','image/webp','image/svg+xml'])
on conflict (id) do update set file_size_limit = excluded.file_size_limit, allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists "Admins can upload app files" on storage.objects;
create policy "Admins can upload app files" on storage.objects for insert to authenticated with check (bucket_id = 'app-files' and public.is_app_admin());
drop policy if exists "Admins can update app files" on storage.objects;
create policy "Admins can update app files" on storage.objects for update to authenticated using (bucket_id = 'app-files' and public.is_app_admin()) with check (bucket_id = 'app-files' and public.is_app_admin());
drop policy if exists "Public can download app files" on storage.objects;
create policy "Public can download app files" on storage.objects for select using (bucket_id = 'app-files');
drop policy if exists "Admins can delete app files" on storage.objects;
create policy "Admins can delete app files" on storage.objects for delete to authenticated using (bucket_id = 'app-files' and public.is_app_admin());

-- After creating the admin account under Authentication > Users, run:
-- insert into public.app_admins (user_id) values ('PASTE-AUTH-USER-UUID-HERE');
