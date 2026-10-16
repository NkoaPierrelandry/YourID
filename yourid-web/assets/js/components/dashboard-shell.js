const SIDEBAR_GROUPS = [
  {
    label: 'Vendre',
    items: [
      { href: '/dashboard/index.html', icon: 'dashboard', label: 'Tableau de bord' },
      { href: '/dashboard/products.html', icon: 'package', label: 'Produits' },
      { href: '/dashboard/orders.html', icon: 'cart', label: 'Commandes' },
    ],
  },
  {
    label: 'Gérer',
    items: [
      { href: '/dashboard/customers.html', icon: 'users', label: 'Clients' },
      { href: '/dashboard/payouts.html', icon: 'wallet', label: 'Versements' },
      { href: '/dashboard/analytics.html', icon: 'chart', label: 'Analytics' },
      { href: '/dashboard/ai.html', icon: 'bot', label: 'IA Assistant' },
    ],
  },
  {
    label: 'Compte',
    items: [
      { href: '/dashboard/settings.html', icon: 'settings', label: 'Paramètres' },
    ],
  },
];

function showToast(message) {
  const existing = document.querySelector('.toast');
  if (existing) existing.remove();

  const toast = document.createElement('div');
  toast.className = 'toast';
  toast.setAttribute('role', 'status');
  toast.innerHTML = `<span>${escapeHtml(message)}</span><button aria-label="Fermer">${Icons.x(14)}</button>`;
  toast.querySelector('button').onclick = () => toast.remove();
  document.body.appendChild(toast);
  setTimeout(() => toast.remove(), 3500);
}

async function initDashboardShell(activeHref) {
  Auth.requireAuth();
  const user = Auth.getUser();
  if (!user) return;

  const sidebarRoot = document.getElementById('sidebar-root');
  const headerRoot = document.getElementById('header-root');

  let collapsed = false;

  function renderSidebar() {
    const groupsHtml = SIDEBAR_GROUPS.map((group) => `
      <div>
        ${!collapsed ? `<p class="dash-nav-group-label">${group.label}</p>` : ''}
        <div class="dash-nav-items">
          ${group.items.map((item) => {
            const active = activeHref === item.href;
            return `
              <a href="${item.href}" class="dash-nav-link ${active ? 'active' : ''}" title="${collapsed ? item.label : ''}">
                ${Icons[item.icon](20)}
                ${!collapsed ? `<span>${item.label}</span>` : ''}
              </a>`;
          }).join('')}
        </div>
      </div>
    `).join('');

    const storefrontSlug = user.store?.slug || '';

    sidebarRoot.className = `dash-sidebar ${collapsed ? 'collapsed' : ''}`;
    sidebarRoot.innerHTML = `
      <div class="dash-sidebar-logo">
        <a href="/" class="dash-logo-link">
          <div class="dash-logo-mark">YI</div>
          ${!collapsed ? `<span class="dash-logo-text">Your<span style="color:var(--brand)">ID</span></span>` : ''}
        </a>
      </div>

      <div class="dash-sidebar-toggle">
        <button id="sidebar-toggle-btn" aria-label="Basculer la barre latérale">
          ${collapsed ? Icons.chevronRight(16) : Icons.chevronLeft(16)}
        </button>
      </div>

      ${user.store && storefrontSlug ? `
        <div class="dash-sidebar-store">
          <a href="/shop/store.html?slug=${encodeURIComponent(storefrontSlug)}" target="_blank" class="dash-store-link">
            <div class="dash-store-avatar">${getInitials(user.store.name)}</div>
            ${!collapsed ? `<div><p class="dash-store-name">${escapeHtml(user.store.name)}</p><p class="dash-store-sub">Voir la boutique</p></div>` : ''}
          </a>
        </div>
      ` : ''}

      <nav class="dash-nav">${groupsHtml}</nav>

      <div class="dash-sidebar-profile">
        <div class="dash-profile-row">
          <div class="dash-profile-info">
            <div class="dash-profile-avatar">${getInitials(user.firstName + ' ' + user.lastName)}</div>
            ${!collapsed ? `<div class="dash-profile-text"><p>${escapeHtml(user.firstName)} ${escapeHtml(user.lastName)}</p><p class="dash-profile-email">${escapeHtml(user.email)}</p></div>` : ''}
          </div>
          ${!collapsed ? `<button id="logout-btn" aria-label="Déconnexion" title="Se déconnecter">${Icons.logout(16)}</button>` : ''}
        </div>
      </div>
    `;

    document.getElementById('sidebar-toggle-btn').onclick = () => {
      collapsed = !collapsed;
      renderSidebar();
    };
    const logoutBtn = document.getElementById('logout-btn');
    if (logoutBtn) logoutBtn.onclick = () => Auth.logout();
  }

  renderSidebar();

  // -- Header --
  headerRoot.className = 'dash-header';
  headerRoot.innerHTML = `
    <button id="mobile-menu-btn" class="dash-mobile-toggle" aria-label="Ouvrir le menu">${Icons.menu(20)}</button>
    <div style="flex:1"></div>
    <div id="store-balance-badge" class="dash-balance-badge" style="display:none">${Icons.wallet(14)}<span></span></div>
    <a href="/dashboard/notifications.html" class="dash-bell" aria-label="Notifications">
      ${Icons.bell(18)}
      <span id="unread-badge" class="dash-unread-badge" style="display:none"></span>
    </a>
  `;

  document.getElementById('mobile-menu-btn').onclick = () => {
    sidebarRoot.classList.toggle('mobile-open');
  };

  // -- Données live : solde boutique + notifications non lues --
  try {
    const store = await api.get('/stores/me');
    const badge = document.getElementById('store-balance-badge');
    badge.style.display = 'flex';
    badge.querySelector('span').textContent = formatPrice(store.data.balance, store.data.currency);
    user.store = store.data;
    Auth.setUser(user);
    renderSidebar();
  } catch (e) {
    // Pas encore de boutique -- normal pour un compte tout neuf, on masque juste le badge.
  }

  try {
    const unread = await api.get('/notifications/unread-count');
    const count = unread.data.count;
    if (count > 0) {
      const badge = document.getElementById('unread-badge');
      badge.style.display = 'flex';
      badge.textContent = count > 9 ? '9+' : count;
    }
  } catch (e) { /* silencieux */ }
}
