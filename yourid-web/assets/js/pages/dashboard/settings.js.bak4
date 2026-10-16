const METHOD_LABELS = {
  ORANGE_MONEY: 'Orange Money', WAVE: 'Wave', MTN_MONEY: 'MTN Mobile Money', BANK_TRANSFER: 'Virement bancaire',
};
const METHOD_INITIALS = { ORANGE_MONEY: 'OM', WAVE: 'W', MTN_MONEY: 'MTN', BANK_TRANSFER: 'B' };

(async function init() {
  await initDashboardShell('/dashboard/settings.html');

  setupTabs();
  await loadProfile();
  await loadStore();
  await loadPaymentAccounts();

  document.getElementById('profile-form').addEventListener('submit', saveProfile);
  document.getElementById('store-form').addEventListener('submit', saveStore);

  // Aperçu live des images choisies
  document.getElementById('s-logo-file').addEventListener('change', (e) => {
    const file = e.target.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (ev) => {
      document.getElementById('s-logo-preview').innerHTML =
        `<img src="${ev.target.result}" style="width:100%; height:100%; object-fit:cover;">`;
    };
    reader.readAsDataURL(file);
  });

  document.getElementById('s-banner-file').addEventListener('change', (e) => {
    const file = e.target.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (ev) => {
      const preview = document.getElementById('s-banner-preview');
      preview.style.backgroundImage = `url(${ev.target.result})`;
      preview.innerHTML = '';
    };
    reader.readAsDataURL(file);
  });

  // Color picker : mise à jour du hex + swatches
  const colorInput = document.getElementById('s-color');
  const colorHex = document.getElementById('s-color-hex');

  function updateColorHex(value) {
    colorHex.textContent = value.toUpperCase();
    document.querySelectorAll('.color-swatch').forEach((s) => {
      s.classList.toggle('selected', s.dataset.color.toUpperCase() === value.toUpperCase());
    });
  }

  colorInput.addEventListener('input', (e) => updateColorHex(e.target.value));

  document.querySelectorAll('.color-swatch').forEach((btn) => {
    btn.addEventListener('click', () => {
      const color = btn.dataset.color;
      colorInput.value = color;
      updateColorHex(color);
    });
  });

  // Init : sync le hex au chargement
  updateColorHex(colorInput.value);
  document.getElementById('create-store-form').addEventListener('submit', createStore);
  document.getElementById('password-form').addEventListener('submit', (e) => e.preventDefault());

  document.getElementById('add-account-btn').addEventListener('click', () => document.getElementById('account-dialog').showModal());
  document.getElementById('account-dialog-close').addEventListener('click', () => document.getElementById('account-dialog').close());
  document.getElementById('account-cancel').addEventListener('click', () => document.getElementById('account-dialog').close());
  document.getElementById('account-form').addEventListener('submit', createPaymentAccount);
})();

function setupTabs() {
  const tabs = document.querySelectorAll('#settings-tabs .pill');
  tabs.forEach((tab) => {
    tab.addEventListener('click', () => {
      tabs.forEach((t) => t.classList.remove('active'));
      tab.classList.add('active');
      ['profile', 'store', 'payment', 'security'].forEach((key) => {
        document.getElementById(`tab-${key}`).style.display = key === tab.dataset.tab ? 'block' : 'none';
      });
    });
  });
}

async function loadProfile() {
  const user = Auth.getUser();
  document.getElementById('p-firstname').value = user.firstName || '';
  document.getElementById('p-lastname').value = user.lastName || '';
  document.getElementById('p-email').value = user.email || '';
  document.getElementById('p-bio').value = user.bio || '';
}

async function saveProfile(e) {
  e.preventDefault();
  showToast('Enregistrement du profil…');
  try {
    const { data } = await api.patch('/users/me', {
      firstName: document.getElementById('p-firstname').value.trim(),
      lastName: document.getElementById('p-lastname').value.trim(),
      bio: document.getElementById('p-bio').value.trim(),
    });
    Auth.setUser({ ...Auth.getUser(), ...data });
    showToast('Profil mis à jour.');
  } catch (err) {
    showToast(err.message || 'Erreur lors de la mise à jour.');
  }
}

let _currentStore = null;

