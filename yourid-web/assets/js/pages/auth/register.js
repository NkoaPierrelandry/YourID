if (Auth.isAuthenticated()) window.location.href = '/dashboard/index.html';

const form = document.getElementById('register-form');
const errorBox = document.getElementById('error-box');
const submitBtn = document.getElementById('submit-btn');

form.addEventListener('submit', async (e) => {
  e.preventDefault();
  errorBox.classList.remove('visible');

  const payload = {
    firstName: document.getElementById('firstName').value.trim(),
    lastName: document.getElementById('lastName').value.trim(),
    username: document.getElementById('username').value.trim(),
    email: document.getElementById('email').value.trim().toLowerCase(),
    password: document.getElementById('password').value,
  };

  submitBtn.disabled = true;
  submitBtn.textContent = 'Création...';

  try {
    const response = await api.post('/auth/register', payload, { skipAuth: true });
    const { accessToken, ...user } = response.data;
    Auth.setToken(accessToken);
    Auth.setUser(user);
    window.location.href = '/dashboard/settings.html'; // pour creer la boutique directement
  } catch (err) {
    if (err.status === 409) {
      errorBox.textContent = err.message; // "Cet email est deja utilise." / "Ce nom d utilisateur est deja pris."
    } else if (err.status === 400 && err.body?.data) {
      // Erreurs de validation par champ -- on affiche la premiere trouvee.
      const firstError = Object.values(err.body.data)[0];
      errorBox.textContent = firstError || 'Données invalides.';
    } else {
      errorBox.textContent = err.message || 'Une erreur est survenue.';
    }
    errorBox.classList.add('visible');
    submitBtn.disabled = false;
    submitBtn.textContent = 'Créer mon compte';
  }
});