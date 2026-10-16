const STATUS_BADGE = {
  PENDING: { cls: 'badge-warning', label: 'En attente' },
  APPROVED: { cls: 'badge-neutral', label: 'Approuvé' },
  PAID: { cls: 'badge-success', label: 'Payé' },
  REJECTED: { cls: 'badge-danger', label: 'Rejeté' },
};
const METHOD_LABELS = {
  ORANGE_MONEY: 'Orange Money', WAVE: 'Wave', MTN_MONEY: 'MTN Mobile Money', BANK_TRANSFER: 'Virement bancaire',
};

let storeBalance = 0;
let storeCurrency = 'XOF';
let paymentAccounts = [];

(async function init() {
  await initDashboardShell('/dashboard/payouts.html');

  try {
    const { data: store } = await api.get('/stores/me');
    storeBalance = store.balance;
    storeCurrency = store.currency;
    document.getElementById('kpi-balance').textContent = formatPrice(storeBalance, storeCurrency);
    document.getElementById('withdraw-available').textContent = formatPrice(storeBalance, storeCurrency);
  } catch (e) { document.getElementById('kpi-balance').textContent = '—'; }

  await loadPaymentAccountsIntoSelect();
  await loadWithdrawals();

  document.getElementById('open-withdraw-btn').addEventListener('click', () => {
    if (storeBalance <= 0) { showToast('Votre solde est à zéro pour le moment.'); return; }
    document.getElementById('withdraw-dialog').showModal();
  });
  document.getElementById('withdraw-close').addEventListener('click', () => document.getElementById('withdraw-dialog').close());
  document.getElementById('withdraw-cancel').addEventListener('click', () => document.getElementById('withdraw-dialog').close());
  document.getElementById('withdraw-form').addEventListener('submit', handleSubmit);
})();

async function loadPaymentAccountsIntoSelect() {
  const select = document.getElementById('w-account-select');
  try {
    const { data } = await api.get('/payment-accounts/me');
    paymentAccounts = data;

    if (data.length === 0) {
      document.getElementById('no-account-hint').style.display = 'block';
      select.innerHTML = '';
      select.disabled = true;
      return;
    }
    select.disabled = false;
    select.innerHTML = data.map((acc) =>
      `<option value="${acc.id}">${escapeHtml(acc.label || METHOD_LABELS[acc.method])} — ${METHOD_LABELS[acc.method]}${acc.isDefault ? ' (défaut)' : ''}</option>`
    ).join('');

    const defaultAcc = data.find((a) => a.isDefault);
    if (defaultAcc) select.value = defaultAcc.id;
  } catch (e) {
    select.innerHTML = '';
    select.disabled = true;
  }
}

async function loadWithdrawals() {
  try {
    const { data } = await api.get('/withdrawals/me?size=100&sort=createdAt,desc');
    const withdrawals = data.content;
    document.getElementById('kpi-pending').textContent = withdrawals.filter((w) => w.status === 'PENDING').length;

    const tbody = document.getElementById('withdrawals-tbody');
    if (withdrawals.length === 0) {
      tbody.innerHTML = `<tr><td colspan="5" class="empty-state">Aucune demande de retrait pour l'instant</td></tr>`;
      return;
    }
    tbody.innerHTML = withdrawals.map((w) => {
      const badge = STATUS_BADGE[w.status] || { cls: 'badge-neutral', label: w.status };
      return `
        <tr>
          <td style="font-weight:600;">${formatPrice(w.amount, w.currency)}</td>
          <td>${escapeHtml(w.paymentAccountLabel || w.method)}</td>
          <td><span class="badge ${badge.cls}">${badge.label}</span></td>
          <td>${formatDate(w.createdAt)}</td>
          <td>${w.processedAt ? formatDate(w.processedAt) : '—'}</td>
        </tr>`;
    }).join('');
  } catch (e) {
    document.getElementById('withdrawals-tbody').innerHTML = `<tr><td colspan="5" class="empty-state">Impossible de charger les retraits</td></tr>`;
  }
}

async function handleSubmit(e) {
  e.preventDefault();
  const amount = Number(document.getElementById('w-amount').value);

  if (amount > storeBalance) { showToast('Le montant dépasse votre solde disponible.'); return; }
  if (paymentAccounts.length === 0) { showToast('Ajoutez un compte de paiement dans Paramètres avant de continuer.'); return; }

  const payload = {
    amount,
    paymentAccountId: document.getElementById('w-account-select').value,
    notes: document.getElementById('w-notes').value.trim() || null,
  };

  try {
    await api.post('/withdrawals', payload);
    showToast('Demande de retrait envoyée.');
    document.getElementById('withdraw-dialog').close();
    document.getElementById('withdraw-form').reset();
    window.location.reload();
  } catch (err) {
    showToast(err.message || 'Erreur lors de la demande.');
  }
}