async function loadStore() {
  try {
    const { data: store } = await api.get('/stores/me');
    _currentStore = store;
    document.getElementById('no-store-view').style.display = 'none';
    document.getElementById('store-form').style.display = 'block';
    document.getElementById('s-name').value = store.name;
    document.getElementById('s-description').value = store.description || '';
    document.getElementById('s-color').value = store.primaryColor;
    const _hex = document.getElementById('s-color-hex');
    if (_hex) _hex.textContent = (store.primaryColor || '#E01B24').toUpperCase();
    document.getElementById('s-website').value = store.website || '';
    document.getElementById('s-instagram').value = store.instagram || '';
    document.getElementById('s-twitter').value = store.twitter || '';
    document.getElementById('s-slug').textContent = store.slug;

    // Nouveaux parametres
    const _tagline = document.getElementById('s-tagline');
    if (_tagline) _tagline.value = store.tagline || '';
    const _cemail = document.getElementById('s-contact-email');
    if (_cemail) _cemail.value = store.contactEmail || '';
    const _cphone = document.getElementById('s-contact-phone');
    if (_cphone) _cphone.value = store.contactPhone || '';
    const _ann = document.getElementById('s-announcement');
    if (_ann) _ann.value = store.announcement || '';
    const _annActive = document.getElementById('s-announcement-active');
    if (_annActive) _annActive.checked = !!store.announcementActive;
    const _defCur = document.getElementById('s-default-currency');
    if (_defCur) _defCur.value = store.defaultCurrency || '';

    // Aperçu logo
    const logoPreview = document.getElementById('s-logo-preview');
    if (store.logo) {
      logoPreview.innerHTML = `<img src="${resolveAssetUrl(store.logo)}" style="width:100%; height:100%; object-fit:cover;">`;
    } else {
      logoPreview.innerHTML = '<i class="fa-solid fa-store" aria-hidden="true"></i>';
    }

    // Aperçu bannière
    const bannerPreview = document.getElementById('s-banner-preview');
    if (store.banner) {
      bannerPreview.style.backgroundImage = `url(${resolveAssetUrl(store.banner)})`;
      bannerPreview.innerHTML = '';
    } else {
      bannerPreview.style.backgroundImage = '';
      bannerPreview.innerHTML = '<span>Aucune bannière — le dernier produit publié sera utilisé</span>';
    }
  } catch (e) {
    document.getElementById('no-store-view').style.display = 'block';
    document.getElementById('store-form').style.display = 'none';
  }
}

async function uploadImage(file) {
  const fd = new FormData();
  fd.append('file', file);
  const { data } = await api.post('/files/upload', fd);
  return data.url;
}

async function saveStore(e) {
  e.preventDefault();
  const submitBtn = e.submitter || document.querySelector('#store-form button[type="submit"]');
  if (submitBtn) { submitBtn.disabled = true; submitBtn.textContent = 'Enregistrement…'; }

  try {
    const payload = {
      name: document.getElementById('s-name').value.trim(),
      description: document.getElementById('s-description').value.trim(),
      primaryColor: document.getElementById('s-color').value,
      website: document.getElementById('s-website').value.trim() || null,
      instagram: document.getElementById('s-instagram').value.trim() || null,
      twitter: document.getElementById('s-twitter').value.trim() || null,
      tagline: (document.getElementById('s-tagline')?.value || '').trim() || null,
      contactEmail: (document.getElementById('s-contact-email')?.value || '').trim() || null,
      contactPhone: (document.getElementById('s-contact-phone')?.value || '').trim() || null,
      announcement: (document.getElementById('s-announcement')?.value || '').trim() || null,
      announcementActive: !!document.getElementById('s-announcement-active')?.checked,
      defaultCurrency: (document.getElementById('s-default-currency')?.value || '').trim() || null,
    };

    // Upload logo si un fichier a été choisi
    const logoFile = document.getElementById('s-logo-file').files[0];
    if (logoFile) {
      showToast('Upload du logo…');
      payload.logo = await uploadImage(logoFile);
    }

    // Upload bannière si un fichier a été choisi
    const bannerFile = document.getElementById('s-banner-file').files[0];
    if (bannerFile) {
      showToast('Upload de la bannière…');
      payload.banner = await uploadImage(bannerFile);
    }

    await api.patch('/stores/me', payload);

    // Reset des inputs file
    document.getElementById('s-logo-file').value = '';
    document.getElementById('s-banner-file').value = '';

    showToast('Boutique mise à jour.');
    await loadStore();
  } catch (err) {
    showToast(err.message || 'Erreur lors de la mise à jour.');
  } finally {
    if (submitBtn) { submitBtn.disabled = false; submitBtn.textContent = 'Enregistrer'; }
  }
}

