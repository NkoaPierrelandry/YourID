(function () {
  const SHOW_ICON = '<i class="fa-regular fa-eye" aria-hidden="true"></i>';
  const HIDE_ICON = '<i class="fa-regular fa-eye-slash" aria-hidden="true"></i>';

  function setButtonState(button, input) {
    const isVisible = input.type === 'text';
    button.setAttribute('aria-label', isVisible ? 'Masquer le mot de passe' : 'Afficher le mot de passe');
    button.setAttribute('title', isVisible ? 'Masquer le mot de passe' : 'Afficher le mot de passe');
    button.setAttribute('aria-pressed', String(isVisible));
    button.innerHTML = isVisible ? HIDE_ICON : SHOW_ICON;
  }

  function bindPasswordInput(input) {
    if (!input || input.type !== 'password' || input.disabled || input.dataset.passwordToggleBound === 'true') {
      return;
    }

    const wrapper = document.createElement('div');
    wrapper.className = 'password-toggle-wrap';

    if (input.parentNode) {
      input.parentNode.insertBefore(wrapper, input);
    }
    wrapper.appendChild(input);

    const toggleButton = document.createElement('button');
    toggleButton.type = 'button';
    toggleButton.className = 'password-toggle-btn';
    toggleButton.setAttribute('aria-label', 'Afficher le mot de passe');
    toggleButton.setAttribute('title', 'Afficher le mot de passe');
    wrapper.appendChild(toggleButton);

    toggleButton.addEventListener('click', (event) => {
      event.preventDefault();
      event.stopPropagation();

      const shouldShow = input.type === 'password';
      input.type = shouldShow ? 'text' : 'password';
      setButtonState(toggleButton, input);

      if (shouldShow) {
        const caret = input.value.length;
        input.focus();
        input.setSelectionRange(caret, caret);
      } else {
        input.focus();
      }
    });

    input.dataset.passwordToggleBound = 'true';
    setButtonState(toggleButton, input);
  }

  function initPasswordToggles() {
    document.querySelectorAll('input[type="password"]').forEach((input) => {
      if (input.disabled || input.dataset.passwordToggleBound === 'true') {
        return;
      }
      bindPasswordInput(input);
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initPasswordToggles);
  } else {
    initPasswordToggles();
  }
})();
