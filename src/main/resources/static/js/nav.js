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

// 상단 내비게이션 현재 위치 표시
// 실제 주소(a.pathname)로 비교해 서버(/learn)와 정적 목업(learn/index.html) 모두에서 동작
(function () {
  function clean(p) { return p.replace(/index\.html$/, '').replace(/\/$/, '') || '/'; }
  var path = clean(location.pathname);
  var logo = document.querySelector('nav .logo');
  var home = logo ? clean(logo.pathname) : '/';
  document.querySelectorAll('nav .nav-links a').forEach(function (a) {
    var target = clean(a.pathname);
    var on = target === home ? path === home : (path === target || path.indexOf(target + '/') === 0);
    if (on) {
      a.classList.add('active');
      a.setAttribute('aria-current', 'page');
    }
  });
})();

// 모바일 앱 바: 홈은 로고, 그 밖의 화면은 페이지 제목, 상세 화면은 뒤로가기 + 제목 (화면 표시는 mobile.css)
(function () {
  var nav = document.querySelector('nav');
  var links = nav && nav.querySelector('.nav-links');
  var heading = document.querySelector('main .course-page-heading h1, main .quiz-heading h1');
  if (!links || !heading) return;
  function clean(p) { return p.replace(/index\.html$/, '').replace(/\/$/, '') || '/'; }
  var active = links.querySelector('a.active');
  if (active && active.getAttribute('data-tab') === 'home') return;
  var isDetail = !!active && clean(location.pathname) !== clean(active.pathname);

  var bar = document.createElement('div');
  bar.className = 'appbar-title';
  if (isDetail) {
    var back = document.createElement('a');
    back.className = 'appbar-back';
    back.href = active.getAttribute('href');
    back.setAttribute('aria-label', active.textContent.trim() + ' 목록으로');
    back.innerHTML = '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M15 5 8 12l7 7" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/></svg>';
    // 같은 사이트에서 왔다면 브라우저 뒤로가기(스크롤 위치 유지), 아니면 목록으로
    back.addEventListener('click', function (e) {
      if (history.length > 1 && document.referrer.indexOf(location.origin) === 0) {
        e.preventDefault();
        history.back();
      }
    });
    bar.appendChild(back);
  }
  var title = document.createElement('strong');
  title.textContent = heading.textContent.trim();
  bar.appendChild(title);
  nav.insertBefore(bar, nav.querySelector('.nav-right'));
  document.body.classList.add('has-appbar-title');

  // 화면의 주요 동작(예: 커뮤니티 글쓰기)을 앱 바 오른쪽 버튼으로
  var action = document.querySelector('[data-appbar-action]');
  var right = nav.querySelector('.nav-right');
  if (action && right) {
    var btn = document.createElement('a');
    btn.className = 'appbar-action';
    btn.href = action.getAttribute('href');
    btn.innerHTML = '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 20h4L19 9l-4-4L4 16zM14 6l4 4" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg><span></span>';
    btn.querySelector('span').textContent = action.getAttribute('data-appbar-action');
    right.insertBefore(btn, right.firstChild);
  }
})();

