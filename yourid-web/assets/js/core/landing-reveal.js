(function () {
  const root = document.documentElement;
  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  function revealElement(el) {
    if (!el || !(el instanceof Element)) return;
    el.classList.add('is-revealed');
  }

  function revealGroup(group) {
    if (!group || !(group instanceof Element)) return;
    const children = group.querySelectorAll(':scope > [data-reveal], :scope > .landing-reveal');
    children.forEach((el) => revealElement(el));
  }

  function initLandingReveal() {
    if (!root || reduceMotion) return () => {};

    root.classList.add('has-landing-anim');

    const groups = [...document.querySelectorAll('[data-reveal-group]')];
    const revealables = [...document.querySelectorAll('[data-reveal], .landing-reveal, .landing-hero-item')];
    const groupedEls = new Set();

    groups.forEach((group) => {
      const children = group.querySelectorAll(':scope > [data-reveal], :scope > .landing-reveal');
      children.forEach((el, index) => {
        groupedEls.add(el);
        el.style.setProperty('--reveal-delay', `${Math.min(index * 70, 420)}ms`);
      });
    });

    const toObserve = [];

    revealables.forEach((el) => {
      if (groupedEls.has(el)) return;
      el.style.setProperty('--reveal-delay', el.style.getPropertyValue('--reveal-delay') || '0ms');
      if (el.getBoundingClientRect().bottom < 0) {
        revealElement(el);
      } else {
        toObserve.push(el);
      }
    });

    groups.forEach((group) => {
      if (group.getBoundingClientRect().bottom < 0) {
        revealGroup(group);
      } else {
        toObserve.push(group);
      }
    });

    if (!('IntersectionObserver' in window)) {
      revealables.forEach(revealElement);
      groups.forEach(revealGroup);
      return () => {};
    }

    const observer = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (!entry.isIntersecting) return;
        observer.unobserve(entry.target);
        if (entry.target.hasAttribute('data-reveal-group')) {
          revealGroup(entry.target);
        } else {
          revealElement(entry.target);
        }
      });
    }, { rootMargin: '0px 0px -8% 0px' });

    toObserve.forEach((el) => observer.observe(el));
    return () => observer.disconnect();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initLandingReveal, { once: true });
  } else {
    initLandingReveal();
  }
})();
