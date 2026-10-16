let allProducts = [];
let categories = [];
let searchTerm = '';
let categoryFilter = 'all';
let editingProductId = null;

const TYPE_LABELS = {
  EBOOK: 'Ebook', COURSE: 'Formation', AUDIO: 'Audio', TEMPLATE: 'Template',
  SOFTWARE: 'Logiciel', SERVICE: 'Service', SUBSCRIPTION: 'Abonnement', OTHER: 'Autre',
};
const STATUS_BADGE = {
  DRAFT: { cls: 'badge-neutral', label: 'Brouillon' },
  PUBLISHED: { cls: 'badge-success', label: 'Publié' },
  PRIVATE: { cls: 'badge-warning', label: 'Privé' },
};

(async function init() {
  await initDashboardShell('/dashboard/products.html');
  document.getElementById('plus-icon-slot').innerHTML = Icons.plus(16);
  document.getElementById('search-icon').innerHTML = Icons.search(16);

  await loadCategories();
  await loadProducts();

  document.getElementById('search-input').addEventListener('input', (e) => {
    searchTerm = e.target.value.trim().toLowerCase();
    renderTable();
  });

  document.getElementById('open-create-btn').addEventListener('click', () => openCreateModal());
  document.getElementById('pd-close').addEventListener('click', () => closeProductDialog());
  document.getElementById('pd-cancel').addEventListener('click', () => closeProductDialog());
  document.getElementById('product-form').addEventListener('submit', handleSubmit);
  document.getElementById('pd-cover-file').addEventListener('change', handleCoverPreview);
  document.getElementById('pd-ai-btn').addEventListener('click', handleAiGenerate);

  document.getElementById('mode-url-btn').addEventListener('click', () => setContentMode('url'));
  document.getElementById('mode-file-btn').addEventListener('click', () => setContentMode('file'));

  document.getElementById('detail-close').addEventListener('click', () => document.getElementById('detail-dialog').close());
  document.getElementById('detail-delete').addEventListener('click', handleDelete);
  document.getElementById('detail-edit').addEventListener('click', () => {
    document.getElementById('detail-dialog').close();
    openEditModal(currentDetailProduct);
  });
})();

async function loadCategories() {
  try {
    const { data } = await api.get('/categories');
    categories = data;
    const select = document.getElementById('pd-category');
    categories.forEach((c) => {
      const opt = document.createElement('option');
      opt.value = c.id;
      opt.textContent = c.name;
      select.appendChild(opt);
    });
  } catch (e) { /* liste vide acceptable */ }
}

async function loadProducts() {
  try {
    const { data } = await api.get('/products/me?size=100');
    allProducts = data.content;
    renderKpis();
    renderCategoryPills();
    renderTable();
  } catch (e) {
    document.getElementById('products-tbody').innerHTML = `<tr><td colspan="7" class="empty-state">Impossible de charger les produits</td></tr>`;
  }
}

function renderKpis() {
  document.getElementById('kpi-count').textContent = allProducts.length;
  const revenue = allProducts.reduce((sum, p) => sum + Number(p.totalRevenue || 0), 0);
  const sales = allProducts.reduce((sum, p) => sum + (p.totalSales || 0), 0);
  document.getElementById('kpi-revenue').textContent = formatPrice(revenue, allProducts[0]?.currency || 'XOF');
  document.getElementById('kpi-sales').textContent = sales;
}

function renderCategoryPills() {
  const names = Array.from(new Set(allProducts.map((p) => p.categoryName).filter(Boolean))).sort();
  const wrap = document.getElementById('category-pills');
  wrap.innerHTML = `<button class="pill ${categoryFilter === 'all' ? 'active' : ''}" data-cat="all">Toutes les catégories</button>` +
    names.map((n) => `<button class="pill ${categoryFilter === n ? 'active' : ''}" data-cat="${escapeHtml(n)}">${escapeHtml(n)}</button>`).join('');

  wrap.querySelectorAll('.pill').forEach((btn) => {
    btn.addEventListener('click', () => {
      categoryFilter = btn.dataset.cat;
      renderCategoryPills();
      renderTable();
    });
  });
}

