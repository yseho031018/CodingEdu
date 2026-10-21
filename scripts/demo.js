// CodingEdu 정적 목업 전용 스크립트 (scripts/build_demo.py 가 docs/demo/demo.js 로 복사)
// 서버가 없는 목업에서 AI 도우미·반응·검색·완료 인증·댓글이 화면에서 동작하도록 흉내 냅니다.
(function () {
  var loggedIn = location.pathname.indexOf('/me/') !== -1;

  // ── AI 도우미: 서버(Gemini) 대신 키워드별 데모 답변
  function demoReply(message) {
    var m = (message || '').toLowerCase();
    if (/오답|퀴즈|틀린/.test(m)) {
      return '틀린 문제는 ① 내가 고른 답이 왜 맞다고 생각했는지 ② 정답의 근거가 되는 개념 ③ 관련 강의 구간 순서로 다시 보세요. '
        + '예를 들어 JavaScript 퀴즈라면 로드맵에서 JavaScript 노드를 눌러 선수 강의인 CSS까지 함께 복습하면 좋아요.';
    }
    if (/추천|다음|순서|로드맵|강의/.test(m)) {
      return 'HTML과 CSS를 마쳤다면 다음은 JavaScript예요. 그 뒤에는 목표에 따라 갈라져요. '
        + '화면을 만들고 싶다면 프론트엔드(React → Next.js), 서버가 궁금하다면 백엔드(Git → PHP → Java)를 추천해요. '
        + '홈의 학습 지도에서 각 분야의 순서를 확인할 수 있어요.';
    }
    if (/질문|커뮤니티|글/.test(m)) {
      return '질문 글은 [하려던 것] → [실제 결과·오류 메시지] → [시도한 방법] → [궁금한 점] 순서로 쓰면 답변을 빨리 받을 수 있어요. '
        + '코드는 문제가 생긴 부분만 짧게 붙여 주세요.';
    }
    if (/계획|오늘|루틴|챌린지/.test(m)) {
      return '오늘의 추천 루틴: ① 이어서 학습할 강의 1개 시청(약 15분) ② 관련 퀴즈 3문제 ③ 틀린 문제 해설 확인 ④ 노트에 핵심 3줄 정리. '
        + '챌린지에 참여하면 이 흐름을 매일 기록할 수 있어요.';
    }
    if (/css|박스|margin|padding/.test(m)) {
      return 'margin은 요소 바깥 여백, padding은 테두리 안쪽 여백이에요. 배경색은 padding 영역까지 칠해지고 margin에는 칠해지지 않아요.';
    }
    return '저는 CodingEdu 학습 도우미예요(목업 데모 답변). 오답 해설, 다음 강의 추천, 질문 글 정리, 오늘 학습 계획 중에서 물어보세요.';
  }

  function jsonResponse(data, status) {
    return new Response(JSON.stringify(data), { status: status || 200, headers: { 'Content-Type': 'application/json' } });
  }

  var realFetch = window.fetch ? window.fetch.bind(window) : null;
  window.fetch = function (input, init) {
    var url = typeof input === 'string' ? input : (input && input.url) || '';
    if (url.indexOf('/api/ai/chat') !== -1) {
      var message = '';
      try { message = JSON.parse((init && init.body) || '{}').message || ''; } catch (e) { /* 무시 */ }
      return new Promise(function (resolve) {
        setTimeout(function () { resolve(jsonResponse({ reply: demoReply(message), source: 'demo' })); }, 700);
      });
    }
    var reaction = url.match(/\/community\/\d+\/reaction\?type=(\w+)/);
    if (reaction) {
      if (!loggedIn) return Promise.resolve(jsonResponse({ error: '로그인하면 반응을 남길 수 있어요. 위의 로그인 버튼을 눌러보세요.' }));
      return Promise.resolve(jsonResponse(toggleReaction(reaction[1])));
    }
    return realFetch ? realFetch(input, init) : Promise.reject(new Error('fetch unavailable'));
  };

  // 좋아요·아쉬워요: 화면의 숫자를 기준으로 하나만 켜지도록 전환
  function toggleReaction(type) {
    var like = document.getElementById('likeCount');
    var sad = document.getElementById('disappointedCount');
    var counts = { LIKE: +(like && like.textContent) || 0, DISAPPOINTED: +(sad && sad.textContent) || 0 };
    var activeBtn = document.querySelector('.reaction-btn.active');
    var current = activeBtn ? activeBtn.dataset.reactionType : null;
    if (current) counts[current] = Math.max(0, counts[current] - 1);
    var active = current !== type;
    if (active) counts[type] += 1;
    return { likeCount: counts.LIKE, disappointedCount: counts.DISAPPOINTED, active: active, reactionType: type };
  }

  // ── 커뮤니티 검색·태그: 서버 대신 모든 목록 페이지의 글을 모아 바로 거름 (밑줄은 아무 글자 하나로 취급)
  function filterCommunity() {
    var keyword = new URLSearchParams(location.search).get('keyword');
    var rows = Array.prototype.slice.call(document.querySelectorAll('.community-post-row'));
    if (!keyword || !rows.length) return;
    var container = rows[0].parentNode;
    var input = document.querySelector('.community-search input[name="keyword"]');
    if (input) input.value = keyword;

    // 페이지 버튼에 적힌 다른 목록 페이지 주소
    var urls = [];
    document.querySelectorAll('.pagination [onclick]').forEach(function (btn) {
      var m = (btn.getAttribute('onclick') || '').match(/location\.href='([^']+)'/);
      if (!m) return;
      var u = new URL(m[1], location.href);
      u.search = '';
      if (u.pathname !== location.pathname && urls.indexOf(u.href) === -1) urls.push(u.href);
    });

    // 서버처럼 본문까지 찾도록 빌드 때 만든 검색 색인(글 번호 → 제목+본문)을 함께 사용
    var root = location.pathname.slice(0, location.pathname.indexOf('/community/') + '/community/'.length);
    var indexReq = fetch(root + 'search-index.json').then(function (r) { return r.json(); }).catch(function () { return {}; });

    Promise.all(urls.map(function (url) {
      return fetch(url).then(function (r) { return r.text(); }).then(function (text) {
        var doc = new DOMParser().parseFromString(text, 'text/html');
        return Array.prototype.map.call(doc.querySelectorAll('.community-post-row'), function (row) {
          row.setAttribute('href', new URL(row.getAttribute('href'), url).href); // 다른 폴더 기준 주소 보정
          return document.importNode(row, true);
        });
      }).catch(function () { return []; });
    }).concat([indexReq])).then(function (results) {
      var index = results.pop() || {};
      results.forEach(function (list) { list.forEach(function (row) { container.appendChild(row); rows.push(row); }); });
      var pattern = new RegExp(keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&').replace(/_/g, '.'), 'i');
      var visible = 0;
      rows.forEach(function (row) {
        var id = ((row.getAttribute('href') || '').match(/community\/(\d+)\//) || [])[1];
        var show = pattern.test(row.textContent) || (id && pattern.test(index[id] || ''));
        row.style.display = show ? '' : 'none';
        if (show) visible++;
      });
      var pager = document.querySelector('.pagination');
      if (pager) pager.style.display = 'none';
      if (!visible) {
        var empty = document.createElement('p');
        empty.className = 'demo-empty';
        empty.textContent = '\'' + keyword + '\'에 맞는 글이 없어요. 다른 검색어를 입력해보세요.';
        container.appendChild(empty);
      }
    });
  }

  // ── 서버에 저장하는 폼: 목업에서는 화면에서 바로 결과를 보여줌
  function onDemoSubmit(e) {
    var form = e.target.closest('form[data-demo-post]');
    if (!form) return;
    var path = form.getAttribute('data-demo-post');

    if (/^\/challenge\/\d+\/complete$/.test(path)) {
      e.preventDefault();
      var done = document.createElement('div');
      done.className = 'join-button done';
      done.textContent = '완료 인증됨';
      var leave = form.parentNode.querySelector('form[data-demo-post$="/leave"]');
      if (leave) leave.remove();
      form.replaceWith(done);
      return;
    }

    if (/^\/community\/\d+\/comment$/.test(path)) {
      e.preventDefault();
      var textarea = form.querySelector('textarea');
      var text = textarea && textarea.value.trim();
      if (!text) return;
      var list = document.querySelector('.comment-list');
      var sample = list && list.querySelector('div[style*="padding: 20px"]');
      var card = document.createElement('div');
      card.setAttribute('style', sample ? sample.getAttribute('style') : 'padding:20px; border-radius:12px; margin-bottom:12px; border:1px solid var(--border-card);');
      var nick = (document.querySelector('.nav-user-name') || {}).textContent || '데모학습자';
      var now = new Date();
      var stamp = now.getFullYear() + '-' + String(now.getMonth() + 1).padStart(2, '0') + '-' + String(now.getDate()).padStart(2, '0')
        + ' ' + String(now.getHours()).padStart(2, '0') + ':' + String(now.getMinutes()).padStart(2, '0');
      card.innerHTML = '<div style="display:flex; align-items:center; gap:12px; margin-bottom:10px;">'
        + '<span style="font-weight:800; color:var(--ink-1);"></span><span style="font-size:.85rem; color:var(--ink-3);"></span></div>'
        + '<div style="color:var(--ink-1); line-height:1.5; white-space:pre-wrap; font-size:.95rem;"></div>';
      card.querySelectorAll('span')[0].textContent = nick;
      card.querySelectorAll('span')[1].textContent = stamp;
      card.lastChild.textContent = text;
      Array.prototype.forEach.call(list.children, function (child) {
        if (/아직 작성된 댓글이 없습니다/.test(child.textContent)) child.remove();
      });
      list.appendChild(card);
      var count = document.querySelector('.comments-section h3 span');
      if (count) count.textContent = String((+count.textContent || 0) + 1);
      textarea.value = '';
    }

    // 설정: 서버 대신 화면에서 바로 결과를 보여줌 (목업이라 저장은 되지 않음)
    if (/^\/settings\//.test(path)) {
      e.preventDefault();
      settingsSubmit(path, form);
      return;
    }
    // 그 밖의 저장 기능은 목업 안내 화면으로 이동
  }

  function settingsAlert(text, type) {
    var wrap = document.querySelector('.settings-wrap');
    if (!wrap) return;
    wrap.querySelectorAll('.settings-alert').forEach(function (el) { el.remove(); });
    var box = document.createElement('div');
    box.className = 'settings-alert' + (type ? ' is-' + type : '');
    box.setAttribute('role', type === 'error' ? 'alert' : 'status');
    box.textContent = text;
    wrap.insertBefore(box, wrap.firstChild);
    box.scrollIntoView({ block: 'center', behavior: 'smooth' });
  }

  function settingsSubmit(path, form) {
    var details = form.closest('details');
    function done(message) {
      if (details) details.open = false;
      settingsAlert(message + ' (목업이라 실제로 저장되지는 않아요)', 'success');
    }
    if (path === '/settings/nickname') {
      var nick = form.querySelector('[name="newNickname"]').value.trim();
      if (!nick) return settingsAlert('닉네임을 입력해 주세요.', 'error');
      document.querySelectorAll('[data-field="nickname"], .nav-user-name').forEach(function (el) { el.textContent = nick; });
      document.querySelectorAll('[data-field="initial"]').forEach(function (el) { el.textContent = nick.charAt(0).toUpperCase(); });
      return done('닉네임이 변경되었어요.');
    }
    if (path === '/settings/email') {
      var email = form.querySelector('[name="newEmail"]').value.trim();
      document.querySelectorAll('[data-field="email"]').forEach(function (el) { el.textContent = email; });
      return done('이메일이 변경되었어요.');
    }
    if (path === '/settings/password') {
      var next = form.querySelector('[name="newPassword"]').value;
      if (next !== form.querySelector('[name="confirmPassword"]').value) {
        return settingsAlert('새 비밀번호가 서로 달라요. 다시 확인해 주세요.', 'error');
      }
      form.reset();
      return done('비밀번호가 변경되었어요.');
    }
    if (path === '/settings/delete-account') {
      form.reset();
      var sheet = form.closest('[role="dialog"]');
      if (sheet) sheet.hidden = true;
      settingsAlert('목업에서는 계정이 삭제되지 않아요. 실제 서비스에서는 비밀번호를 확인한 뒤 바로 삭제돼요.');
    }
  }

  // ── 알림: 서버처럼 알림 화면을 한 번 열면 다른 화면의 안 읽은 개수 배지를 지움 (이 탭을 닫으면 처음 상태로)
  try {
    if (/\/notifications\/(index\.html)?$/.test(location.pathname)) {
      sessionStorage.setItem('demoNotificationsRead', '1');
    } else if (sessionStorage.getItem('demoNotificationsRead')) {
      var bell = document.querySelector('nav .nav-notify');
      var badge = bell && bell.querySelector('span:not(.notify-emoji)');
      if (badge) badge.remove();
      if (bell) bell.setAttribute('aria-label', '알림');
    }
  } catch (err) { /* 저장소를 쓸 수 없으면 배지를 그대로 둠 */ }

  document.addEventListener('submit', onDemoSubmit, true);
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', filterCommunity);
  else filterCommunity();
})();
