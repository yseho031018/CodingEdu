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