function renderTable() {
  const tbody = document.getElementById('products-tbody');
  const filtered = allProducts.filter((p) => {
    const matchesSearch = !searchTerm || p.title.toLowerCase().includes(searchTerm);
    const matchesCategory = categoryFilter === 'all' || p.categoryName === categoryFilter;
    return matchesSearch && matchesCategory;
  });

  if (filtered.length === 0) {
    tbody.innerHTML = `<tr><td colspan="7" class="empty-state">Aucun produit trouvé</td></tr>`;
    return;
  }

  tbody.innerHTML = filtered.map((p) => {
    const badge = STATUS_BADGE[p.status] || { cls: 'badge-neutral', label: p.status };
    return `
      <tr class="clickable" data-id="${p.id}">
        <td>
          <div style="display:flex; align-items:center; gap:10px;">
            <div class="product-thumb">${p.coverImage ? `<img src="${p.coverImage}">` : Icons.package(16)}</div>
            <div style="min-width:0;">
              <p style="margin:0; font-weight:600; font-size:13px;">${escapeHtml(p.title)}</p>
              <p style="margin:0; font-size:11px; color:var(--text-muted);"><span class="type-badge">${TYPE_LABELS[p.type] || p.type}</span></p>
            </div>
          </div>
        </td>
        <td>${p.categoryName ? escapeHtml(p.categoryName) : '—'}</td>
        <td style="font-weight:600;">${formatPrice(p.price, p.currency)}</td>
        <td><span class="badge ${badge.cls}">${badge.label}</span></td>
        <td>${p.totalSales}</td>
        <td style="font-weight:600;">${formatPrice(p.totalRevenue, p.currency)}</td>
        <td style="text-align:center;">${Icons.eye(16)}</td>
      </tr>
    `;
  }).join('');

  tbody.querySelectorAll('tr.clickable').forEach((row) => {
    row.addEventListener('click', () => {
      const product = allProducts.find((p) => p.id === row.dataset.id);
      openDetailDialog(product);
    });
  });
}

let currentDetailProduct = null;

function openDetailDialog(product) {
  currentDetailProduct = product;
  const badge = STATUS_BADGE[product.status] || { cls: 'badge-neutral', label: product.status };
  document.getElementById('detail-body').innerHTML = `
    <div class="pd-cover-preview">${product.coverImage ? `<img src="${product.coverImage}">` : 'Pas de couverture'}</div>
    <div class="pd-detail-row"><span>Titre</span><span style="font-weight:600;">${escapeHtml(product.title)}</span></div>
    <div class="pd-detail-row"><span>Statut</span><span class="badge ${badge.cls}">${badge.label}</span></div>
    <div class="pd-detail-row"><span>Type</span><span>${TYPE_LABELS[product.type]}</span></div>
    <div class="pd-detail-row"><span>Prix</span><span>${formatPrice(product.price, product.currency)}</span></div>
    <div class="pd-detail-row"><span>Ventes</span><span>${product.totalSales}</span></div>
    <div class="pd-detail-row"><span>Revenus</span><span>${formatPrice(product.totalRevenue, product.currency)}</span></div>
    <div class="pd-detail-row"><span>Note moyenne</span><span>${product.rating} / 5 (${product.reviewCount} avis)</span></div>
    <div class="pd-detail-row"><span>Vues</span><span>${product.viewCount}</span></div>
    ${product.contentUrl ? `<div class="pd-detail-row"><span>Contenu</span><a class="auth-link" href="${product.contentUrl}" target="_blank">Ouvrir le lien →</a></div>` : ''}
    ${product.contentFileName ? `<div class="pd-detail-row"><span>Fichier</span><a class="auth-link" href="${window.YOURID_API_URL || 'http://localhost:4000/api/v1'}/products/${product.id}/content/download" target="_blank">${escapeHtml(product.contentFileName)} →</a></div>` : ''}
    <p style="font-size:13px; color:var(--text-muted); margin-top:12px;">${escapeHtml(product.description || '')}</p>
    ${product.status !== 'PUBLISHED' ? `<button type="button" id="quick-publish" class="btn-secondary" style="width:100%; margin-top:12px; font-size:13px; padding:8px;">Publier maintenant</button>` : ''}
  `;
  const quickPublish = document.getElementById('quick-publish');
  if (quickPublish) {
    quickPublish.addEventListener('click', async () => {
      try {
        await api.patch(`/products/${product.id}`, { status: 'PUBLISHED' });
        showToast('Produit publié.');
        document.getElementById('detail-dialog').close();
        loadProducts();
      } catch (e) { showToast(e.message); }
    });
  }
  document.getElementById('detail-dialog').showModal();
}

