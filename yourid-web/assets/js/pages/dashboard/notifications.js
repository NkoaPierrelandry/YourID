(async function init() {
  await initDashboardShell(null); // aucun lien de sidebar actif pour cette page
  await load();
})();

async function load() {
  const list = document.getElementById('notif-list');
  try {
    const { data } = await api.get('/notifications?size=50&sort=createdAt,desc');
    const items = data.content;

    if (items.length === 0) {
      list.innerHTML = `<div class="empty-state">Aucune notification</div>`;
      return;
    }

    list.innerHTML = items.map((n) => `
      <div class="notif-row" data-id="${n.id}" style="padding:16px 20px; border-bottom:1px solid var(--border); display:flex; gap:12px; align-items:flex-start; ${n.read ? 'opacity:0.6;' : ''} cursor:pointer;">
        <div style="width:8px; height:8px; border-radius:50%; background:${n.read ? 'transparent' : 'var(--brand)'}; margin-top:6px; flex-shrink:0;"></div>
        <div>
          <p style="margin:0; font-weight:600; font-size:14px;">${escapeHtml(n.title)}</p>
          <p style="margin:2px 0 0; font-size:13px; color:var(--text-muted);">${escapeHtml(n.message)}</p>
          <p style="margin:4px 0 0; font-size:11px; color:#94A3B8;">${formatDate(n.createdAt)}</p>
        </div>
      </div>
    `).join('');

    list.querySelectorAll('.notif-row').forEach((row) => {
      row.addEventListener('click', async () => {
        await api.patch(`/notifications/${row.dataset.id}/read`);
        row.style.opacity = '0.6';
        row.querySelector('div').style.background = 'transparent';
      });
    });
  } catch (e) {
    list.innerHTML = `<div class="empty-state">Impossible de charger les notifications</div>`;
  }
}