async function createStore(e) {
  e.preventDefault();
  const name = document.getElementById('cs-name').value.trim();
  if (!name) {
    showToast('Renseignez le nom de votre boutique.');
    return;
  }

  const submitBtn = e.submitter || document.querySelector('#create-store-form button[type="submit"]');
  if (submitBtn) {
    submitBtn.disabled = true;
    submitBtn.textContent = 'Création…';
  }

  showToast('Création de la boutique…');

  try {
    await api.post('/stores', {
      name,
      description: document.getElementById('cs-description').value.trim() || null,
      currency: document.getElementById('cs-currency').value,
      country: document.getElementById('cs-country').value.trim().toUpperCase(),
    });
    showToast('Boutique créée !');
    window.location.reload();
  } catch (err) {
    showToast(err.message || 'Erreur lors de la création.');
    if (submitBtn) {
      submitBtn.disabled = false;
      submitBtn.textContent = 'Créer ma boutique';
    }
  }
}

async function loadPaymentAccounts() {
  const list = document.getElementById('payment-accounts-list');
  try {
    const { data } = await api.get('/payment-accounts/me');
    if (data.length === 0) {
      list.innerHTML = `<div class="settings-empty">Aucun compte enregistré pour l'instant.</div>`;
      return;
    }
    list.innerHTML = data.map((acc) => `
      <div class="payment-account-card" data-id="${acc.id}">
        <div class="payment-account-left">
          <div class="payment-method-icon pm-${acc.method.toLowerCase()}">${METHOD_INITIALS[acc.method]}</div>
          <div style="min-width:0;">
            <p class="payment-account-name">${escapeHtml(acc.label || METHOD_LABELS[acc.method])}</p>
            <p class="payment-account-sub">${METHOD_LABELS[acc.method]}</p>
          </div>
        </div>
        <div class="payment-account-actions">
          ${acc.isDefault ? '<span class="default-badge">Par défaut</span>' : `<button class="icon-btn set-default-btn" title="Definir par defaut">${Icons.checkCircle(16)}</button>`}
          <button class="icon-btn delete-account-btn" title="Supprimer">${Icons.trash(16)}</button>
        </div>
      </div>
    `).join('');

    list.querySelectorAll('.set-default-btn').forEach((btn) => {
      btn.addEventListener('click', async (e) => {
        const id = e.target.closest('.payment-account-card').dataset.id;
        await api.patch(`/payment-accounts/${id}/default`);
        showToast('Compte par défaut mis à jour.');
        loadPaymentAccounts();
      });
    });
    list.querySelectorAll('.delete-account-btn').forEach((btn) => {
      btn.addEventListener('click', async (e) => {
        const id = e.target.closest('.payment-account-card').dataset.id;
        if (!confirm('Supprimer ce compte de paiement ?')) return;
        await api.delete(`/payment-accounts/${id}`);
        showToast('Compte supprimé.');
        loadPaymentAccounts();
      });
    });
  } catch (e) {
    list.innerHTML = `<div class="settings-empty">Impossible de charger vos comptes.</div>`;
  }
}

async function createPaymentAccount(e) {
  e.preventDefault();
  try {
    await api.post('/payment-accounts', {
      method: document.getElementById('acc-method').value,
      label: document.getElementById('acc-label').value.trim() || null,
      accountInfo: JSON.stringify({ account: document.getElementById('acc-number').value.trim() }),
    });
    showToast('Compte ajouté.');
    document.getElementById('account-dialog').close();
    document.getElementById('account-form').reset();
    loadPaymentAccounts();
  } catch (err) {
    showToast(err.message || 'Erreur lors de l\'ajout.');
  }
}