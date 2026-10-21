// 홈 학습 로드맵
// 1단계: 분야(큰 노드)와 강의(작은 노드)를 학습 순서 화살표로 잇는 지도
//        화면 크기와 관계없이 같은 배치를 유지하고, 좁은 화면은 확대/축소·이동(핀치·버튼·드래그)으로 봅니다.
//        마우스는 오버로 강조, 터치는 탭으로 강조 (같은 노드를 다시 탭하면 학습 순서로 이동)
// 2단계: 선택한 분야의 순서형 경로 패널
(function () {
  var root = document.getElementById('learningRoadmap');
  if (!root) return;

  var SVG_NS = 'http://www.w3.org/2000/svg';
  var graph = root.querySelector('[data-rm-graph]');
  var svg = root.querySelector('[data-rm-graph-svg]');
  var branches = Array.prototype.slice.call(root.querySelectorAll('.rm-branch'));
  var panels = Array.prototype.slice.call(root.querySelectorAll('.rm-panel'));
  var reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
  var selected = root.getAttribute('data-initial-track');
  var targetTimer = null;
  var focusedId = null;
  var lastPointer = 'mouse';
  var viewport = root.querySelector('[data-rm-viewport]');
  var zoomLabel = root.querySelector('[data-zoom-level]');
  var ZOOM_MIN = 1;
  var ZOOM_MAX = 4;
  var zoom = 1;

  // ── 배치. 분야는 가장자리, 공유 강의(JavaScript·Git)는 두 분야 사이에 둡니다.
  //    label: 라벨 위치(above/below/left/right). 지정하지 않으면 분야는 아래, 강의는 위.
  var LAYOUT = {
    width: 1200, height: 540, fieldR: 22, courseR: 7,
    field: { web: [110, 150], frontend: [640, 82], python: [1090, 140], backend: [1090, 430], database: [110, 410] },
    course: {
      html: [255, 95], css: [395, 150], javascript: [535, 220], project: [415, 322],
      react: [695, 275], nextjs: [825, 172], python: [955, 200], git: [860, 342],
      php: [720, 430], java: [565, 478], mysql: [255, 462], sqljoin: [390, 410]
    },
    label: { 'f:frontend': 'above' }
  };

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
  var trackByKey = {};
  tracks.forEach(function (t) { trackByKey[t.key] = t; });

  var nodes = {};   // id → { id, kind, key, name, tracks: {trackKey: stepNumber}, el, x, y, r }
  var edges = [];   // { from, to, tracks: {trackKey: true}, el }

  function addNode(id, kind, key, name, extra) {
    if (!nodes[id]) nodes[id] = { id: id, kind: kind, key: key, name: name, tracks: {} };
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
    addNode(fieldId, 'field', track.key, track.name, { track: track.key, meta: track.meta, summary: track.summary.trim() });
    var prev = fieldId;
    track.steps.forEach(function (step) {
      var id = 'c:' + step.lang;
      var node = addNode(id, 'course', step.lang, step.name, {});
      node.tracks[track.key] = step.number;
      if (!node.status || step.status === 'done' || step.status === 'upcoming') node.status = step.status;
      addEdge(prev, id, track.key);
      prev = id;
    });
  });

  // ── 공통 계산
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

  function relationText(node) {
    var n = neighbours(node.id);
    var prevCourses = n.prev.filter(function (p) { return p.kind === 'course'; }).map(function (p) { return p.name; });
    var nextCourses = n.next.map(function (p) { return p.name; });
    var text = prevCourses.length ? '선수 ' + prevCourses.join(', ') : '선수 강의 없음';
    if (nextCourses.length) text += ' · 다음 ' + nextCourses.join(', ');
    if (node.status === 'upcoming') text = '콘텐츠 준비 중 · ' + text;
    return text;
  }

  function captionFor(node) {
    return node.kind === 'field' ? '클릭해서 학습 순서 보기' : relationText(node);
  }

  // 이미 선택한 분야에 포함된 강의면 그 분야를, 아니면 이 강의가 속한 첫 분야
  function trackFor(node) {
    if (node.kind === 'field') return node.track;
    return node.tracks[selected] ? selected : Object.keys(node.tracks)[0];
  }

  function labelPos(side, r, kind) {
    switch (side) {
      case 'above': return kind === 'field' ? { x: 0, y: -r - 30, meta: -r - 12, anchor: 'middle' } : { x: 0, y: -r - 9, anchor: 'middle' };
      case 'left': return { x: -r - 8, y: 5, anchor: 'end' };
      case 'left-above': return { x: -r - 6, y: -r - 6, anchor: 'end' };
      case 'right': return { x: r + 8, y: 5, anchor: 'start' };
      default: return kind === 'field' ? { x: 0, y: r + 22, meta: r + 39, anchor: 'middle' } : { x: 0, y: r + 20, anchor: 'middle' }; // below
    }
  }

  // ── 그리기
  function drawGraph() {
    if (!svg) return;
    var layout = LAYOUT;
    clearFocus();
    svg.textContent = '';
    svg.setAttribute('viewBox', '0 0 ' + layout.width + ' ' + layout.height);

    Object.keys(nodes).forEach(function (id) {
      var n = nodes[id];
      var pos = (n.kind === 'field' ? layout.field : layout.course)[n.key] || [layout.width / 2, layout.height / 2];
      n.x = pos[0];
      n.y = pos[1];
      n.r = n.kind === 'field' ? layout.fieldR : layout.courseR;
    });

    var defs = el('defs', {}, svg);
    [['rm-arrow', 'rm-arrowhead'], ['rm-arrow-on', 'rm-arrowhead is-on'], ['rm-arrow-prev', 'rm-arrowhead is-prev']].forEach(function (m) {
      var marker = el('marker', { id: m[0], viewBox: '0 0 10 10', refX: '9', refY: '5', markerWidth: '7', markerHeight: '7', orient: 'auto-start-reverse' }, defs);
      el('path', { d: 'M0 1 L10 5 L0 9 z', 'class': m[1] }, marker);
    });

    // 빈 곳을 탭하면 선택 해제
    el('rect', { 'class': 'rm-gbg', x: 0, y: 0, width: layout.width, height: layout.height }, svg)
      .addEventListener('click', function () { clearFocus(); });

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
        : n.name + ' 강의' + (n.status === 'done' ? ', 완료' : '') + '. ' + relationText(n);
      var g = el('g', { 'class': cls, transform: 'translate(' + n.x + ' ' + n.y + ')', tabindex: '0', role: 'button', 'aria-label': label, 'data-id': id }, nodeLayer);
      el('circle', { 'class': 'rm-gring', r: n.r + 7 }, g);
      el('circle', { 'class': 'rm-gdot', r: n.r }, g);
      if (n.status === 'done') el('path', { 'class': 'rm-gcheck', d: n.kind === 'field' ? 'M-6 0 l4 4 l8 -8' : 'M-3 0 l2 2 l4 -4' }, g);

      var lp = labelPos(layout.label[id] || (n.kind === 'field' ? 'below' : 'above'), n.r, n.kind);
      var t = el('text', { 'class': 'rm-glabel', 'text-anchor': lp.anchor, x: lp.x, y: lp.y }, g);
      t.textContent = n.name;
      if (n.kind === 'field') {
        var m = el('text', { 'class': 'rm-gmeta', 'text-anchor': lp.anchor, x: lp.x, y: lp.meta }, g);
        m.textContent = n.meta;
      }
      var captionY = n.kind === 'field' ? (lp.meta > 0 ? lp.meta + 20 : n.r + 22) : n.r + 20;
      var c = el('text', { 'class': 'rm-gcaption', 'text-anchor': 'middle', y: captionY }, g);
      c.textContent = captionFor(n);
      // 판정은 강조로 커지는 점과 무관한 원 하나로만. 반지름은 확대 배율에 맞춰 updateHitAreas()에서 조정
      var hit = el('circle', { 'class': 'rm-ghit', r: n.r + (n.kind === 'field' ? 4 : 7) }, g);
      n.el = g;
      n.hit = hit;

      hit.addEventListener('pointerenter', function (ev) { if (ev.pointerType === 'mouse') focusNode(id); });
      hit.addEventListener('pointerleave', function (ev) { if (ev.pointerType === 'mouse') clearFocus(); });
      // 키보드로 이동했을 때만 포커스 강조 (클릭 후 남는 포커스로 강조가 유지되지 않게)
      g.addEventListener('focus', function () { if (g.matches(':focus-visible')) focusNode(id); });
      g.addEventListener('blur', function () { if (lastPointer === 'mouse') clearFocus(); });
      g.addEventListener('click', function () { onNodeClick(id); });
      g.addEventListener('keydown', function (ev) {
        if (ev.key === 'Enter' || ev.key === ' ') {
          ev.preventDefault();
          activate(id);
        }
      });
    });
  }

  // 마우스: 클릭하면 바로 학습 순서로 이동
  // 터치: 첫 탭은 강조하고 지도 아래 경로 패널을 그 분야·단계로 바꿈(화면은 그대로),
  //       같은 노드를 다시 탭하면 경로 패널이 화면 밖일 때 그 위치로 이동
  function onNodeClick(id) {
    if (lastPointer === 'mouse' || focusedId === id) {
      activate(id);
      return;
    }
    focusNode(id);
    activate(id, true);
  }

  // 올린(탭한) 노드만 커지고, 선수 강의(앰버)·다음 강의(파란 테두리)와 그 연결선만 남깁니다.
  // 분야 노드는 그 분야의 학습 경로 전체를 강조합니다.
  function focusNode(id) {
    clearFocus();
    var n = nodes[id];
    focusedId = id;
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
    focusedId = null;
    svg.classList.remove('is-hovering');
    Array.prototype.forEach.call(svg.querySelectorAll('.is-hot, .is-hl-prev, .is-hl-next, .is-from-prev'), function (e) {
      e.classList.remove('is-hot', 'is-hl-prev', 'is-hl-next', 'is-from-prev');
    });
  }

  function activate(id, stay) {
    var n = nodes[id];
    var key = trackFor(n);
    select(key, true, n.kind === 'field' ? null : n.tracks[key], stay);
  }

  // ── 선택 상태 (지도·트리·경로 패널 공통)
  function select(key, fromUser, stepNumber, stay) {
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
      if (!stay) revealPanel(active);
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

  // ── 확대/축소: 배치(구조)는 그대로 두고 지도 크기만 바꿔, 넘치는 부분은 지도 안에서 이동
  var coarse = window.matchMedia('(pointer: coarse)');

  function fitWidth() { return graph.clientWidth; }

  // 화면에서 손가락(약 44px)·마우스로 누르기 쉬운 크기가 되도록 판정 원 크기를 배율에 맞춤
  function updateHitAreas() {
    var scale = fitWidth() * zoom / LAYOUT.width;
    var target = coarse.matches ? 22 : 10;
    Object.keys(nodes).forEach(function (id) {
      var n = nodes[id];
      if (!n.hit) return;
      var base = n.r + (n.kind === 'field' ? 4 : 7);
      n.hit.setAttribute('r', Math.min(55, Math.max(base, target / scale)).toFixed(1));
    });
  }

  // px, py: 확대 기준점(지도 영역 안 좌표). 그 지점이 제자리에 있도록 스크롤을 맞춤
  function applyZoom(next, px, py) {
    next = Math.min(ZOOM_MAX, Math.max(ZOOM_MIN, next));
    var ratio = LAYOUT.height / LAYOUT.width;
    var prevW = parseFloat(svg.style.width) || fitWidth();
    if (px == null) px = viewport.clientWidth / 2;
    if (py == null) py = viewport.clientHeight / 2;
    var cx = (viewport.scrollLeft + px) / prevW;
    var cy = (viewport.scrollTop + py) / (prevW * ratio);

    zoom = next;
    var w = fitWidth() * zoom;
    var h = w * ratio;
    svg.style.width = w + 'px';
    svg.style.height = h + 'px';
    // 확대할수록 보이는 높이도 늘리되 화면 높이의 65%까지만, 그 이상은 지도 안에서 스크롤
    viewport.style.height = Math.round(Math.min(h, Math.max(fitWidth() * ratio, window.innerHeight * 0.65))) + 'px';
    graph.classList.toggle('is-zoomed', zoom > 1.001);
    viewport.scrollLeft = cx * w - px;
    viewport.scrollTop = cy * h - py;
    if (zoomLabel) zoomLabel.textContent = Math.round(zoom * 100) + '%';
    root.querySelectorAll('[data-zoom="out"], [data-zoom="fit"]').forEach(function (b) { b.disabled = zoom <= ZOOM_MIN + 0.001; });
    root.querySelectorAll('[data-zoom="in"]').forEach(function (b) { b.disabled = zoom >= ZOOM_MAX - 0.001; });
    updateHitAreas();
  }

  function setupZoom() {
    root.querySelectorAll('[data-zoom]').forEach(function (btn) {
      btn.addEventListener('click', function () {
        var k = btn.getAttribute('data-zoom');
        applyZoom(k === 'in' ? zoom * 1.5 : k === 'out' ? zoom / 1.5 : 1);
      });
    });

    // 두 손가락 핀치: 지도만 확대 (한 손가락은 기본 스크롤로 지도 이동)
    var pinch = null;
    function touchDist(t) { return Math.hypot(t[0].clientX - t[1].clientX, t[0].clientY - t[1].clientY); }
    viewport.addEventListener('touchstart', function (e) {
      if (e.touches.length === 2) pinch = { dist: touchDist(e.touches), zoom: zoom };
    }, { passive: true });
    viewport.addEventListener('touchmove', function (e) {
      if (!pinch || e.touches.length !== 2) return;
      e.preventDefault();
      var rect = viewport.getBoundingClientRect();
      applyZoom(pinch.zoom * touchDist(e.touches) / pinch.dist,
        (e.touches[0].clientX + e.touches[1].clientX) / 2 - rect.left,
        (e.touches[0].clientY + e.touches[1].clientY) / 2 - rect.top);
    }, { passive: false });
    viewport.addEventListener('touchend', function (e) { if (e.touches.length < 2) pinch = null; });

    // PC: Ctrl+휠(트랙패드 핀치)로 확대. 일반 휠은 페이지 스크롤 그대로
    viewport.addEventListener('wheel', function (e) {
      if (!e.ctrlKey) return;
      e.preventDefault();
      var rect = viewport.getBoundingClientRect();
      // 마우스 휠 한 칸(약 100)은 1.28배, 트랙패드 핀치의 작은 값은 부드럽게
      var delta = Math.max(-25, Math.min(25, e.deltaY));
      applyZoom(zoom * Math.exp(-delta * 0.01), e.clientX - rect.left, e.clientY - rect.top);
    }, { passive: false });

    // PC: 확대한 상태에서 마우스로 끌어 이동 (끌었을 때는 노드 클릭으로 처리하지 않음)
    var drag = null;
    var justDragged = false;
    viewport.addEventListener('pointerdown', function (e) {
      if (e.pointerType !== 'mouse' || e.button !== 0 || zoom <= 1.001) return;
      drag = { x: e.clientX, y: e.clientY, left: viewport.scrollLeft, top: viewport.scrollTop, moved: false };
    });
    window.addEventListener('pointermove', function (e) {
      if (!drag) return;
      var dx = e.clientX - drag.x, dy = e.clientY - drag.y;
      if (!drag.moved && Math.abs(dx) + Math.abs(dy) < 5) return;
      drag.moved = true;
      graph.classList.add('is-dragging');
      viewport.scrollLeft = drag.left - dx;
      viewport.scrollTop = drag.top - dy;
    });
    window.addEventListener('pointerup', function () {
      if (drag && drag.moved) {
        justDragged = true;
        setTimeout(function () { justDragged = false; }, 0);
      }
      drag = null;
      graph.classList.remove('is-dragging');
    });
    viewport.addEventListener('click', function (e) {
      if (justDragged) { e.stopPropagation(); e.preventDefault(); }
    }, true);

    // 화면 폭이 바뀌면 같은 배율로 다시 맞춤 (구조는 그대로)
    var lastW = 0;
    function onResize() {
      var w = fitWidth();
      if (Math.abs(w - lastW) < 1) return;
      lastW = w;
      graph.classList.toggle('is-narrow', w < 860);
      applyZoom(zoom);
    }
    onResize();
    if ('ResizeObserver' in window) new ResizeObserver(onResize).observe(graph);
    else window.addEventListener('resize', onResize);
  }

  if (svg && graph && viewport) {
    root.classList.add('has-graph');
    document.addEventListener('pointerdown', function (ev) { lastPointer = ev.pointerType || 'mouse'; }, true);
    drawGraph();
    setupZoom();
  }
  select(selected, false);
})();
