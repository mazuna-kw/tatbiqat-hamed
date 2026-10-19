(() => {
  const config = window.TATBIQATI_CONFIG || {};
  const grid = document.getElementById('app-grid');
  const search = document.getElementById('app-search');
  const count = document.getElementById('result-count');
  const empty = document.getElementById('empty-state');
  const reset = document.getElementById('reset-search');
  let allApps = [];
  let activeFilter = 'all';
  const esc = (s='') => String(s).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const categoryLabel = c => ({free:'مجانية',plus:'بلس',modified:'معدلة'}[c] || 'تطبيق');
  function card(app) {
    const ready = app.status === 'published' && app.download_url;
    const icon = app.icon_url ? '<img src="'+esc(app.icon_url)+'" alt="" loading="lazy">' : '<span>'+esc((app.name||'ت').trim().charAt(0)||'ت')+'</span>';
    return '<article class="app-card"><div class="app-card-top"><div class="app-icon">'+icon+'</div><span class="app-category">'+esc(app.category_label||categoryLabel(app.category))+'</span></div><h3>'+esc(app.name)+'</h3><p class="app-desc">'+esc(app.description||'تفاصيل هذا التطبيق ستضاف قريباً.')+'</p><div class="app-meta"><span>الإصدار '+esc(app.version||'—')+'</span><span>الحجم '+esc(app.size_label||'—')+'</span></div><div class="app-card-bottom"><span class="app-status '+(ready?'':'pending')+'">'+(ready?'متاح للتنزيل':'قريباً')+'</span>'+(ready?'<a class="download-btn" href="'+esc(app.download_url)+'" rel="nofollow" target="_blank" aria-label="تنزيل '+esc(app.name)+'">تنزيل <span>↓</span></a>':'<button class="download-btn disabled" disabled>غير متاح بعد</button>')+'</div></article>';
  }
  function render() {
    const term = (search.value||'').trim().toLocaleLowerCase('ar');
    const visible = allApps.filter(a => (activeFilter==='all'||a.category===activeFilter) && [a.name,a.description,a.category_label,categoryLabel(a.category)].join(' ').toLocaleLowerCase('ar').includes(term));
    grid.innerHTML = visible.map(card).join('');
    empty.hidden = visible.length > 0;
    grid.hidden = visible.length === 0;
    count.textContent = 'عرض '+visible.length+' من '+allApps.length+' تطبيق';
  }
  async function load() {
    if (config.supabaseUrl && config.supabaseAnonKey && window.supabase) {
      try {
        const client = window.supabase.createClient(config.supabaseUrl, config.supabaseAnonKey);
        const {data,error} = await client.from('apps').select('id,name,slug,category,category_label,description,version,size_label,icon_url,download_url,status,featured,created_at').eq('status','published').order('featured',{ascending:false}).order('created_at',{ascending:false});
        if (error) throw error;
        allApps = data || [];
      } catch (e) {
        console.error('Catalog load failed:', e);
        allApps = [];
        count.textContent = 'تعذر الاتصال بالمكتبة؛ حاول تحديث الصفحة لاحقاً.';
      }
    } else allApps = [];
    render();
  }
  document.querySelectorAll('.filter').forEach(btn => btn.addEventListener('click', () => {
    document.querySelectorAll('.filter').forEach(b => b.classList.toggle('active', b===btn));
    activeFilter = btn.dataset.filter; render();
  }));
  search.addEventListener('input', render);
  reset.addEventListener('click', () => { search.value=''; activeFilter='all'; document.querySelectorAll('.filter').forEach(b=>b.classList.toggle('active',b.dataset.filter==='all')); render(); });
  document.getElementById('year').textContent = new Date().getFullYear();
  load();
})();