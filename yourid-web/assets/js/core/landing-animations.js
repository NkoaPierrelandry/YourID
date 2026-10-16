(function () {
  const EXTRA_ICONS = {
    download: '<path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/>',
    shield: '<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/><polyline points="9 12 11 14 15 10"/>'
  };

  function renderDataIcons() {
    if (typeof document === 'undefined') return;

    document.querySelectorAll('[data-icon]').forEach((el) => {
      const name = el.dataset.icon;
      if (!name) return;

      if (typeof Icons !== 'undefined' && Icons[name]) {
        el.innerHTML = Icons[name](22);
        return;
      }

      if (typeof icon !== 'undefined' && EXTRA_ICONS[name]) {
        el.innerHTML = icon(EXTRA_ICONS[name], 22);
      }
    });
  }

  /**
   * Animations de la landing page uniquement.
   *
   * - Chargement : les éléments du premier écran apparaissent un par un (cascade).
   * - Scroll vers le bas : les éléments montent (fade-up) en entrant par le bas.
   * - Scroll vers le haut : les éléments descendent (fade-down) en entrant par le haut.
   * - Les éléments qui sortent de l'écran se réarment, donc l'animation rejoue
   *   dans les deux sens. Option { once: true } pour n'animer qu'une seule fois.
   *
   * Marquage HTML (déjà en place chez toi) :
   *   .landing-hero-item   → séquence d'entrée du hero (animation CSS)
   *   .landing-reveal      → apparition au scroll
   *   data-anim="fade|slide-left|slide-right|scale"  (optionnel, défaut : fade-up/down)
   *   data-delay="200"     (optionnel, force un délai en ms)
   *
   * Script classique (sans import/export), compatible avec tes autres fichiers
   * (api.js, icons.js). À charger uniquement sur la page d'accueil.
   * Réglage : <html data-landing-once> pour n'animer qu'une seule fois.
   * Retourne une fonction de nettoyage (utile pour les tests).
   */
  function initLandingAnimations({ once = document.documentElement.hasAttribute('data-landing-once') } = {}) {
    if (typeof window === 'undefined') return () => {};

    const html = document.documentElement;
    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    // Les icônes doivent être dessinées même si l'animation est désactivée.
    renderDataIcons();

    // Pas d'animation : tout reste visible, rien n'est masqué.
    if (reduceMotion || !('IntersectionObserver' in window)) return () => {};

    const STEP_INTRO = 110;  // ms entre chaque élément au chargement
    const STEP_GROUP = 90;   // ms entre frères d'un même bloc pendant le scroll
    const MAX_INTRO = 12;    // plafond pour que la séquence reste courte
    const MAX_GROUP = 6;

    const heroItems = [...document.querySelectorAll('.landing-hero-item')];
    const reveals = [...document.querySelectorAll('.landing-reveal')];
    const doneTimers = new Map();
    const vh = window.innerHeight;

    // Active le mode animé (le CSS masque seulement avec .js) et signale que le script tourne.
    html.classList.add('js');
    window.__landingAnim = true;

    const byDocOrder = (a, b) =>
      a.compareDocumentPosition(b) & Node.DOCUMENT_POSITION_FOLLOWING ? -1 : 1;

    const groupDelay = (el) => {
      if (el.dataset.delay) return Number(el.dataset.delay);
      const siblings = [...el.parentElement.children].filter((n) => n.classList.contains('landing-reveal'));
      return Math.min(siblings.indexOf(el), MAX_GROUP) * STEP_GROUP;
    };

    const setDelay = (el, ms) => el.style.setProperty('--landing-delay', `${ms}ms`);

    // ── 1. Séquence de chargement : tout le premier écran, un élément après l'autre ──
    const intro = [...heroItems, ...reveals]
      .filter((el) => el.getBoundingClientRect().top < vh * 0.95)
      .sort(byDocOrder);

    intro.forEach((el, i) => {
      const ms = 120 + Math.min(i, MAX_INTRO) * STEP_INTRO;
      setDelay(el, el.dataset.delay ? Number(el.dataset.delay) : ms);
    });

    // Les autres éléments : cascade par groupe pendant le scroll.
    reveals.filter((el) => !intro.includes(el)).forEach((el) => setDelay(el, groupDelay(el)));

    // Une fois l'intro terminée, tous les éléments passent en cascade par groupe.
    const introEnd = 120 + Math.min(intro.length, MAX_INTRO) * STEP_INTRO + 900;
    const introTimer = window.setTimeout(() => reveals.forEach((el) => setDelay(el, groupDelay(el))), introEnd);

    // ── 2. Direction d'entrée initiale (utile si la page se recharge déjà scrollée) ──
    const setFrom = (el) => {
      el.dataset.from = el.getBoundingClientRect().top < 0 ? 'top' : 'bottom';
    };
    reveals.forEach(setFrom);

    // ── 3. Observation du scroll, dans les deux sens ──
    const show = (el) => {
      el.classList.add('is-visible');
      window.clearTimeout(doneTimers.get(el));
      // is-done rend la main au hover des cartes une fois l'animation terminée.
      doneTimers.set(el, window.setTimeout(() => el.classList.add('is-done'), 1100));
    };

    const hide = (el) => {
      window.clearTimeout(doneTimers.get(el));
      el.classList.remove('is-visible', 'is-done');
      // Sorti par le haut → rentrera par le haut (fade-down), sinon par le bas.
      setFrom(el);
    };

    const io = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            show(entry.target);
            if (once) io.unobserve(entry.target);
          } else if (!once) {
            hide(entry.target);
          }
        }
      },
      { rootMargin: '0px 0px -8% 0px', threshold: 0 }
    );

    reveals.forEach((el) => io.observe(el));

    // ── Nettoyage ──
    return () => {
      io.disconnect();
      window.clearTimeout(introTimer);
      doneTimers.forEach((t) => window.clearTimeout(t));
      html.classList.remove('js');
      reveals.forEach((el) => {
        el.classList.remove('is-visible', 'is-done');
        el.removeAttribute('data-from');
        el.style.removeProperty('--landing-delay');
      });
      heroItems.forEach((el) => el.style.removeProperty('--landing-delay'));
      delete window.__landingAnim;
    };
  }

  function initCommissionCarousel() {
    const carousel = document.querySelector('.commission-carousel');
    if (!carousel) return;

    const cards = [...carousel.querySelectorAll('.commission-card')];
    const dots = [...carousel.querySelectorAll('.commission-dot')];
    if (!cards.length) return;

    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const intervalMs = 2000;
    let activeIndex = 0;
    let timerId = null;

    const setActive = (nextIndex) => {
      activeIndex = (nextIndex + cards.length) % cards.length;

      cards.forEach((card, index) => {
        card.classList.toggle('is-active', index === activeIndex);
      });

      dots.forEach((dot, index) => {
        const isActive = index === activeIndex;
        dot.classList.toggle('is-active', isActive);
        dot.setAttribute('aria-pressed', String(isActive));
      });
    };

    const startLoop = () => {
      if (reduceMotion) return;
      window.clearInterval(timerId);
      timerId = window.setInterval(() => setActive(activeIndex + 1), intervalMs);
    };

    dots.forEach((dot, index) => {
      dot.addEventListener('click', () => {
        setActive(index);
        startLoop();
      });
    });

    setActive(0);
    startLoop();
  }

  window.initLandingAnimations = initLandingAnimations;
  window.initCommissionCarousel = initCommissionCarousel;

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function () {
      renderDataIcons();
      initLandingAnimations();
      initCommissionCarousel();
    });
  } else {
    renderDataIcons();
    initLandingAnimations();
    initCommissionCarousel();
  }
})();