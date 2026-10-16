let orders = [];
let products = [];
let period = 30;

(async function init() {
  await initDashboardShell('/dashboard/analytics.html');

  try {
    const { data: stats } = await api.get('/analytics/dashboard');
    renderKpis(stats);
  } catch (e) {
    document.getElementById('kpi-grid').innerHTML = `<div class="empty-state">Impossible de charger les statistiques</div>`;
  }

  try {
    const [ordersRes, productsRes] = await Promise.all([
      api.get('/orders/me?size=500&sort=createdAt,desc'),
      api.get('/products/me?size=100'),
    ]);
    orders = ordersRes.data.content;
    products = productsRes.data.content;
  } catch (e) {
    showToast('Impossible de charger les données des graphiques.');
  }

  renderRanking();
  renderCharts();

  document.querySelectorAll('#period-pills .pill').forEach((btn) => {
    btn.addEventListener('click', () => {
      period = Number(btn.dataset.days);
      document.querySelectorAll('#period-pills .pill').forEach((b) => b.classList.toggle('active', b === btn));
      renderRevenueChart();
    });
  });
})();

function renderKpis(stats) {
  const cards = [
    { label: 'Revenu total', value: formatPrice(stats.totalRevenue) },
    { label: 'Solde disponible', value: formatPrice(stats.balance) },
    { label: 'Ventes totales', value: stats.totalSales },
    { label: 'Produits', value: stats.productCount },
  ];
  document.getElementById('kpi-grid').innerHTML = cards.map((c) => `
    <div class="stat-card"><span class="kpi-label">${c.label}</span><span class="kpi-value">${c.value}</span></div>
  `).join('');
}

function renderCharts() {
  if (!setupChartDefaults()) {
    document.getElementById('period-summary').textContent = "Chart.js n'a pas pu être chargé (connexion internet requise).";
    return;
  }
  renderRevenueChart();

  const currency = products[0]?.currency || 'XOF';
  mountChart('products-chart', topProductsBarConfig(products, currency));
  mountChart('status-chart', statusDoughnutConfig(orders));
}

function renderRevenueChart() {
  if (typeof Chart === 'undefined') return;
  const currency = orders[0]?.currency || 'XOF';
  const series = buildDailySeries(orders, period);
  mountChart('revenue-chart', revenueLineConfig(series, currency));

  document.getElementById('period-summary').textContent = series.totalCount === 0
    ? `Aucune vente complétée sur les ${period} derniers jours.`
    : `${series.totalCount} vente(s) · ${formatPrice(series.totalRevenue, currency)} net sur ${period} jours`;
}

function renderRanking() {
  const sorted = [...products].sort((a, b) => b.totalRevenue - a.totalRevenue);
  const tbody = document.getElementById('ranking-tbody');

  if (sorted.length === 0) {
    tbody.innerHTML = `<tr><td colspan="6" class="empty-state">Aucun produit pour l'instant</td></tr>`;
    return;
  }

  tbody.innerHTML = sorted.map((p, i) => {
    const conversion = p.viewCount > 0 ? ((p.totalSales / p.viewCount) * 100).toFixed(1) + '%' : '—';
    return `
      <tr>
        <td style="font-weight:700; color:var(--text-muted);">#${i + 1}</td>
        <td style="font-weight:600;">${escapeHtml(p.title)}</td>
        <td>${p.totalSales}</td>
        <td style="font-weight:600;">${formatPrice(p.totalRevenue, p.currency)}</td>
        <td>${p.viewCount}</td>
        <td>${conversion}</td>
      </tr>`;
  }).join('');
}