function openCreateModal() {
  editingProductId = null;
  document.getElementById('pd-title').textContent = 'Nouveau produit';
  document.getElementById('pd-submit').textContent = 'Créer le produit';
  document.getElementById('product-form').reset();
  document.getElementById('pd-cover-preview').innerHTML = 'Aperçu couverture';
  setContentMode('url');
  document.getElementById('existing-content-file').style.display = 'none';
  document.getElementById('product-dialog').showModal();
}

function setContentMode(mode) {
  document.getElementById('mode-url-btn').classList.toggle('active', mode === 'url');
  document.getElementById('mode-file-btn').classList.toggle('active', mode === 'file');
  document.getElementById('content-url-block').style.display = mode === 'url' ? 'block' : 'none';
  document.getElementById('content-file-block').style.display = mode === 'file' ? 'block' : 'none';
}

function openEditModal(product) {
  editingProductId = product.id;
  document.getElementById('pd-title').textContent = 'Modifier le produit';
  document.getElementById('pd-submit').textContent = 'Enregistrer';
  document.getElementById('pd-titlefield').value = product.title;
  document.getElementById('pd-type').value = product.type;
  document.getElementById('pd-description').value = product.description || '';
  document.getElementById('pd-shortdesc').value = product.shortDesc || '';
  document.getElementById('pd-category').value = product.categoryId || '';
  document.getElementById('pd-currency').value = product.currency;
  document.getElementById('pd-price').value = product.price;
  document.getElementById('pd-compareprice').value = product.comparePrice || '';
  document.getElementById('pd-publish-now').checked = product.status === 'PUBLISHED';
  document.getElementById('pd-cover-preview').innerHTML = product.coverImage ? `<img src="${product.coverImage}">` : 'Aperçu couverture';

  const existingFileNote = document.getElementById('existing-content-file');
  if (product.contentType === 'FILE' && product.contentFileName) {
    setContentMode('file');
    existingFileNote.style.display = 'block';
    existingFileNote.textContent = `Fichier actuel : ${product.contentFileName} (choisissez un nouveau fichier pour le remplacer)`;
    document.getElementById('pd-contenttype').value = 'URL';
    document.getElementById('pd-contenturl').value = '';
  } else {
    setContentMode('url');
    existingFileNote.style.display = 'none';
    document.getElementById('pd-contenttype').value = product.contentType || 'URL';
    document.getElementById('pd-contenturl').value = product.contentUrl || '';
  }

  document.getElementById('product-dialog').showModal();
}

function closeProductDialog() {
  document.getElementById('product-dialog').close();
}

