(function () {
  'use strict';

  const OVERLAY_ID = 'yi-page-transition';
  const GUARD_KEY = '__yiPageTransitionInit';
  const RELOAD_FLAG_KEY = '__yiReloadTransition';
  const TRANSITION_MS = 600;

  if (window[GUARD_KEY]) return;
  window[GUARD_KEY] = true;

  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  let navigationTimer = null;
  let hideTimer = null;

  function createOverlay() {
    let overlay = document.getElementById(OVERLAY_ID);
    if (overlay) return overlay;

    overlay = document.createElement('div');
    overlay.id = OVERLAY_ID;
    overlay.className = 'yi-page-transition';
    overlay.setAttribute('aria-hidden', 'true');
    overlay.innerHTML = `
      <div class="yi-transition-box" aria-label="Chargement YourID">
        <div class="yi-transition-mark">YI</div>
        <div class="yi-transition-dots" aria-hidden="true">
          <span></span>
          <span></span>
          <span></span>
        </div>
      </div>
    `;

    document.body.appendChild(overlay);
    return overlay;
  }

  function showTransition() {
    if (reduceMotion) return;
    const overlay = createOverlay();
    overlay.classList.add('is-visible');
    overlay.setAttribute('aria-hidden', 'false');
    document.body.classList.add('yi-transition-active');
  }

  function hideTransition() {
    clearTimeout(hideTimer);
    clearTimeout(navigationTimer);
    navigationTimer = null;

    const overlay = document.getElementById(OVERLAY_ID);
    if (!overlay) return;
    overlay.classList.remove('is-visible');
    overlay.setAttribute('aria-hidden', 'true');
    document.body.classList.remove('yi-transition-active');
  }

  function isInternalNavigationTarget(anchor) {
    if (!anchor || anchor.target === '_blank' || anchor.hasAttribute('download')) return false;

    const href = anchor.getAttribute('href');
    if (!href || href.startsWith('#')) return false;

    let url;
    try {
      url = new URL(anchor.href, window.location.href);
    } catch (error) {
      return false;
    }

    // Ignores mailto:, tel:, javascript:, other origins, etc.
    if (url.origin !== window.location.origin || !/^https?:$/.test(url.protocol)) return false;

    // Same page, only the hash changes: no transition needed
    if (
      url.hash &&
      url.pathname === window.location.pathname &&
      url.search === window.location.search
    ) {
      return false;
    }

    return true;
  }

  // One delegated listener: also works for links added later to the DOM
  function onDocumentClick(event) {
    if (
      event.defaultPrevented ||
      event.button !== 0 ||
      event.metaKey || event.ctrlKey || event.shiftKey || event.altKey
    ) {
      return;
    }

    const link = event.target instanceof Element ? event.target.closest('a[href]') : null;
    if (!isInternalNavigationTarget(link)) return;

    // Reduced motion: navigate normally, no artificial delay
    if (reduceMotion) return;

    event.preventDefault();
    if (navigationTimer) return; // navigation already in progress

    showTransition();
    const targetUrl = link.href;
    navigationTimer = setTimeout(() => {
      window.location.href = targetUrl;
    }, TRANSITION_MS);
  }

  function init() {
    if (reduceMotion) {
      hideTransition();
      return;
    }

    const shouldShowReloadTransition = sessionStorage.getItem(RELOAD_FLAG_KEY) === '1';
    if (shouldShowReloadTransition) {
      sessionStorage.removeItem(RELOAD_FLAG_KEY);
    }

    showTransition();
    hideTimer = setTimeout(hideTransition, TRANSITION_MS);

    if (!shouldShowReloadTransition) {
      window.addEventListener('beforeunload', () => {
        sessionStorage.setItem(RELOAD_FLAG_KEY, '1');
      }, { once: true });
    }
  }

  document.addEventListener('click', onDocumentClick);

  // Back/forward cache: avoids a page stuck behind the overlay
  window.addEventListener('pageshow', (event) => {
    if (event.persisted) hideTransition();
  });

  window.YIPageTransition = {
    show: showTransition,
    hide: hideTransition,
    init: init
  };

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init, { once: true });
  } else {
    init();
  }
})();