let allOrders = [];
let searchTerm = '';
let statusFilter = 'all';
let debounceTimer = null;

const STATUS_BADGE = {
  PENDING: { cls: 'badge-warning', label: 'En attente' },
  COMPLETED: { cls: 'badge-success', label: 'Complétée' },
  REFUNDED: { cls: 'badge-danger', label: 'Remboursée' },
};

(async function init() {
  await initDashboardShell('/dashboard/orders.html');
  document.getElementById('search-icon').innerHTML = Icons.search(16);

  await loadOrders();

  document.getElementById('search-input').addEventListener('input', (e) => {
    clearTimeout(debounceTimer);
    const value = e.target.value;
    debounceTimer = setTimeout(() => {
      searchTerm = value.trim().toLowerCase();
      renderTable();
    }, 300);
  });

  document.getElementById('status-filter').addEventListener('change', (e) => {
    statusFilter = e.target.value;
    renderTable();
  });

  document.getElementById('export-btn').addEventListener('click', exportCsv);
  document.getElementById('order-dialog-close').addEventListener('click', () => {
    document.getElementById('order-dialog').close();
  });
})();

async function loadOrders() {
  try {
    const { data } = await api.get('/orders/me?size=200&sort=createdAt,desc');
    allOrders = data.content;
    renderTable();
  } catch (e) {
    document.getElementById('orders-tbody').innerHTML = `<tr><td colspan="6" class="empty-state">Impossible de charger les commandes</td></tr>`;
  }
}

function getFiltered() {
  return allOrders.filter((o) => {
    const matchesStatus = statusFilter === 'all' || o.status === statusFilter;
    const q = searchTerm;
    const matchesSearch = !q ||
      o.orderNumber.toLowerCase().includes(q) ||
      o.customerName.toLowerCase().includes(q) ||
      o.customerEmail.toLowerCase().includes(q);
    return matchesStatus && matchesSearch;
  });
}

function renderTable() {
  const filtered = getFiltered();
  const tbody = document.getElementById('orders-tbody');
  document.getElementById('export-btn').disabled = filtered.length === 0;

  if (filtered.length === 0) {
    tbody.innerHTML = `<tr><td colspan="6" class="empty-state">Aucune commande trouvée</td></tr>`;
    return;
  }

  tbody.innerHTML = filtered.map((o) => {
    const badge = STATUS_BADGE[o.status] || { cls: 'badge-neutral', label: o.status };
    const itemsSummary = o.items.map((i) => i.title).join(', ');
    return `
      <tr class="clickable" data-id="${o.id}">
        <td style="font-weight:600;">${o.orderNumber}</td>
        <td>
          <p style="margin:0; font-weight:600; font-size:13px;">${escapeHtml(o.customerName)}</p>
          <p style="margin:0; font-size:11px; color:var(--text-muted);">${escapeHtml(o.customerEmail)}</p>
        </td>
        <td style="max-width:220px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;" title="${escapeHtml(itemsSummary)}">${escapeHtml(itemsSummary)}</td>
        <td style="font-weight:600;">${formatPrice(o.total, o.currency)}</td>
        <td><span class="badge ${badge.cls}">${badge.label}</span></td>
        <td>${formatDate(o.createdAt)}</td>
      </tr>
    `;
  }).join('');

  tbody.querySelectorAll('tr.clickable').forEach((row) => {
    row.addEventListener('click', () => {
      const order = allOrders.find((o) => o.id === row.dataset.id);
      openOrderDialog(order);
    });
  });
}

function openOrderDialog(order) {
  const badge = STATUS_BADGE[order.status] || { cls: 'badge-neutral', label: order.status };
  document.getElementById('order-dialog-body').innerHTML = `
    <div style="display:flex; justify-content:space-between; margin-bottom:12px;">
      <span style="font-weight:600;">${order.orderNumber}</span>
      <span class="badge ${badge.cls}">${badge.label}</span>
    </div>
    <p style="font-size:13px; color:var(--text-muted); margin:0 0 16px;">${escapeHtml(order.customerName)} — ${escapeHtml(order.customerEmail)}</p>
    <div style="border-top:1px solid var(--border); border-bottom:1px solid var(--border); padding:12px 0; margin-bottom:12px;">
      ${order.items.map((item) => `
        <div style="display:flex; justify-content:space-between; font-size:14px; padding:4px 0;">
          <span>${escapeHtml(item.title)} × ${item.quantity}</span>
          <span>${formatPrice(item.price * item.quantity, order.currency)}</span>
        </div>
      `).join('')}
    </div>
    <div class="pd-detail-row"><span>Sous-total</span><span>${formatPrice(order.subtotal, order.currency)}</span></div>
    <div class="pd-detail-row"><span>Commission plateforme</span><span>- ${formatPrice(order.platformFee, order.currency)}</span></div>
    <div class="pd-detail-row" style="font-weight:700;"><span>Votre revenu</span><span>${formatPrice(order.sellerRevenue, order.currency)}</span></div>
    ${order.completedAt ? `<p style="font-size:12px; color:var(--text-muted); margin-top:12px;">Complétée le ${formatDate(order.completedAt)}</p>` : ''}
  `;
  document.getElementById('order-dialog').showModal();
}

function exportCsv() {
  const filtered = getFiltered();
  if (filtered.length === 0) return;

  const headers = ['Numero', 'Client', 'Email', 'Montant', 'Devise', 'Statut', 'Date'];
  const rows = filtered.map((o) => [
    o.orderNumber, o.customerName, o.customerEmail, o.total, o.currency, o.status, formatDate(o.createdAt),
  ]);

  const csv = [headers, ...rows]
    .map((row) => row.map((cell) => `"${String(cell).replace(/"/g, '""')}"`).join(','))
    .join('\n');

  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `commandes-${new Date().toISOString().slice(0, 10)}.csv`;
  a.click();
  URL.revokeObjectURL(url);
}
