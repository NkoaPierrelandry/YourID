const BRAND = '#00A86B';
const chartInstances = {};

function setupChartDefaults() {
  if (typeof Chart === 'undefined') return false;
  Chart.defaults.font.family = "'Satoshi', 'Inter', system-ui, sans-serif";
  Chart.defaults.color = '#64748B';
  return true;
}

function mountChart(canvasId, config) {
  if (chartInstances[canvasId]) chartInstances[canvasId].destroy();
  const ctx = document.getElementById(canvasId).getContext('2d');
  chartInstances[canvasId] = new Chart(ctx, config);
  return chartInstances[canvasId];
}

function dateKey(d) {
  return d.toLocaleDateString('en-CA'); // AAAA-MM-JJ, en heure locale
}

function buildDailySeries(orders, days) {
  const buckets = new Map();
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  for (let i = days - 1; i >= 0; i--) {
    const d = new Date(today);
    d.setDate(today.getDate() - i);
    buckets.set(dateKey(d), { date: d, revenue: 0, count: 0 });
  }

  orders
    .filter((o) => o.status === 'COMPLETED' && o.completedAt)
    .forEach((o) => {
      const bucket = buckets.get(dateKey(new Date(o.completedAt)));
      if (bucket) {
        bucket.revenue += Number(o.sellerRevenue);
        bucket.count += 1;
      }
    });

  const list = Array.from(buckets.values());
  return {
    labels: list.map((b) => b.date.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' })),
    revenue: list.map((b) => b.revenue),
    count: list.map((b) => b.count),
    totalRevenue: list.reduce((s, b) => s + b.revenue, 0),
    totalCount: list.reduce((s, b) => s + b.count, 0),
  };
}

const compactNumber = (v) => new Intl.NumberFormat('fr-FR', { notation: 'compact' }).format(v);

function revenueLineConfig(series, currency) {
  return {
    type: 'line',
    data: {
      labels: series.labels,
      datasets: [{
        label: 'Revenus',
        data: series.revenue,
        borderColor: BRAND,
        borderWidth: 2.5,
        tension: 0.35,
        fill: true,
        pointRadius: 0,
        pointHoverRadius: 5,
        pointHoverBackgroundColor: BRAND,
        // Dégradé vert -> transparent sous la courbe
        backgroundColor: (context) => {
          const { ctx, chartArea } = context.chart;
          if (!chartArea) return null;
          const g = ctx.createLinearGradient(0, chartArea.top, 0, chartArea.bottom);
          g.addColorStop(0, 'rgba(0,168,107,0.25)');
          g.addColorStop(1, 'rgba(0,168,107,0)');
          return g;
        },
      }],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: { mode: 'index', intersect: false },
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label: (c) => formatPrice(c.parsed.y, currency),
            afterLabel: (c) => `${series.count[c.dataIndex]} vente(s)`,
          },
        },
      },
      scales: {
        x: { grid: { display: false }, ticks: { maxTicksLimit: 8 } },
        y: { beginAtZero: true, grid: { color: 'rgba(148,163,184,0.15)' }, ticks: { callback: compactNumber } },
      },
    },
  };
}

const STATUS_CHART = {
  COMPLETED: { label: 'Complétées', color: BRAND },
  PENDING: { label: 'En attente', color: '#F59E0B' },
  REFUNDED: { label: 'Remboursées', color: '#EF4444' },
};

function statusDoughnutConfig(orders) {
  const keys = Object.keys(STATUS_CHART);
  return {
    type: 'doughnut',
    data: {
      labels: keys.map((k) => STATUS_CHART[k].label),
      datasets: [{
        data: keys.map((k) => orders.filter((o) => o.status === k).length),
        backgroundColor: keys.map((k) => STATUS_CHART[k].color),
        borderWidth: 0,
      }],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      cutout: '68%',
      plugins: { legend: { position: 'bottom', labels: { usePointStyle: true, boxWidth: 8 } } },
    },
  };
}

function topProductsBarConfig(products, currency) {
  const top = [...products].sort((a, b) => b.totalRevenue - a.totalRevenue).slice(0, 5);
  return {
    type: 'bar',
    data: {
      labels: top.map((p) => (p.title.length > 24 ? p.title.slice(0, 24) + '…' : p.title)),
      datasets: [{ data: top.map((p) => Number(p.totalRevenue)), backgroundColor: BRAND, borderRadius: 8, barThickness: 18 }],
    },
    options: {
      indexAxis: 'y',
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { display: false }, tooltip: { callbacks: { label: (c) => formatPrice(c.parsed.x, currency) } } },
      scales: {
        x: { beginAtZero: true, grid: { color: 'rgba(148,163,184,0.15)' }, ticks: { callback: compactNumber } },
        y: { grid: { display: false } },
      },
    },
  };
}