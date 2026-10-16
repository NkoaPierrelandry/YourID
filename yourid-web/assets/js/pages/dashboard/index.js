const STATUS_BADGE = {
  PENDING: { cls: 'badge-warning', label: 'En attente' },
  COMPLETED: { cls: 'badge-success', label: 'Complétée' },
  REFUNDED: { cls: 'badge-danger', label: 'Remboursée' },
};

(async function init() {
  await initDashboardShell('/dashboard/index.html');

  const user = Auth.getUser();
  document.getElementById('greeting').textContent = `Bonjour, ${user.firstName || ''}`;

  try {
    const { data: stats } = await api.get('/analytics/dashboard');
    renderKpis(stats);
    loadOrders();
    loadTopProducts();
  } catch (err) {
    if (err.status === 404 || err.status === 400) {
      document.getElementById('no-store-banner').style.display = 'block';
      document.getElementById('kpi-grid').style.display = 'none';
      document.querySelectorAll('.dash-content > section').forEach((s) => (s.style.display = 'none'));
    } else {
      showToast('Erreur lors du chargement des statistiques.');
    }
  }
})();

function renderKpis(stats) {
  const grid = document.getElementById('kpi-grid');
  const cards = [
    { label: 'Revenu total', value: formatPrice(stats.totalRevenue) },
    { label: 'Solde disponible', value: formatPrice(stats.balance) },
    { label: 'Ventes totales', value: stats.totalSales },
    { label: 'Commandes', value: stats.orderCount },
  ];
  grid.innerHTML = cards.map((c) => `
    <div class="stat-card">
      <span class="kpi-label">${c.label}</span>
      <span class="kpi-value">${c.value}</span>
    </div>
  `).join('');

  if (stats.pendingWithdrawalsCount > 0) {
    const note = document.createElement('p');
    note.style.cssText = 'font-size:13px; color:var(--text-muted); margin-top:12px;';
    note.innerHTML = `⏳ ${stats.pendingWithdrawalsCount} demande(s) de retrait en attente - <a href="/dashboard/payouts.html" class="auth-link">voir</a>`;
    grid.insertAdjacentElement('afterend', note);
  }
}

async function loadOrders() {
  const wrap = document.getElementById('recent-orders-wrap');
  try {
    const { data } = await api.get('/orders/me?size=500&sort=createdAt,desc');
    const orders = data.content;

    renderRevenueChart(orders);
    renderRecentOrders(orders.slice(0, 8), wrap);
  } catch (e) {
    wrap.innerHTML = `<div class="empty-state"><p>Impossible de charger les commandes</p></div>`;
    document.getElementById('revenue-chart-note').textContent = 'Impossible de charger les données.';
  }
}

function renderRevenueChart(orders) {
  const note = document.getElementById('revenue-chart-note');
  if (!setupChartDefaults()) {
    note.textContent = "Chart.js n'a pas pu être chargé (connexion internet requise).";
    return;
  }
  const currency = orders[0]?.currency || 'XOF';
  const series = buildDailySeries(orders, 30);
  mountChart('revenue-chart', revenueLineConfig(series, currency));

  note.textContent = series.totalCount === 0
    ? 'Aucune vente complétée sur les 30 derniers jours.'
    : `${series.totalCount} vente(s) · ${formatPrice(series.totalRevenue, currency)} net sur 30 jours`;
}

async function loadTopProducts() {
  const container = document.getElementById('top-products-list');
  try {
    const { data } = await api.get('/products/me?size=50');
    const products = [...data.content].sort((a, b) => b.totalSales - a.totalSales).slice(0, 5);

    if (products.length === 0) {
      container.innerHTML = `<div class="empty-state"><p>Aucun produit pour l'instant</p></div>`;
      return;
    }
    container.innerHTML = products.map((p) => `
      <div style="display:flex; align-items:center; justify-content:space-between; padding:10px 0; border-bottom:1px solid var(--border);">
        <div style="min-width:0;">
          <p style="font-size:14px; font-weight:600; margin:0; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;">${escapeHtml(p.title)}</p>
          <p style="font-size:12px; color:var(--text-muted); margin:2px 0 0;">${p.totalSales} vente(s)</p>
        </div>
        <span style="font-size:13px; font-weight:600; flex-shrink:0; margin-left:12px;">${formatPrice(p.totalRevenue, p.currency)}</span>
      </div>
    `).join('');
  } catch (e) {
    container.innerHTML = `<div class="empty-state"><p>Impossible de charger les produits</p></div>`;
  }
}

function renderRecentOrders(orders, wrap) {
  if (orders.length === 0) {
    wrap.innerHTML = `<div class="empty-state"><p>Aucune commande pour l'instant</p></div>`;
    return;
  }

  wrap.innerHTML = `
    <table class="dash-table">
      <thead><tr><th>Commande</th><th>Client</th><th>Montant</th><th>Statut</th><th>Date</th></tr></thead>
      <tbody>
        ${orders.map((o) => {
          const badge = STATUS_BADGE[o.status] || { cls: 'badge-neutral', label: o.status };
          return `
            <tr class="clickable" data-order='${escapeHtml(JSON.stringify(o))}'>
              <td style="font-weight:600;">${o.orderNumber}</td>
              <td>${escapeHtml(o.customerName)}</td>
              <td>${formatPrice(o.total, o.currency)}</td>
              <td><span class="badge ${badge.cls}">${badge.label}</span></td>
              <td>${formatDate(o.createdAt)}</td>
            </tr>`;
        }).join('')}
      </tbody>
    </table>
  `;

  wrap.querySelectorAll('tr.clickable').forEach((row) => {
    row.addEventListener('click', () => openOrderDialog(JSON.parse(row.dataset.order)));
  });
}

function openOrderDialog(order) {
  const dialog = document.getElementById('order-dialog');
  const badge = STATUS_BADGE[order.status] || { cls: 'badge-neutral', label: order.status };

  document.getElementById('order-dialog-content').innerHTML = `
    <div style="display:flex; justify-content:space-between; margin-bottom:12px;">
      <span style="font-weight:600;">${order.orderNumber}</span>
      <span class="badge ${badge.cls}">${badge.label}</span>
    </div>
    <p style="font-size:13px; color:var(--text-muted); margin:0 0 16px;">${escapeHtml(order.customerName)} - ${escapeHtml(order.customerEmail)}</p>
    <div style="border-top:1px solid var(--border); border-bottom:1px solid var(--border); padding:12px 0; margin-bottom:12px;">
      ${order.items.map((item) => `
        <div style="display:flex; justify-content:space-between; font-size:14px; padding:4px 0;">
          <span>${escapeHtml(item.title)} X ${item.quantity}</span>
          <span>${formatPrice(item.price * item.quantity, order.currency)}</span>
        </div>
      `).join('')}
    </div>
    <div style="display:flex; justify-content:space-between; font-weight:700;">
      <span>Total</span><span>${formatPrice(order.total, order.currency)}</span>
    </div>
  `;
  dialog.showModal();
}

document.getElementById('close-order-dialog').addEventListener('click', () => {
  document.getElementById('order-dialog').close();
});