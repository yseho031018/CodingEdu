// 홈 학습 로드맵
// 1단계: 분야(큰 노드)와 강의(작은 노드)를 학습 순서 화살표로 잇는 지도 (태블릿·모바일은 목록형 트리)
// 2단계: 선택한 분야의 순서형 경로 패널
(function () {
  var root = document.getElementById('learningRoadmap');
  if (!root) return;

  var SVG_NS = 'http://www.w3.org/2000/svg';
  var svg = root.querySelector('[data-rm-graph-svg]');
  var branches = Array.prototype.slice.call(root.querySelectorAll('.rm-branch'));
  var panels = Array.prototype.slice.call(root.querySelectorAll('.rm-panel'));
  var reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
  var selected = root.getAttribute('data-initial-track');
  var targetTimer = null;

  // ── 지도 배치 (viewBox 1200×540). 분야는 가장자리, 공유 강의(JavaScript·Git)는 두 분야 사이에 둡니다.
  var FIELD_POS = {
    web: [110, 150], frontend: [640, 82], python: [1090, 140], backend: [1090, 430], database: [110, 410]
  };
  var COURSE_POS = {
    html: [255, 95], css: [395, 150], javascript: [535, 220], project: [415, 322],
    react: [695, 275], nextjs: [825, 172], python: [955, 200], git: [860, 342],
    php: [720, 430], java: [565, 478], mysql: [255, 462], sqljoin: [390, 410]
  };
  var FIELD_R = 22;
  var COURSE_R = 7;

  // ── 데이터: 서버가 렌더링한 목록형 트리에서 분야·강의·진행 상태를 읽어옵니다.
  var tracks = branches.map(function (branch) {
    var meta = branch.querySelector('.rm-dest-meta span');
    return {
      key: branch.getAttribute('data-track'),
      name: branch.querySelector('h3').textContent.trim(),
      meta: meta ? meta.textContent.trim() : '',
      summary: (branch.querySelector('.rm-dest-summary') || {}).textContent || '',
      steps: Array.prototype.map.call(branch.querySelectorAll('.rm-node'), function (node) {
        return {
          lang: node.getAttribute('data-lang'),
          number: node.getAttribute('data-step'),
          name: node.querySelector('.rm-node-name').textContent.trim(),
          status: (node.className.match(/is-(done|current|next|later|upcoming)/) || [])[1]
        };
      })
    };
  });

  var nodes = {};   // id → { id, kind, name, x, y, r, tracks: {key: stepNumber}, el }
  var edges = [];   // { from, to, tracks: {key: true}, el }

  function addNode(id, kind, name, pos, extra) {
    if (!nodes[id]) {
      nodes[id] = { id: id, kind: kind, name: name, x: pos[0], y: pos[1], r: kind === 'field' ? FIELD_R : COURSE_R, tracks: {} };
    }
    for (var k in extra) nodes[id][k] = extra[k];
    return nodes[id];
  }

  function addEdge(from, to, trackKey) {
    for (var i = 0; i < edges.length; i++) {
      if (edges[i].from === from && edges[i].to === to) {
        edges[i].tracks[trackKey] = true;
        return;
      }
    }
    var t = {};
    t[trackKey] = true;
    edges.push({ from: from, to: to, tracks: t });
  }

  tracks.forEach(function (track) {
    var fieldId = 'f:' + track.key;
    addNode(fieldId, 'field', track.name, FIELD_POS[track.key] || [600, 270], { track: track.key, meta: track.meta, summary: track.summary.trim() });
    var prev = fieldId;
    track.steps.forEach(function (step) {
      var id = 'c:' + step.lang;
      var node = addNode(id, 'course', step.name, COURSE_POS[step.lang] || [600, 270], {});
      node.tracks[track.key] = step.number;
      if (!node.status || step.status === 'done' || step.status === 'upcoming') node.status = step.status;
      addEdge(prev, id, track.key);
      prev = id;
    });
  });

  // ── 그리기
  function el(name, attrs, parent) {
    var e = document.createElementNS(SVG_NS, name);
    for (var a in attrs) e.setAttribute(a, attrs[a]);
    if (parent) parent.appendChild(e);
    return e;
  }

  function neighbours(id) {
    var prev = [], next = [];
    edges.forEach(function (e) {
      if (e.to === id) prev.push(nodes[e.from]);
      if (e.from === id) next.push(nodes[e.to]);
    });
    return { prev: prev, next: next };
  }

  function captionFor(node) {
    if (node.kind === 'field') return '클릭해서 학습 순서 보기';
    var n = neighbours(node.id);
    var prevCourses = n.prev.filter(function (p) { return p.kind === 'course'; }).map(function (p) { return p.name; });
    var nextCourses = n.next.map(function (p) { return p.name; });
    var text = prevCourses.length ? '선수 ' + prevCourses.join(', ') : '선수 강의 없음';
    if (nextCourses.length) text += ' · 다음 ' + nextCourses.join(', ');
    if (node.status === 'upcoming') text = '콘텐츠 준비 중 · ' + text;
    return text;
  }

  function drawGraph() {
    if (!svg) return;
    var defs = el('defs', {}, svg);
    [['rm-arrow', 'rm-arrowhead'], ['rm-arrow-on', 'rm-arrowhead is-on'], ['rm-arrow-prev', 'rm-arrowhead is-prev']].forEach(function (m) {
      var marker = el('marker', { id: m[0], viewBox: '0 0 10 10', refX: '9', refY: '5', markerWidth: '7', markerHeight: '7', orient: 'auto-start-reverse' }, defs);
      el('path', { d: 'M0 1 L10 5 L0 9 z', 'class': m[1] }, marker);
    });

    var edgeLayer = el('g', { 'class': 'rm-edges' }, svg);
    var nodeLayer = el('g', { 'class': 'rm-gnodes' }, svg);

    edges.forEach(function (e) {
      var a = nodes[e.from], b = nodes[e.to];
      var dx = b.x - a.x, dy = b.y - a.y, len = Math.sqrt(dx * dx + dy * dy) || 1;
      var ux = dx / len, uy = dy / len;
      var x1 = a.x + ux * (a.r + 3), y1 = a.y + uy * (a.r + 3);
      var x2 = b.x - ux * (b.r + 5), y2 = b.y - uy * (b.r + 5);
      e.el = el('path', { 'class': 'rm-edge', d: 'M' + x1.toFixed(1) + ' ' + y1.toFixed(1) + ' L' + x2.toFixed(1) + ' ' + y2.toFixed(1) }, edgeLayer);
    });

    Object.keys(nodes).forEach(function (id) {
      var n = nodes[id];
      var cls = 'rm-gnode is-' + n.kind + (n.status === 'done' || n.status === 'upcoming' ? ' is-' + n.status : '');
      var label = n.kind === 'field'
        ? n.name + ' 분야, ' + n.meta + '. ' + n.summary
        : n.name + ' 강의' + (n.status === 'done' ? ', 완료' : '') + '. ' + captionFor(n);
      var g = el('g', { 'class': cls, transform: 'translate(' + n.x + ' ' + n.y + ')', tabindex: '0', role: 'button', 'aria-label': label, 'data-id': id }, nodeLayer);
      el('circle', { 'class': 'rm-gring', r: n.r + 7 }, g);
      el('circle', { 'class': 'rm-gdot', r: n.r }, g);
      if (n.status === 'done') el('path', { 'class': 'rm-gcheck', d: n.kind === 'field' ? 'M-6 0 l4 4 l8 -8' : 'M-3 0 l2 2 l4 -4' }, g);

      var labelY, metaY, captionY;
      if (n.kind === 'field') {
        var above = n.y < 120;
        labelY = above ? -n.r - 30 : n.r + 24;
        metaY = above ? -n.r - 12 : n.r + 42;
        captionY = above ? n.r + 22 : n.r + 60;
      } else {
        labelY = -n.r - 9;
        captionY = n.r + 20;
      }
      var t = el('text', { 'class': 'rm-glabel', 'text-anchor': 'middle', y: labelY }, g);
      t.textContent = n.name;
      if (n.kind === 'field') {
        var m = el('text', { 'class': 'rm-gmeta', 'text-anchor': 'middle', y: metaY }, g);
        m.textContent = n.meta;
      }
      var c = el('text', { 'class': 'rm-gcaption', 'text-anchor': 'middle', y: captionY }, g);
      c.textContent = captionFor(n);
      // 마우스 판정은 크기가 변하지 않는 이 원 하나로만 합니다. (커진 점·라벨·숨은 캡션은 판정에서 제외)
      var hit = el('circle', { 'class': 'rm-ghit', r: n.r + (n.kind === 'field' ? 4 : 7) }, g);
      n.el = g;

      hit.addEventListener('pointerenter', function () { focusNode(id); });
      hit.addEventListener('pointerleave', clearFocus);
      // 키보드로 이동했을 때만 포커스 강조 (클릭 후 남는 포커스로 강조가 유지되지 않게)
      g.addEventListener('focus', function () { if (g.matches(':focus-visible')) focusNode(id); });
      g.addEventListener('blur', clearFocus);
      g.addEventListener('click', function () { activate(id); });
      g.addEventListener('keydown', function (ev) {
        if (ev.key === 'Enter' || ev.key === ' ') {
          ev.preventDefault();
          activate(id);
        }
      });
    });
  }

  // 올린 노드만 커지고, 선수 강의(앰버)·다음 강의(파란 테두리)와 그 연결선만 남깁니다.
  // 분야 노드는 그 분야의 학습 경로 전체를 강조합니다.
  function focusNode(id) {
    clearFocus();
    var n = nodes[id];
    svg.classList.add('is-hovering');
    n.el.classList.add('is-hot');

    if (n.kind === 'field') {
      edges.forEach(function (e) {
        if (e.tracks[n.track]) {
          e.el.classList.add('is-hot');
          nodes[e.to].el.classList.add('is-hl-next');
        }
      });
      return;
    }
    edges.forEach(function (e) {
      if (e.to === id) {
        e.el.classList.add('is-hot');
        var from = nodes[e.from];
        if (from.kind === 'course') {
          e.el.classList.add('is-from-prev');
          from.el.classList.add('is-hl-prev');
        } else {
          from.el.classList.add('is-hl-next');
        }
      } else if (e.from === id) {
        e.el.classList.add('is-hot');
        nodes[e.to].el.classList.add('is-hl-next');
      }
    });
  }

  function clearFocus() {
    if (!svg) return;
    svg.classList.remove('is-hovering');
    Array.prototype.forEach.call(svg.querySelectorAll('.is-hot, .is-hl-prev, .is-hl-next, .is-from-prev'), function (e) {
      e.classList.remove('is-hot', 'is-hl-prev', 'is-hl-next', 'is-from-prev');
    });
  }

  function activate(id) {
    var n = nodes[id];
    if (n.kind === 'field') {
      select(n.track, true);
      return;
    }
    // 이미 선택한 분야에 포함된 강의면 그 분야를, 아니면 이 강의가 속한 첫 분야를 엽니다.
    var key = n.tracks[selected] ? selected : Object.keys(n.tracks)[0];
    select(key, true, n.tracks[key]);
  }

  // ── 선택 상태 (지도·트리·경로 패널 공통)
  function select(key, fromUser, stepNumber) {
    if (!key) return;
    var changed = key !== selected;
    selected = key;

    branches.forEach(function (branch) {
      var on = branch.getAttribute('data-track') === key;
      branch.classList.toggle('is-selected', on);
      var btn = branch.querySelector('.rm-dest-btn');
      if (btn) btn.setAttribute('aria-pressed', on ? 'true' : 'false');
    });

    var active = null;
    panels.forEach(function (panel) {
      var on = panel.getAttribute('data-panel') === key;
      panel.hidden = !on;
      panel.classList.remove('is-entering');
      if (on) active = panel;
    });

    if (active && fromUser) {
      if (changed) {
        void active.offsetWidth; // 애니메이션 재시작
        active.classList.add('is-entering');
      }
      markTarget(active, stepNumber);
      revealPanel(active);
    }
  }

  // 지도에서 고른 강의를 경로 패널의 해당 단계 카드에 잠시 표시
  function markTarget(panel, stepNumber) {
    clearTimeout(targetTimer);
    Array.prototype.forEach.call(root.querySelectorAll('.rm-step.is-target'), function (e) {
      e.classList.remove('is-target');
    });
    if (!stepNumber) return;
    var step = panel.querySelector('.rm-step[data-step="' + stepNumber + '"]');
    if (!step) return;
    step.classList.add('is-target');
    targetTimer = setTimeout(function () { step.classList.remove('is-target'); }, 1600);
  }

  // 선택 결과가 화면 밖에 있으면 경로 패널 상단까지 부드럽게 이동
  function revealPanel(panel) {
    var rect = panel.getBoundingClientRect();
    var navOffset = 80;
    if (rect.top < navOffset || rect.top > window.innerHeight * 0.75) {
      window.scrollTo({
        top: window.pageYOffset + rect.top - navOffset,
        behavior: reduceMotion.matches ? 'auto' : 'smooth'
      });
    }
  }

  branches.forEach(function (branch) {
    var key = branch.getAttribute('data-track');
    branch.querySelector('.rm-dest').addEventListener('click', function () {
      select(key, true);
    });
    Array.prototype.forEach.call(branch.querySelectorAll('.rm-node'), function (node) {
      node.addEventListener('click', function () {
        select(key, true, node.getAttribute('data-step'));
      });
    });
  });

  drawGraph();
  select(selected, false);
})();
