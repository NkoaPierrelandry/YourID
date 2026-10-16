const API_BASE_URL = window.YOURID_API_URL || 'http://localhost:4000/api/v1';

const TOKEN_KEY = 'yourid_access_token';
const USER_KEY = 'yourid_user';

const Auth = {
  getToken() { return localStorage.getItem(TOKEN_KEY); },
  setToken(token) { localStorage.setItem(TOKEN_KEY, token); },
  clearToken() { localStorage.removeItem(TOKEN_KEY); },

  getUser() {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  },
  setUser(user) { localStorage.setItem(USER_KEY, JSON.stringify(user)); },
  clearUser() { localStorage.removeItem(USER_KEY); },

  isAuthenticated() { return !!this.getToken(); },

  
  logout() {
    this.clearToken();
    this.clearUser();
    window.location.href = '/auth/login.html';   
  },

  requireAuth() {
    if (!this.isAuthenticated()) {
      const redirect = encodeURIComponent(window.location.pathname);
      window.location.href = `/auth/login.html?redirect=${redirect}`;  
    }
  },
};


let refreshPromise = null;   
let refreshBlocked = false;  
function decodeTokenTimes(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
    return { iat: payload.iat * 1000, exp: payload.exp * 1000 };
  } catch (e) {
    return null;
  }
}

async function refreshTokenIfNeeded() {
  const token = Auth.getToken();
  if (!token || refreshBlocked) return;

  const times = decodeTokenTimes(token);
  if (!times) return;

  const lifetime = times.exp - times.iat;
  const remaining = times.exp - Date.now();

  // Déjà expiré : la requête recevra un 401 et la déconnexion suivra.
  // Encore dans sa première moitié de vie : rien à faire.
  if (remaining <= 0 || remaining > lifetime / 2) return;

  if (!refreshPromise) {
    refreshPromise = fetch(`${API_BASE_URL}/auth/refresh`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    })
      .then(async (res) => {
        if (!res.ok) { refreshBlocked = true; return; }
        const body = await res.json();
        Auth.setToken(body.data.accessToken);
      })
      .catch(() => { /* réseau coupé : on réessaiera à la prochaine requête */ })
      .finally(() => { refreshPromise = null; });
  }
  await refreshPromise;
}

/**
 * Appelle l'API backend. Attache le Bearer token automatiquement,
 * sauf si options.skipAuth est vrai (routes publiques : marketplace,
 * fiche produit, checkout invité...).
 */
async function apiFetch(path, options = {}) {
  const { skipAuth = false, body, ...rest } = options;

  if (!skipAuth) await refreshTokenIfNeeded();

  const headers = { ...(rest.headers || {}) };
  const isFormData = body instanceof FormData;
  if (!isFormData) headers['Content-Type'] = 'application/json';

  if (!skipAuth) {
    const token = Auth.getToken();
    if (token) headers['Authorization'] = `Bearer ${token}`;
  }

  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...rest,
      headers,
      body: isFormData ? body : body ? JSON.stringify(body) : undefined,
    });
  } catch (error) {
    throw new Error('Le serveur API est indisponible. Vérifiez que l’API est bien démarrée sur localhost:4000.');
  }

  // 401 sur une route protégée -- session morte, on déconnecte proprement.
  if (response.status === 401 && !skipAuth) {
    Auth.logout();
    throw new Error('Authentification requise.');
  }

  const data = await response.json().catch(() => null);

  if (!response.ok) {
    const message = data?.message || `Erreur ${response.status}`;
    throw new ApiError(message, response.status, data);
  }

  return data;
}

class ApiError extends Error {
  constructor(message, status, body) {
    super(message);
    this.status = status;
    this.body = body;
  }
}

const api = {
  get(path, opts) { return apiFetch(path, { method: 'GET', ...opts }); },
  post(path, body, opts) { return apiFetch(path, { method: 'POST', body, ...opts }); },
  patch(path, body, opts) { return apiFetch(path, { method: 'PATCH', body, ...opts }); },
  delete(path, opts) { return apiFetch(path, { method: 'DELETE', ...opts }); },
};

/* ── Helpers d'affichage, repris de src/lib/utils.ts ── */

function formatPrice(amount, currency = 'XOF') {
  const n = Number(amount) || 0;
  return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(n) + ' ' + currency;
}

const API_ORIGIN = (window.YOURID_API_URL || 'http://localhost:4000/api/v1')
  .replace(/\/api\/v1\/?$/, '');

function resolveAssetUrl(url) {
  if (!url) return url;
  if (/^(https?:|data:|blob:)/i.test(url)) return url;
  return API_ORIGIN + (url.startsWith('/') ? url : '/' + url);
}

function getInitials(name) {
  if (!name) return '?';
  return name
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0].toUpperCase())
    .join('');
}

function formatDate(iso) {
  if (!iso) return '—';
  const d = new Date(iso);
  return d.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' });
}

function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str ?? '';
  return div.innerHTML;
}