// 모바일 하단 탭바 + 더보기 시트 (상단 내비 링크로 구성 → 서버·정적 목업 모두 같은 주소 사용)
(function () {
  var topNav = document.querySelector('nav .nav-links');
  if (!topNav || document.getElementById('mobileTabbar')) return;

  var ICONS = {
    home: '<path d="M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5.5v-6h-5v6H4a1 1 0 0 1-1-1z"/>',
    learn: '<path d="M4 4.5A1.5 1.5 0 0 1 5.5 3H19v15H5.5a1.5 1.5 0 0 0 0 3H19M8 7h7"/>',
    quiz: '<path d="M9 11.5 11 13.5 15.5 9M5 3h14v18H5z"/>',
    my: '<circle cx="12" cy="8" r="4"/><path d="M4 21c1.5-4 4.5-6 8-6s6.5 2 8 6"/>',
    more: '<circle cx="5" cy="12" r="1.6"/><circle cx="12" cy="12" r="1.6"/><circle cx="19" cy="12" r="1.6"/>'
  };
  function icon(name) {
    return '<svg viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">' + ICONS[name] + '</svg>';
  }
  function linkFor(tab) { return topNav.querySelector('a[data-tab="' + tab + '"]'); }

  // <nav>로 만들면 사이트 공통 nav 스타일(sticky·높이)이 적용되므로 div + role 사용
  var bar = document.createElement('div');
  bar.id = 'mobileTabbar';
  bar.className = 'mobile-tabbar';
  bar.setAttribute('role', 'navigation');
  bar.setAttribute('aria-label', '하단 메뉴');

  function addTab(name, label, source) {
    if (!source) return;
    var a = document.createElement('a');
    a.href = source.getAttribute('href');
    a.innerHTML = icon(name) + '<span>' + label + '</span>';
    var here = function (p) { return p.replace(/index\.html$/, '').replace(/\/$/, ''); };
    if (source.classList.contains('active') || (name === 'my' && here(source.pathname) === here(location.pathname))) {
      a.classList.add('active');
      a.setAttribute('aria-current', 'page');
      // 지금 보고 있는 탭을 다시 누르면 맨 위로 (모바일 앱 관습)
      a.addEventListener('click', function (e) {
        e.preventDefault();
        var reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
        window.scrollTo({ top: 0, behavior: reduce ? 'auto' : 'smooth' });
      });
    }
    bar.appendChild(a);
  }
  addTab('home', '홈', linkFor('home'));
  addTab('learn', '강의', linkFor('learn'));
  addTab('quiz', '퀴즈', linkFor('quiz'));
  // 로그인 상태면 오른쪽의 사용자 이름(마이페이지) 링크, 아니면 로그인 버튼
  var userLink = document.querySelector('nav .nav-right a.nav-user');
  var mySource = userLink || document.querySelector('nav .nav-right a.btn-login.ghost');
  addTab('my', userLink ? 'MY' : '로그인', mySource);

  var moreBtn = document.createElement('button');
  moreBtn.type = 'button';
  moreBtn.innerHTML = icon('more') + '<span>더보기</span>';
  moreBtn.setAttribute('aria-haspopup', 'dialog');
  moreBtn.setAttribute('aria-expanded', 'false');
  moreBtn.setAttribute('aria-controls', 'mobileMoreSheet');
  // 챌린지·커뮤니티·AI 도우미처럼 더보기 안의 화면에 있을 때는 더보기 탭을 현재 위치로 표시
  if (topNav.querySelector('a[data-tab="more"].active')) moreBtn.classList.add('active');
  bar.appendChild(moreBtn);

  // 더보기 시트: 나머지 메뉴 + 계정 메뉴
  var sheet = document.createElement('div');
  sheet.id = 'mobileMoreSheet';
  sheet.className = 'mobile-sheet';
  sheet.setAttribute('role', 'dialog');
  sheet.setAttribute('aria-modal', 'true');
  sheet.setAttribute('aria-label', '더보기 메뉴');
  sheet.hidden = true;
  var items = '';
  topNav.querySelectorAll('a[data-tab="more"]').forEach(function (a) {
    items += '<a href="' + a.getAttribute('href') + '">' + a.textContent.trim() + '<span aria-hidden="true">›</span></a>';
  });
  var accountLinks = document.querySelectorAll('nav .nav-right a[href]:not(.nav-mobile-only)');
  accountLinks.forEach(function (a) {
    var text = a.textContent.replace(/[⚙️🔔👤]/g, '').replace(/\s+/g, ' ').trim();
    if (a.classList.contains('nav-notify')) text = '알림';
    // 탭바에 이미 있는 로그인·마이페이지(사용자 이름) 링크는 제외
    if (!text || a === mySource || a.classList.contains('nav-user')) return;
    items += '<a href="' + a.getAttribute('href') + '">' + text + '<span aria-hidden="true">›</span></a>';
  });
  // 상단바의 폼은 로그아웃 폼 하나뿐 (정적 목업에서는 action 주소가 바뀌므로 위치로 찾음)
  var logoutForm = document.querySelector('nav .nav-right form');
  if (logoutForm) items += '<button type="button" data-logout>로그아웃</button>';
  sheet.innerHTML = '<div class="mobile-sheet-panel"><div class="mobile-sheet-head"><strong>더보기</strong>'
    + '<button type="button" class="mobile-sheet-close" aria-label="닫기">✕</button></div>'
    + '<div class="mobile-sheet-list">' + items + '</div></div>';

  function openSheet() {
    sheet.hidden = false;
    requestAnimationFrame(function () { sheet.classList.add('open'); });
    moreBtn.setAttribute('aria-expanded', 'true');
    var first = sheet.querySelector('.mobile-sheet-list a, .mobile-sheet-list button');
    if (first) first.focus();
  }
  function closeSheet() {
    sheet.classList.remove('open');
    moreBtn.setAttribute('aria-expanded', 'false');
    setTimeout(function () { sheet.hidden = true; }, 200);
    moreBtn.focus();
  }
  moreBtn.addEventListener('click', function () { sheet.hidden ? openSheet() : closeSheet(); });
  sheet.addEventListener('click', function (e) {
    if (e.target === sheet || e.target.closest('.mobile-sheet-close')) closeSheet();
    if (e.target.closest('[data-logout]') && logoutForm) logoutForm.submit();
  });
  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape' && !sheet.hidden) closeSheet();
  });

  document.body.appendChild(sheet);
  document.body.appendChild(bar);
  document.body.classList.add('has-mobile-tabbar');
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