async function handleCoverPreview(e) {
  const file = e.target.files[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = () => {
    document.getElementById('pd-cover-preview').innerHTML = `<img src="${reader.result}">`;
  };
  reader.readAsDataURL(file);
}

async function handleAiGenerate() {
  const title = document.getElementById('pd-titlefield').value.trim();
  if (title.length < 3) { showToast('Renseignez d\'abord le titre (3 caractères min).'); return; }

  const btn = document.getElementById('pd-ai-btn');
  btn.disabled = true;
  btn.textContent = '⏳ Génération...';

  try {
    const { data } = await api.post('/ai/generate', {
      type: 'product_description',
      prompt: `Écris une description commerciale accrocheuse (3-4 phrases) pour un produit numérique intitulé "${title}".`,
    });
    document.getElementById('pd-description').value = data.result;
    showToast('Description générée.');
  } catch (e) {
    showToast(e.message || 'La génération IA a échoué (clé OpenAI configurée ?).');
  } finally {
    btn.disabled = false;
    btn.textContent = '<i class="fa-solid fa-wand-magic-sparkles" aria-hidden="true"></i> Générer avec l\'IA';
  }
}

async function handleSubmit(e) {
  e.preventDefault();
  const submitBtn = document.getElementById('pd-submit');
  submitBtn.disabled = true;

  const isFileMode = document.getElementById('mode-file-btn').classList.contains('active');

  const payload = {
    title: document.getElementById('pd-titlefield').value.trim(),
    type: document.getElementById('pd-type').value,
    description: document.getElementById('pd-description').value.trim(),
    shortDesc: document.getElementById('pd-shortdesc').value.trim() || null,
    categoryId: document.getElementById('pd-category').value || null,
    currency: document.getElementById('pd-currency').value,
    price: Number(document.getElementById('pd-price').value),
    comparePrice: document.getElementById('pd-compareprice').value ? Number(document.getElementById('pd-compareprice').value) : null,
    // En mode fichier, on n envoie pas contentType/contentUrl ici -- c est l upload
    // qui les fixera lui-meme a FILE cote serveur (voir ProductService.uploadContent).
    contentType: isFileMode ? null : (document.getElementById('pd-contenttype').value || null),
    contentUrl: isFileMode ? null : (document.getElementById('pd-contenturl').value.trim() || null),
  };

  try {
    let productId = editingProductId;

    if (editingProductId) {
      await api.patch(`/products/${editingProductId}`, payload);
    } else {
      const { data: created } = await api.post('/products', payload);
      productId = created.id;
    }

    const coverFile = document.getElementById('pd-cover-file').files[0];
    if (coverFile) {
      const fd = new FormData();
      fd.append('file', coverFile);
      const { data: uploaded } = await api.post('/files/upload', fd);
      await api.patch(`/products/${productId}`, { coverImage: uploaded.url });
    }

    // Upload du fichier de contenu (ebook/template/...), si mode fichier ET un
    // fichier a ete choisi. S il n y a pas de nouveau fichier en mode edition,
    // on garde simplement le fichier deja heberge tel quel.
    const contentFile = document.getElementById('pd-content-file').files[0];
    if (isFileMode && contentFile) {
      const fd = new FormData();
      fd.append('file', contentFile);
      await api.post(`/products/${productId}/content`, fd);
    }

    if (document.getElementById('pd-publish-now').checked) {
      await api.patch(`/products/${productId}`, { status: 'PUBLISHED' });
    }

    showToast(editingProductId ? 'Produit mis à jour.' : 'Produit créé.');
    closeProductDialog();
    loadProducts();
  } catch (err) {
    showToast(err.message || 'Erreur lors de l\'enregistrement.');
  } finally {
    submitBtn.disabled = false;
  }
}

async function handleDelete() {
  if (!currentDetailProduct) return;
  if (!confirm(`Supprimer "${currentDetailProduct.title}" ? Cette action est irréversible.`)) return;

  try {
    await api.delete(`/products/${currentDetailProduct.id}`);
    showToast('Produit supprimé.');
    document.getElementById('detail-dialog').close();
    loadProducts();
  } catch (e) {
    showToast(e.message || 'Erreur lors de la suppression.');
  }
}
