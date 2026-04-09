// 스크롤 리빌 애니메이션 (전체 페이지 공통)
(function () {
  var els = document.querySelectorAll('.reveal');
  if (!els.length) return;
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    els.forEach(function (el) { el.classList.add('visible'); });
    return;
  }
  var obs = new IntersectionObserver(function (entries) {
    entries.forEach(function (entry) {
      if (entry.isIntersecting) entry.target.classList.add('visible');
      else entry.target.classList.remove('visible');
    });
  }, { threshold: 0.10 });
  els.forEach(function (el) { obs.observe(el); });
})();

// 다크모드 토글
(function () {
  var html = document.documentElement;
  var btn = document.getElementById('themeToggle');
  if (!btn) return;
  btn.addEventListener('click', function () {
    var next = html.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
    html.setAttribute('data-theme', next);
    localStorage.setItem('theme', next);
  });
})();

// 모바일 햄버거 메뉴 토글
(function () {
  const btn = document.getElementById('nav-hamburger');
  const menu = document.getElementById('nav-mobile-menu');
  const overlay = document.getElementById('nav-overlay');
  if (!btn || !menu) return;

  function open() {
    menu.style.display = 'flex';
    menu.classList.add('open');
    overlay && overlay.classList.add('show');
    btn.classList.add('open');
    document.body.style.overflow = 'hidden';
  }
  function close() {
    menu.style.display = 'none';
    menu.classList.remove('open');
    overlay && overlay.classList.remove('show');
    btn.classList.remove('open');
    document.body.style.overflow = '';
  }

  btn.addEventListener('click', () => menu.classList.contains('open') ? close() : open());
  overlay && overlay.addEventListener('click', close);
  menu.querySelectorAll('a, button[type=submit]').forEach(el => el.addEventListener('click', close));
})();
