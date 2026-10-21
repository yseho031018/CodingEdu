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

    // 새 글 등록: 이 탭에 저장해 두고 목록 맨 위·마이페이지에 보여줌 (서버처럼 등록 후 목록으로)
    if (path === '/community/write') {
      e.preventDefault();
      var posts = loadNewPosts();
      posts.unshift({
        id: 'n' + Date.now(),
        category: form.querySelector('[name="category"]').value,
        title: form.querySelector('[name="title"]').value.trim(),
        content: form.querySelector('[name="content"]').value,
        createdAt: Date.now()
      });
      saveNewPosts(posts);
      flash('글을 등록했어요 (이 탭에서만 반영돼요)');
      location.href = communityListUrl();
      return;
    }
    // 새 글의 수정·삭제 (데모 계정 글 화면을 빌려 쓰므로 주소의 ?demoPost= 로 구분)
    if (demoPostId && /^\/community\/\d+\/(edit|delete)$/.test(path)) {
      e.preventDefault();
      e.stopPropagation();
      var list = loadNewPosts();
      var post = list.filter(function (p) { return p.id === demoPostId; })[0];
      if (!post) { location.href = communityListUrl(); return; }
      if (/\/delete$/.test(path)) {
        if (!window.confirm('게시글을 삭제할까요?')) return;
        saveNewPosts(list.filter(function (p) { return p.id !== demoPostId; }));
        flash('글을 삭제했어요 (이 탭에서만 반영돼요)');
        location.href = communityListUrl();
        return;
      }
      post.category = form.querySelector('[name="category"]').value;
      post.title = form.querySelector('[name="title"]').value.trim();
      post.content = form.querySelector('[name="content"]').value;
      saveNewPosts(list);
      flash('글을 수정했어요 (이 탭에서만 반영돼요)');
      location.href = newPostUrl(post);
      return;
    }

    // 게시글 수정·삭제: 이 탭에 저장해 두고 글·목록·마이페이지에 반영
    var postEdit = path.match(/^\/community\/(\d+)\/edit$/);
    if (postEdit) {
      e.preventDefault();
      var changes = loadPostChanges();
      changes[postEdit[1]] = {
        category: form.querySelector('[name="category"]').value,
        title: form.querySelector('[name="title"]').value.trim(),
        content: form.querySelector('[name="content"]').value
      };
      savePostChanges(changes);
      flash('글을 수정했어요 (이 탭에서만 반영돼요)');
      var back = form.querySelector('.btn-cancel');
      location.href = back ? back.href : communityListUrl();
      return;
    }
    var postDelete = path.match(/^\/community\/(\d+)\/delete$/);
    if (postDelete) {
      e.preventDefault();
      e.stopPropagation(); // 폼의 확인 창 대신 아래에서 한 번만 물어봄
      if (!window.confirm('게시글을 삭제할까요?')) return;
      var all = loadPostChanges();
      all[postDelete[1]] = { deleted: true };
      savePostChanges(all);
      flash('글을 삭제했어요 (이 탭에서만 반영돼요)');
      location.href = communityListUrl();
      return;
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

  // ── 게시글 수정·삭제 결과 (이 탭을 닫으면 처음 상태로)
  var POSTS_KEY = 'demoPostChanges';
  var CATEGORY = { qna: ['질문', '💡 질문', 'Q&A'], tips: ['정보', '🔥 꿀팁', '팁'], study: ['스터디', '📚 스터디', '스터디'], free: ['자유', '☕ 자유', '자유'] };
  function loadPostChanges() {
    try { return JSON.parse(sessionStorage.getItem(POSTS_KEY)) || {}; } catch (err) { return {}; }
  }
  function savePostChanges(changes) {
    try { sessionStorage.setItem(POSTS_KEY, JSON.stringify(changes)); } catch (err) { /* 저장소를 쓸 수 없으면 반영하지 않음 */ }
  }
  // 글 상세(…/community/16/) 또는 수정 화면(suffix 'edit/' → …/community/16/edit/)의 글 번호
  function postIdOf(url, suffix) {
    var path = new URL(url, location.href).pathname.replace(/index\.html$/, '');
    var m = path.match(/\/community\/(\d+)\/(edit\/)?$/);
    return m && (m[2] || '') === (suffix || '') ? m[1] : null;
  }
  function communityListUrl() {
    var link = document.querySelector('nav .nav-links a[href*="community"]');
    return link ? link.href : location.href;
  }
  function setText(rootEl, selector, text) {
    var el = rootEl.querySelector(selector);
    if (el) el.textContent = text;
  }
  function setCategory(el, base, key, labelIndex) {
    if (!el || !CATEGORY[key]) return;
    el.className = base + ' ' + key;
    el.textContent = CATEGORY[key][labelIndex];
  }

  function applyPostChanges() {
    var changes = loadPostChanges();
    if (!Object.keys(changes).length) return;

    var detailId = !demoPostId && postIdOf(location.href); // 새 글 화면은 applyNewPost가 채움
    var detail = detailId && changes[detailId];
    if (detail && detail.deleted) { location.replace(communityListUrl()); return; }
    if (detail) {
      document.title = detail.title + ' - CodeEdu';
      setText(document, '.detail-header h2', detail.title);
      setText(document, '.detail-content', detail.content);
      setCategory(document.querySelector('.detail-header .post-category'), 'post-category', detail.category, 1);
    }

    var editId = !demoPostId && postIdOf(location.href, 'edit/');
    var edit = editId && changes[editId];
    if (edit && !edit.deleted) {
      document.getElementById('category').value = edit.category;
      document.getElementById('title').value = edit.title;
      document.getElementById('content').value = edit.content;
    }

    document.querySelectorAll('a.community-post-row, a.post-item').forEach(function (row) {
      if (/[?&]demoPost=/.test(row.href)) return; // 새 글은 데모 계정 글 주소를 빌려 쓰므로 제외
      var change = changes[postIdOf(row.href)];
      if (!change) return;
      if (change.deleted) { row.remove(); return; }
      if (row.classList.contains('post-item')) {
        setText(row, '.post-item-title', change.title);
        setCategory(row.querySelector('.post-category-tag'), 'post-category-tag', change.category, 2);
      } else {
        setText(row, '.community-post-title', change.title);
        setText(row, '.community-post-preview', change.content.split('\n')[0]);
        setCategory(row.querySelector('.community-category'), 'community-category', change.category, 0);
      }
    });
  }

  // ── 새 글 (이 탭을 닫으면 처음 상태로). 상세·수정 화면은 데모 계정 글 화면에 ?demoPost=를 붙여 빌려 씀
  var NEW_POSTS_KEY = 'demoNewPosts';
  var demoPostId = new URLSearchParams(location.search).get('demoPost');
  function loadNewPosts() {
    try { return JSON.parse(sessionStorage.getItem(NEW_POSTS_KEY)) || []; } catch (err) { return []; }
  }
  function saveNewPosts(posts) {
    try { sessionStorage.setItem(NEW_POSTS_KEY, JSON.stringify(posts)); } catch (err) { /* 저장소를 쓸 수 없으면 반영하지 않음 */ }
  }
  function newPostUrl(post, edit) {
    var own = document.querySelector('meta[name="demo-own-post"]');
    if (!own) return communityListUrl();
    var url = new URL(edit ? 'edit/index.html' : 'index.html', new URL(own.content, location.href));
    url.search = '?demoPost=' + encodeURIComponent(post.id);
    return url.href;
  }
  function pad(n) { return String(n).padStart(2, '0'); }
  function formatDate(ms, pattern) {
    var d = new Date(ms);
    return pattern.replace('yyyy', d.getFullYear()).replace('MM', pad(d.getMonth() + 1)).replace('dd', pad(d.getDate()))
      .replace('HH', pad(d.getHours())).replace('mm', pad(d.getMinutes()));
  }
  function myNickname() {
    var el = document.querySelector('.nav-user-name');
    return el ? el.textContent.trim() : '데모학습자';
  }

  // 새 글 상세·수정 화면: 빌려 쓴 데모 계정 글의 내용·반응·댓글을 새 글 기준으로 바꿈
  function applyNewPost() {
    if (!demoPostId) return;
    var post = loadNewPosts().filter(function (p) { return p.id === demoPostId; })[0];
    if (!post) { location.replace(communityListUrl()); return; }

    if (postIdOf(location.href)) {
      document.title = post.title + ' - CodeEdu';
      setText(document, '.detail-header h2', post.title);
      setText(document, '.detail-content', post.content);
      setCategory(document.querySelector('.detail-header .post-category'), 'post-category', post.category, 1);
      setText(document, '.meta-date', formatDate(post.createdAt, 'yyyy-MM-dd HH:mm'));
      setText(document, '.meta-right span', '👁️ 1');
      ['likeCount', 'disappointedCount'].forEach(function (id) { var el = document.getElementById(id); if (el) el.textContent = '0'; });
      document.querySelectorAll('.reaction-btn').forEach(function (btn) {
        btn.classList.remove('active');
        setText(btn, '.reaction-icon', btn.classList.contains('like') ? '🤍' : '💭');
      });
      var comments = document.querySelector('.comment-list');
      if (comments) {
        comments.innerHTML = '';
        var empty = document.createElement('div');
        empty.setAttribute('style', 'font-size: .95rem; color: var(--ink-3); text-align: center; padding: 40px 0;');
        empty.textContent = '아직 작성된 댓글이 없습니다. 첫 번째 댓글을 남겨보세요! ✨';
        comments.appendChild(empty);
      }
      setText(document, '.comments-section h3 span', '0');
      var editLink = document.querySelector('.btn-edit');
      if (editLink) editLink.href = newPostUrl(post, true);
    }
    if (postIdOf(location.href, 'edit/')) {
      document.getElementById('category').value = post.category;
      document.getElementById('title').value = post.title;
      document.getElementById('content').value = post.content;
      var cancel = document.querySelector('.btn-cancel');
      if (cancel) cancel.href = newPostUrl(post);
      document.querySelectorAll('main a[data-appbar-back], .appbar-back').forEach(function (a) { a.href = newPostUrl(post); });
    }
  }

  // 커뮤니티 목록 첫 페이지(전체 또는 같은 카테고리)와 마이페이지 '내 게시글' 맨 위에 새 글 추가
  function addNewPostsToLists() {
    var posts = loadNewPosts();
    if (!posts.length) return;

    var listPage = location.pathname.replace(/index\.html$/, '').match(/\/community\/(?:(free|qna|study|tips)\/)?$/);
    var sampleRow = document.querySelector('a.community-post-row');
    if (listPage && sampleRow) {
      posts.slice().reverse().forEach(function (post) {
        if (listPage[1] && listPage[1] !== post.category) return;
        var row = sampleRow.cloneNode(true);
        row.href = newPostUrl(post);
        setCategory(row.querySelector('.community-category'), 'community-category', post.category, 0);
        setText(row, '.community-post-title', post.title);
        setText(row, '.community-post-preview', post.content.split('\n')[0]);
        var meta = row.querySelectorAll('.community-post-mobile-meta span');
        [myNickname(), '좋아요 0', '댓글 0', '조회 1'].forEach(function (text, i) { if (meta[i]) meta[i].textContent = text; });
        setText(row, '.community-post-author', myNickname());
        row.querySelectorAll('.community-post-stat').forEach(function (el) { el.textContent = '0'; });
        setText(row, '.community-post-date', formatDate(post.createdAt, 'MM-dd HH:mm'));
        sampleRow.parentNode.insertBefore(row, sampleRow.parentNode.querySelector('a.community-post-row'));
      });
    }

    var sampleItem = document.querySelector('a.post-item[href*="/community/"]');
    if (sampleItem) {
      posts.slice().reverse().forEach(function (post) {
        var item = sampleItem.cloneNode(true);
        item.href = newPostUrl(post);
        setCategory(item.querySelector('.post-category-tag'), 'post-category-tag', post.category, 2);
        setText(item, '.post-item-title', post.title);
        setText(item, '.post-item-meta', formatDate(post.createdAt, 'MM.dd') + ' · 👁 1');
        sampleItem.parentNode.insertBefore(item, sampleItem.parentNode.firstChild);
      });
    }
  }

  // 마이페이지: 새 글·삭제를 반영해 '작성 글' 숫자와 '총 N개'를 맞춤
  function adjustMyPostCount(before) {
    var delta = document.querySelectorAll('a.post-item[href*="/community/"]').length - before;
    if (!delta) return;
    var stat = Array.prototype.filter.call(document.querySelectorAll('.stat-box'), function (box) {
      return /작성 글/.test(box.textContent);
    })[0];
    var num = stat && stat.querySelector('.num');
    if (num) {
      var base = +(num.getAttribute('data-count-to') || num.textContent) || 0;
      var next = String(Math.max(0, base + delta));
      if (num.hasAttribute('data-count-to')) num.setAttribute('data-count-to', next); // 숫자 올라가는 효과가 이 값까지
      num.textContent = num.hasAttribute('data-count-to') ? '0' : next;
    }
    Array.prototype.forEach.call(document.querySelectorAll('.block-title span'), function (span) {
      var m = span.textContent.match(/^총 (\d+)개$/);
      if (m && /내 게시글/.test(span.parentNode.textContent)) span.textContent = '총 ' + Math.max(0, +m[1] + delta) + '개';
    });
  }

  // 다음 화면에서 잠깐 보여줄 안내 (수정·삭제 후 이동한 화면)
  function flash(text) {
    try { sessionStorage.setItem('demoFlash', text); } catch (err) { /* 무시 */ }
  }
  function showFlash() {
    var text;
    try { text = sessionStorage.getItem('demoFlash'); sessionStorage.removeItem('demoFlash'); } catch (err) { return; }
    if (!text) return;
    var toast = document.createElement('div');
    toast.className = 'demo-toast';
    toast.setAttribute('role', 'status');
    toast.textContent = text;
    document.body.appendChild(toast);
    setTimeout(function () { toast.classList.add('is-out'); }, 2600);
    setTimeout(function () { toast.remove(); }, 3000);
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
  var myPostsBefore = document.querySelectorAll('a.post-item[href*="/community/"]').length;
  applyNewPost();
  addNewPostsToLists();
  applyPostChanges();
  adjustMyPostCount(myPostsBefore);
  showFlash();
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', filterCommunity);
  else filterCommunity();
})();
