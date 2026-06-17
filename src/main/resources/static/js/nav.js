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