// AI 학습 도우미 플로팅 챗봇
(function () {
  if (document.getElementById('ai-buddy')) return;
  if (document.body.classList.contains('page-ai')) return;

  var buddy = document.createElement('div');
  buddy.className = 'ai-buddy';
  buddy.id = 'ai-buddy';
  buddy.setAttribute('aria-label', 'AI 학습 도우미 챗봇');
  buddy.innerHTML = ''
    + '<div class="ai-buddy-panel" id="aiBuddyPanel" aria-hidden="true">'
    + '  <div class="ai-buddy-head">'
    + '    <span class="ai-buddy-face" aria-hidden="true"></span>'
    + '    <div><strong>AI 학습 도우미</strong><span class="ai-buddy-subtitle">막히는 부분을 짧게 정리해드릴게요.</span></div>'
    + '    <button class="ai-buddy-close" type="button" aria-label="AI 챗봇 닫기">&times;</button>'
    + '  </div>'
    + '  <div class="ai-buddy-body" id="aiBuddyBody">'
    + '    <div class="ai-buddy-msg">안녕하세요. 강의, 퀴즈, 커뮤니티 사용법을 빠르게 안내해드릴게요.</div>'
    + '    <div class="ai-buddy-chips">'
    + '      <button type="button" class="ai-buddy-chip" data-prompt="오답 해설">오답 해설</button>'
    + '      <button type="button" class="ai-buddy-chip" data-prompt="다음 강의 추천">다음 강의 추천</button>'
    + '      <button type="button" class="ai-buddy-chip" data-prompt="질문 글 정리">질문 글 정리</button>'
    + '    </div>'
    + '  </div>'
    + '  <form class="ai-buddy-form" id="aiBuddyForm">'
    + '    <input class="ai-buddy-input" id="aiBuddyInput" type="text" placeholder="궁금한 내용을 입력하세요" autocomplete="off">'
    + '    <button class="ai-buddy-send" type="submit" aria-label="AI 질문 보내기">↵</button>'
    + '  </form>'
    + '</div>'
    + '<button class="ai-buddy-toggle" type="button" aria-expanded="false" aria-controls="aiBuddyPanel">'
    + '  <span class="ai-buddy-face" aria-hidden="true"></span>'
    + '  <span class="ai-buddy-label">AI 도우미</span>'
    + '</button>';

  document.body.appendChild(buddy);

  var toggle = buddy.querySelector('.ai-buddy-toggle');
  var closeButton = buddy.querySelector('.ai-buddy-close');
  var panel = buddy.querySelector('#aiBuddyPanel');
  var body = buddy.querySelector('#aiBuddyBody');
  var form = buddy.querySelector('#aiBuddyForm');
  var input = buddy.querySelector('#aiBuddyInput');

  // ── 눈동자 시선 제어 ──
  var faces = buddy.querySelectorAll('.ai-buddy-face');
  var tracking = false; // 패널 열림 → 마우스 추적
  var thinking = false; // 질문 받고 고민 중 → 눈 중앙 고정
  var EYE_MAX = 3.2;    // 눈동자가 움직일 수 있는 최대 거리(px)

  function lookAt(clientX, clientY) {
    faces.forEach(function (face) {
      var r = face.getBoundingClientRect();
      var dx = clientX - (r.left + r.width / 2);
      var dy = clientY - (r.top + r.height / 2);
      var dist = Math.hypot(dx, dy) || 1;
      face.style.setProperty('--aiEyeX', (dx / dist * EYE_MAX).toFixed(2) + 'px');
      face.style.setProperty('--aiEyeY', (dy / dist * EYE_MAX).toFixed(2) + 'px');
    });
  }

  function centerEyes() {
    faces.forEach(function (face) {
      face.style.setProperty('--aiEyeX', '0px');
      face.style.setProperty('--aiEyeY', '0px');
    });
  }

  document.addEventListener('mousemove', function (event) {
    if (tracking && !thinking) lookAt(event.clientX, event.clientY);
  });

  function setOpen(open) {
    buddy.classList.toggle('open', open);
    toggle.setAttribute('aria-expanded', String(open));
    panel.setAttribute('aria-hidden', String(!open));
    tracking = open;
    if (!open) centerEyes();
    if (open) {
      window.setTimeout(function () {
        input.focus();
      }, 80);
    }
  }

  function answerFor(text) {
    var normalized = (text || '').toLowerCase();

    if (normalized.indexOf('오답') >= 0 || normalized.indexOf('퀴즈') >= 0) {
      return '퀴즈 결과 화면의 AI 오답 해설 코치에서 틀린 이유, 집중 개념, 다음 학습 방향을 확인할 수 있어요.';
    }
    if (normalized.indexOf('강의') >= 0 || normalized.indexOf('추천') >= 0) {
      return '처음이라면 HTML, CSS, JavaScript로 웹 기초를 잡고, 이후 Java나 PHP로 백엔드 기초까지 이어가는 흐름을 추천해요.';
    }
    if (normalized.indexOf('커뮤니티') >= 0 || normalized.indexOf('질문') >= 0) {
      return '질문 글은 무엇을 하려 했는지, 어떤 오류가 났는지, 시도한 방법 순서로 정리하면 답변을 받기 좋아요.';
    }
    return '지금은 발표용 간단 도우미라 강의, 퀴즈, 커뮤니티 흐름을 중심으로 안내해요. 궁금한 기능 이름을 입력해보세요.';
  }

  function csrfToken() {
    var match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
    return match ? decodeURIComponent(match[1]) : '';
  }

  function requestAiAnswer(text) {
    var headers = { 'Content-Type': 'application/json' };
    var token = csrfToken();
    if (token) headers['X-XSRF-TOKEN'] = token;

    return fetch('/api/ai/chat', {
      method: 'POST',
      headers: headers,
      credentials: 'same-origin',
      body: JSON.stringify({ message: text })
    })
      .then(function (response) {
        if (!response.ok) throw new Error('AI request failed');
        return response.json();
      })
      .then(function (data) {
        return data && data.reply ? data.reply : answerFor(text);
      })
      .catch(function () {
        return answerFor(text);
      });
  }

  function addMessage(text, isUser) {
    var message = document.createElement('div');
    message.className = 'ai-buddy-msg' + (isUser ? ' user' : '');
    message.textContent = text;
    body.appendChild(message);
    body.scrollTop = body.scrollHeight;
    return message;
  }

  function sendMessage(text) {
    addMessage(text, true);
    // 질문을 받고 고민하는 동안에는 눈을 원래 위치(중앙)로 고정
    thinking = true;
    centerEyes();
    var pending = addMessage('답변을 준비하고 있어요...', false);
    requestAiAnswer(text).then(function (reply) {
      pending.textContent = reply;
      body.scrollTop = body.scrollHeight;
      thinking = false; // 답변 완료 → 다시 마우스 추적
    });
  }

  toggle.addEventListener('click', function () {
    setOpen(!buddy.classList.contains('open'));
  });
  closeButton.addEventListener('click', function () {
    setOpen(false);
  });

  buddy.querySelectorAll('.ai-buddy-chip').forEach(function (chip) {
    chip.addEventListener('click', function () {
      var text = chip.dataset.prompt || chip.textContent;
      sendMessage(text);
    });
  });

  form.addEventListener('submit', function (event) {
    event.preventDefault();
    var text = input.value.trim();
    if (!text) return;
    input.value = '';
    sendMessage(text);
  });
})();

// 강의 썸네일 보정: maxresdefault가 없는 영상은 유튜브가 404를 "회색 플레이스홀더 JPEG"로
// 돌려줘 onerror가 발생하지 않는다. naturalWidth로 플레이스홀더/실패를 감지해 hqdefault로 교체.
(function () {
  function attach(img) {
    var swapped = false;
    function toFallback() {
      if (swapped) return;
      var fb = img.getAttribute('data-fallback-src');
      if (fb && img.src !== fb) { swapped = true; img.src = fb; }
    }
    function check() {
      // naturalWidth 0 = 로드 실패, <= 120 = 유튜브 회색 플레이스홀더(120x90)
      if (img.naturalWidth === 0 || img.naturalWidth <= 120) toFallback();
    }
    img.addEventListener('error', toFallback);
    if (img.complete) check();
    else img.addEventListener('load', check);
  }
  document.querySelectorAll('.course-youtube-thumbnail img[data-fallback-src]').forEach(attach);
})();
