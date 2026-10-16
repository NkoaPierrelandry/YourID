if (Auth.isAuthenticated()) {
  window.location.href = '/dashboard/index.html';
}

const form = document.getElementById('login-form');
const errorBox = document.getElementById('error-box');
const submitBtn = document.getElementById('submit-btn');

function showError(message) {
  errorBox.textContent = message;
  errorBox.classList.add('visible');
}
function hideError() {
  errorBox.classList.remove('visible');
}

form.addEventListener('submit', async (e) => {
  e.preventDefault();
  hideError();

  const email = document.getElementById('email').value.trim().toLowerCase();
  const password = document.getElementById('password').value;

  submitBtn.disabled = true;
  submitBtn.textContent = 'Connexion...';

  try {
    const response = await api.post('/auth/login', { email, password }, { skipAuth: true });
    const { accessToken, ...user } = response.data;
    Auth.setToken(accessToken);
    Auth.setUser(user);

    // Respecte le ?redirect=... posé par Auth.requireAuth() si on venait
    // d'une page protégée.
    const params = new URLSearchParams(window.location.search);
    const redirect = params.get('redirect');
    window.location.href = redirect || '/dashboard/index.html';
  } catch (err) {
    if (err.status === 401) {
      showError('Email ou mot de passe incorrect.');
    } else if (err.status === 429) {
      showError('Trop de tentatives. Réessayez dans quelques minutes.');
    } else {
      showError(err.message || 'Une erreur est survenue.');
    }
    submitBtn.disabled = false;
    submitBtn.textContent = 'Se connecter';
  }
});
