(() => {
 const cfg=window.TATBIQATI_CONFIG||{}, $=id=>document.getElementById(id);
 const loginPanel=$('login-panel'), dashboard=$('dashboard');
 let client=null, user=null;
 const setStatus=(id,msg,type='')=>{const el=$(id);el.textContent=msg;el.className='admin-status '+type;};
 const safe=(s='')=>String(s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
 function init(){
   if(!cfg.supabaseUrl||!cfg.supabaseAnonKey||!window.supabase){setStatus('auth-status','لوحة الإدارة غير مفعلة بعد. راجع ملف SETUP-AR.md لإعداد Supabase بأمان.','error');$('login-btn').disabled=true;return;}
   client=window.supabase.createClient(cfg.supabaseUrl,cfg.supabaseAnonKey);
   client.auth.onAuthStateChange((_event,session)=>{user=session?.user||null;refreshAccess();});
   client.auth.getSession().then(({data})=>{user=data.session?.user||null;refreshAccess();});
 }
 async function refreshAccess(){
   if(!user){loginPanel.classList.remove('admin-hidden');dashboard.classList.add('admin-hidden');return;}
   const {data,error}=await client.from('app_admins').select('user_id').eq('user_id',user.id).maybeSingle();
   if(error||!data){loginPanel.classList.remove('admin-hidden');dashboard.classList.add('admin-hidden');setStatus('auth-status','تم تسجيل الدخول، لكن هذا الحساب غير مخوّل كمدير. أضف user_id إلى app_admins من لوحة Supabase.','error');return;}
   loginPanel.classList.add('admin-hidden');dashboard.classList.remove('admin-hidden');await loadApps();
 }
 $('login-btn').addEventListener('click',async()=>{if(!client)return;const email=$('email').value.trim(),password=$('password').value;if(!email||!password){setStatus('auth-status','أدخل البريد وكلمة المرور.','error');return;}setStatus('auth-status','جارٍ تسجيل الدخول…');const {error}=await client.auth.signInWithPassword({email,password});if(error)setStatus('auth-status','تعذر تسجيل الدخول: '+error.message,'error');});
 $('logout-btn').addEventListener('click',async()=>{if(client)await client.auth.signOut();user=null;refreshAccess();});
 $('name').addEventListener('input',()=>{if(!$('slug').dataset.edited){$('slug').value=$('name').value.trim().toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/[^a-z0-9]+/g,'-').replace(/^-|-$/g,'');}});
 $('slug').addEventListener('input',()=>{$('slug').dataset.edited='1';$('slug').value=$('slug').value.toLowerCase().replace(/[^a-z0-9-]/g,'');});
 $('app-form').addEventListener('reset',()=>{delete $('slug').dataset.edited;setStatus('upload-status','');});
 $('app-form').addEventListener('submit',async e=>{
   e.preventDefault();if(!client||!user)return;
   const name=$('name').value.trim(),slug=$('slug').value.trim(),description=$('description').value.trim(),apk=$('apk').files[0],icon=$('icon').files[0],category=$('category').value;
   if(!/^[a-z0-9-]+$/.test(slug)){setStatus('upload-status','معرّف الرابط يجب أن يحتوي على أحرف إنجليزية صغيرة وأرقام وشرطات فقط.','error');return;}
   if(!apk||!apk.name.toLowerCase().endsWith('.apk')){setStatus('upload-status','اختر ملف APK صالحاً.','error');return;}
   if(apk.size>100*1024*1024){setStatus('upload-status','حجم APK يتجاوز الحد المسموح (100 MB).','error');return;}
   if(icon&&(!icon.type.startsWith('image/')||icon.size>2*1024*1024)){setStatus('upload-status','الأيقونة يجب أن تكون صورة وحجمها أقل من 2 MB.','error');return;}
   const btn=$('save-app');btn.disabled=true;setStatus('upload-status','جارٍ رفع الملفات وحفظ بيانات التطبيق…');
   try{
     const base=slug+'-'+Date.now(),apkPath='apks/'+base+'.apk';
     const up=await client.storage.from('app-files').upload(apkPath,apk,{contentType:'application/vnd.android.package-archive',upsert:false});
     if(up.error)throw up.error;
     let iconUrl='';
     if(icon){const ext=(icon.name.split('.').pop()||'png').toLowerCase().replace(/[^a-z0-9]/g,'')||'png',p='icons/'+base+'.'+ext;const iu=await client.storage.from('app-files').upload(p,icon,{contentType:icon.type,upsert:false});if(iu.error)throw iu.error;iconUrl=client.storage.from('app-files').getPublicUrl(p).data.publicUrl;}
     const downloadUrl=client.storage.from('app-files').getPublicUrl(apkPath).data.publicUrl;
     const row={name,slug,category,category_label:({free:'مجانية',plus:'بلس',modified:'معدلة'})[category],description,version:$('version').value.trim()||'1.0',size_label:$('size-label').value.trim()||((apk.size/1048576).toFixed(1)+' MB'),icon_url:iconUrl,download_url:downloadUrl,status:$('status').value,featured:$('featured').checked,uploaded_by:user.id,storage_path:apkPath};
     const {error}=await client.from('apps').insert(row);if(error)throw error;
     setStatus('upload-status','تم رفع التطبيق وحفظ بياناته بنجاح. '+($('status').value==='published'?'سيظهر في الصفحة العامة.':'حُفظ كمسودة ولن يظهر للعامة بعد.'),'success');
     $('app-form').reset();await loadApps();
   }catch(err){console.error(err);setStatus('upload-status','تعذر إكمال الرفع: '+(err.message||'خطأ غير معروف')+' — راجع سياسات Supabase والإعدادات.','error');}
   finally{btn.disabled=false;}
 });
 async function loadApps(){
   const {data,error}=await client.from('apps').select('id,name,slug,category,version,status,created_at').order('created_at',{ascending:false});
   if(error){setStatus('list-status','تعذر تحميل القائمة: '+error.message,'error');return;}
   $('admin-app-list').innerHTML=(data||[]).map(a=>'<div class="admin-app-row"><div class="app-icon">'+safe((a.name||'ت').charAt(0))+'</div><div><b>'+safe(a.name)+'</b><small>'+safe(a.slug)+' · '+safe(a.version||'1.0')+' · '+(a.status==='published'?'منشور':'مسودة')+'</small></div><button class="button button-danger" data-delete="'+safe(a.id)+'" type="button">حذف</button></div>').join('')||'<p class="file-hint">لا توجد تطبيقات مسجلة حتى الآن.</p>';
   $('admin-app-list').querySelectorAll('[data-delete]').forEach(btn=>btn.addEventListener('click',async()=>{if(!confirm('هل تريد حذف سجل هذا التطبيق من المكتبة؟'))return;const {error}=await client.from('apps').delete().eq('id',btn.dataset.delete);if(error){setStatus('list-status','تعذر الحذف: '+error.message,'error');return;}setStatus('list-status','تم حذف سجل التطبيق.','success');await loadApps();}));
 }
 init();
})();