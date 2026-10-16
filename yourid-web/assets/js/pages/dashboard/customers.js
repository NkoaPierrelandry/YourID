let customers = [];
let searchTerm = '';
let statusFilter = 'all';

(async function init() {
  await initDashboardShell('/dashboard/customers.html');
  document.getElementById('search-icon').innerHTML = Icons.search(16);

  try {
    const { data } = await api.get('/orders/me?size=500');
    customers = deriveCustomers(data.content);
    renderKpis();
    renderPills();
    renderTable();
  } catch (e) {
    document.getElementById('customers-tbody').innerHTML = `<tr><td colspan="6" class="empty-state">Impossible de charger les commandes</td></tr>`;
  }

  document.getElementById('search-input').addEventListener('input', (e) => {
    searchTerm = e.target.value.trim().toLowerCase();
    renderTable();
  });
  document.getElementById('export-btn').addEventListener('click', exportCsv);
})();

function deriveCustomers(orders) {
  const completed = orders.filter((o) => o.status === 'COMPLETED');
  const byEmail = new Map();

  for (const order of completed) {
    const key = order.customerEmail;
    if (!byEmail.has(key)) {
      byEmail.set(key, { name: order.customerName, email: key, orders: [] });
    }
    byEmail.get(key).orders.push(order);
  }

  return Array.from(byEmail.values()).map((c) => {
    const totalOrders = c.orders.length;
    const totalSpent = c.orders.reduce((sum, o) => sum + Number(o.total), 0);
    const lastPurchase = c.orders.reduce((latest, o) => (!latest || o.completedAt > latest ? o.completedAt : latest), null);
    const currency = c.orders[0]?.currency || 'XOF';

    // Produit favori : le titre d'article le plus fréquent sur ses commandes.
    const titleCounts = {};
    c.orders.forEach((o) => o.items.forEach((i) => { titleCounts[i.title] = (titleCounts[i.title] || 0) + 1; }));
    const favoriteProduct = Object.entries(titleCounts).sort((a, b) => b[1] - a[1])[0]?.[0] || '—';

    const status = totalOrders >= 3 ? 'vip' : totalOrders === 2 ? 'regular' : 'new';

    return { ...c, totalOrders, totalSpent, lastPurchase, favoriteProduct, status, currency };
  }).sort((a, b) => b.totalSpent - a.totalSpent);
}

const STATUS_LABEL = { vip: 'VIP', regular: 'Fidèle', new: 'Nouveau' };
const STATUS_CLASS = { vip: 'badge-success', regular: 'badge-warning', new: 'badge-neutral' };

function renderKpis() {
  document.getElementById('kpi-total').textContent = customers.length;
  document.getElementById('kpi-vip').textContent = customers.filter((c) => c.status === 'vip').length;
  const avg = customers.length
    ? customers.reduce((s, c) => s + c.totalSpent / c.totalOrders, 0) / customers.length
    : 0;
  document.getElementById('kpi-avg').textContent = formatPrice(avg, customers[0]?.currency);
}

function renderPills() {
  const wrap = document.getElementById('status-pills');
  const options = [['all', 'Tous'], ['vip', 'VIP'], ['regular', 'Fidèles'], ['new', 'Nouveaux']];
  wrap.innerHTML = options.map(([key, label]) =>
    `<button class="pill ${statusFilter === key ? 'active' : ''}" data-status="${key}">${label}</button>`
  ).join('');
  wrap.querySelectorAll('.pill').forEach((btn) => {
    btn.addEventListener('click', () => { statusFilter = btn.dataset.status; renderPills(); renderTable(); });
  });
}

function getFiltered() {
  return customers.filter((c) => {
    const matchesStatus = statusFilter === 'all' || c.status === statusFilter;
    const q = searchTerm;
    const matchesSearch = !q || c.name.toLowerCase().includes(q) || c.email.toLowerCase().includes(q);
    return matchesStatus && matchesSearch;
  });
}

function renderTable() {
  const filtered = getFiltered();
  document.getElementById('export-btn').disabled = filtered.length === 0;
  const tbody = document.getElementById('customers-tbody');

  if (filtered.length === 0) {
    tbody.innerHTML = `<tr><td colspan="6" class="empty-state">Aucun client trouvé</td></tr>`;
    return;
  }

  tbody.innerHTML = filtered.map((c) => `
    <tr>
      <td>
        <p style="margin:0; font-weight:600; font-size:13px;">${escapeHtml(c.name)}</p>
        <p style="margin:0; font-size:11px; color:var(--text-muted);">${escapeHtml(c.email)}</p>
      </td>
      <td>${c.totalOrders}</td>
      <td style="font-weight:600;">${formatPrice(c.totalSpent, c.currency)}</td>
      <td><span class="badge ${STATUS_CLASS[c.status]}">${STATUS_LABEL[c.status]}</span></td>
      <td>${formatDate(c.lastPurchase)}</td>
      <td style="max-width:160px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;" title="${escapeHtml(c.favoriteProduct)}">${escapeHtml(c.favoriteProduct)}</td>
    </tr>
  `).join('');
}

function exportCsv() {
  const filtered = getFiltered();
  if (filtered.length === 0) return;

  const headers = ['Nom', 'Email', 'Commandes', 'Total dépensé', 'Statut', 'Dernier achat', 'Produit favori'];
  const rows = filtered.map((c) => [
    c.name, c.email, c.totalOrders, c.totalSpent, STATUS_LABEL[c.status], formatDate(c.lastPurchase), c.favoriteProduct,
  ]);

  const csv = [headers, ...rows]
    .map((row) => row.map((cell) => `"${String(cell).replace(/"/g, '""')}"`).join(';'))
    .join('\r\n');

  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `clients-${new Date().toISOString().slice(0, 10)}.csv`;
  a.click();
  URL.revokeObjectURL(url);
}
