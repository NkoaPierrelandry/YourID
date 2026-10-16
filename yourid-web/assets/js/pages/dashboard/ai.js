(async function init() {
  await initDashboardShell('/dashboard/ai.html');

  document.getElementById('ai-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const submitBtn = document.getElementById('ai-submit');
    submitBtn.disabled = true;
    submitBtn.textContent = 'Génération en cours...';

    try {
      const { data } = await api.post('/ai/generate', {
        type: document.getElementById('ai-type').value,
        prompt: document.getElementById('ai-prompt').value.trim(),
      });

      document.getElementById('ai-result-text').textContent = data.result;
      document.getElementById('ai-meta').textContent = `Modèle : ${data.model}${data.tokens ? ` · ${data.tokens} tokens` : ''}`;
      document.getElementById('ai-result-card').style.display = 'block';
    } catch (err) {
      showToast(err.message || 'La génération a échoué.');
    } finally {
      submitBtn.disabled = false;
      submitBtn.textContent = 'Générer';
    }
  });

  document.getElementById('copy-btn').addEventListener('click', () => {
    const text = document.getElementById('ai-result-text').textContent;
    navigator.clipboard.writeText(text).then(() => showToast('Copié dans le presse-papiers.'));
  });
})();
