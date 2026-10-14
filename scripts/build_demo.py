"""CodingEdu 정적 목업(docs/demo) 빌드 스크립트.

공유 DB를 건드리지 않도록 H2 메모리 DB로 띄운 로컬 서버에서 화면을 받아 정적 HTML로 내보냅니다.
  1) 비로그인 화면            → docs/demo/
  2) 데모 계정 로그인 화면     → docs/demo/me/
     (스크립트가 메모리 DB에 데모 계정을 만들고 강의 진도·퀴즈 기록을 남긴 뒤 캡처)
목업에서 로그인/회원가입을 누르면 2)의 같은 화면으로, 로그아웃을 누르면 1)로 이동합니다.

사용법:
  1) H2 서버 실행 (.claude/launch.json 의 codingedu-h2 와 동일)
     mvn spring-boot:run -Dspring-boot.run.useTestClasspath=true \
       "-Dspring-boot.run.arguments=--server.port=8098 --spring.datasource.url=jdbc:h2:mem:demo;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1 --spring.datasource.driver-class-name=org.h2.Driver --spring.datasource.username=sa --spring.datasource.password= --spring.jpa.hibernate.ddl-auto=create-drop"
  2) python scripts/build_demo.py
"""
import http.cookiejar
import json
import os
import re
import secrets
import shutil
import urllib.parse
import urllib.request

BASE = os.environ.get('DEMO_BASE', 'http://localhost:8098')
PROJECT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(PROJECT, 'docs', 'demo')
STATIC = os.path.join(PROJECT, 'src', 'main', 'resources', 'static')
LANGS = ['html', 'css', 'javascript', 'java', 'php', 'mysql', 'sqljoin', 'python', 'react', 'nextjs', 'git']

PAGES = {'/': 'index.html', '/learn': 'learn/index.html', '/quiz': 'quiz/index.html',
         '/quiz/practice': 'quiz/practice/index.html'}
for _lang in LANGS:
    PAGES['/learn/' + _lang] = 'learn/' + _lang + '/index.html'
ME_ONLY = {'/mypage': 'mypage/index.html'}

# 데모 계정: 메모리 DB에만 존재. 비밀번호는 매 빌드마다 무작위로 만들고 저장하지 않음.
DEMO_USER = {'username': 'demo_learner', 'nickname': '데모학습자', 'email': 'demo@example.com',
             'password': secrets.token_urlsafe(12)}
# 데모 진도: 웹 개발 입문 1단계 완료, 2단계 진행 중, 데이터베이스 시작
DEMO_PROGRESS = {'html': 28, 'css': 6, 'mysql': 4}
DEMO_QUIZ_COUNT = 3


def session():
    jar = http.cookiejar.CookieJar()
    return urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))


def get(opener, path):
    with opener.open(BASE + path) as res:
        return res.read().decode('utf-8')


def csrf_from(html):
    m = re.search(r'name="_csrf" value="([^"]+)"', html) or re.search(r'name="_csrf" content="([^"]+)"', html)
    if not m:
        raise RuntimeError('CSRF token not found')
    return m.group(1)


def post_form(opener, path, data, csrf):
    body = urllib.parse.urlencode(dict(data, _csrf=csrf)).encode()
    with opener.open(urllib.request.Request(BASE + path, data=body, method='POST')) as res:
        return res.read().decode('utf-8')


def prepare_demo_account():
    op = session()
    post_form(op, '/register', DEMO_USER, csrf_from(get(op, '/register')))
    post_form(op, '/login_process', {'username': DEMO_USER['username'], 'password': DEMO_USER['password']},
              csrf_from(get(op, '/login')))
    home = get(op, '/')
    if 'demo_learner' not in home and DEMO_USER['nickname'] not in home:
        raise RuntimeError('demo login failed')

    token = csrf_from(home)
    header = re.search(r'name="_csrf_header" content="([^"]+)"', get(op, '/learn')).group(1)
    for lang, count in DEMO_PROGRESS.items():
        for idx in range(count):
            req = urllib.request.Request(BASE + '/api/learn/%s/complete' % lang, method='POST',
                                         data=json.dumps({'lessonIdx': idx}).encode(),
                                         headers={'Content-Type': 'application/json', header: token})
            op.open(req).read()

    quiz_ids = []
    for qid in re.findall(r'href="/quiz/(\d+)"', get(op, '/quiz')):
        if qid not in quiz_ids:
            quiz_ids.append(qid)
    quiz_ids = quiz_ids[:DEMO_QUIZ_COUNT]
    answer_keys = probe_answer_keys(quiz_ids)
    for qid in quiz_ids:
        page = get(op, '/quiz/' + qid)
        questions = question_choices(page)
        answers = {}
        for i, (name, values) in enumerate(questions):
            correct = answer_keys[qid][i] if i < len(answer_keys[qid]) else 0
            # 마지막 문제만 일부러 틀려 현실적인 기록(예: 4/5)을 남김
            pick = correct if i < len(questions) - 1 else (correct + 1) % len(values)
            answers[name] = values[pick]
        post_form(op, '/quiz/%s/submit' % qid, answers, csrf_from(page))
    return op


def question_choices(page):
    """퀴즈 응시 화면 → [(answer_문항ID, [보기ID...]), ...] (화면 순서 유지)"""
    order, choices = [], {}
    for name, value in re.findall(r'name="(answer_\d+)"[^>]*value="(\d+)"', page):
        if name not in choices:
            order.append(name)
            choices[name] = []
        choices[name].append(value)
    return [(name, choices[name]) for name in order]


def probe_answer_keys(quiz_ids):
    """정답은 응시 화면에 없으므로, 샘플 계정으로 한 번 제출한 뒤 결과 화면에서 정답 위치를 읽습니다."""
    op = session()
    probe = {'username': 'sample_learner', 'nickname': '샘플학습자', 'email': 'sample@example.com',
             'password': secrets.token_urlsafe(12)}
    post_form(op, '/register', probe, csrf_from(get(op, '/register')))
    post_form(op, '/login_process', {'username': probe['username'], 'password': probe['password']},
              csrf_from(get(op, '/login')))
    keys = {}
    for qid in quiz_ids:
        page = get(op, '/quiz/' + qid)
        answers = {name: values[0] for name, values in question_choices(page)}
        result = post_form(op, '/quiz/%s/submit' % qid, answers, csrf_from(page))
        keys[qid] = []
        for card in result.split('class="q-card')[1:]:
            rows = re.findall(r'class="choice-row\s*([^"]*)"', card)
            keys[qid].append(next((i for i, cls in enumerate(rows) if 'correct' in cls.split()), 0))
    return keys


def target_for(path, area, here):
    """사이트 경로 → 목업 파일 경로. area: '' 비로그인 / 'me/' 로그인."""
    page, _, frag = path.partition('#')
    page = page.split('?')[0] or '/'
    for prefix in ('/css/', '/js/', '/images/'):
        if page.startswith(prefix):
            return page[1:]
    if area == '' and page in ('/login', '/register'):
        out = 'me/' + here                       # 로그인·회원가입 → 데모 계정으로 같은 화면
    elif area == 'me/' and page == '/logout':
        out = here                               # 로그아웃 → 비로그인 같은 화면
    elif page in PAGES:
        out = area + PAGES[page]
    elif area == 'me/' and page in ME_ONLY:
        out = 'me/' + ME_ONLY[page]
    else:
        out = 'notice.html'
    return out + ('#' + frag if frag else '')


def convert(html, area, here):
    depth = (area + here).count('/')
    prefix = '../' * depth

    def repl(m):
        attr, quote, url = m.group(1), m.group(2), m.group(3)
        if url.startswith('//'):
            return m.group(0)
        return '%s=%s%s%s' % (attr, quote, prefix + target_for(url, area, here), quote)

    html = re.sub(r'\b(href|src|action)=(["\'])(/[^"\']*)\2', repl, html)
    # 정적 호스팅은 POST를 받지 못하므로 로그아웃 폼은 GET 이동으로
    html = re.sub(r'(<form[^>]*action="[^"]*"[^>]*?)method="post"', r'\1method="get"', html, flags=re.I)
    html = re.sub(r'<input type="hidden" name="_csrf"[^>]*>', '', html)
    html = html.replace('</head>', '<link rel="stylesheet" href="%sdemo.css">\n</head>' % prefix, 1)
    # 새로고침 시 404가 나지 않도록 주소 변경 제거
    html = html.replace("history.replaceState(null, '', '/learn/' + lang);", '')
    if area == 'me/':
        # 강의 완료 저장: 서버 대신 브라우저에서 바로 완료 처리
        html = re.sub(r"const res = await fetch\(`/api/learn/\$\{currentLang\}/complete`, \{.*?\}\);\s*if \(res\.ok\) \{",
                      'const res = { ok: true };\n            if (res.ok) {', html, count=1, flags=re.S)
    return html


def export(opener, pages, area):
    for path, here in pages.items():
        html = convert(get(opener, path), area, here)
        dest = os.path.join(OUT, area, here)
        os.makedirs(os.path.dirname(dest), exist_ok=True)
        with open(dest, 'w', encoding='utf-8') as f:
            f.write(html)
        print('page', area + here)


def main():
    if os.path.isdir(OUT):
        shutil.rmtree(OUT)
    os.makedirs(OUT)

    export(session(), PAGES, '')
    export(prepare_demo_account(), dict(PAGES, **ME_ONLY), 'me/')

    for folder in ('css', 'js', 'images'):
        shutil.copytree(os.path.join(STATIC, folder), os.path.join(OUT, folder))
    with open(os.path.join(OUT, 'demo.css'), 'w', encoding='utf-8') as f:
        f.write('/* 목업 전용: 서버가 필요한 AI 도우미는 숨김 */\n'
                '#ai-buddy, .ai-buddy, .ai-buddy-panel { display: none !important; }\n')
    with open(os.path.join(OUT, 'notice.html'), 'w', encoding='utf-8') as f:
        f.write(NOTICE)
    print('done', OUT)


NOTICE = '''<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>목업 안내 - CodingEdu</title>
<style>
  body { margin: 0; min-height: 100vh; display: grid; place-items: center; padding: 16px;
         background: #f7f8fc; color: #0f172a; font-family: Inter, system-ui, sans-serif; }
  .box { max-width: 420px; padding: 32px 28px; text-align: center; background: #fff;
         border: 1px solid #e4e8f0; border-radius: 16px; box-shadow: 0 2px 8px rgba(28,39,68,.05); }
  .box b { color: #4f55e7; }
  h1 { margin: 0 0 10px; font-size: 1.3rem; }
  p { margin: 0 0 22px; color: #475569; line-height: 1.7; }
  .actions { display: flex; gap: 8px; justify-content: center; flex-wrap: wrap; }
  a, button { min-height: 44px; padding: 10px 16px; border-radius: 8px; font: inherit; font-weight: 700;
              font-size: .9rem; text-decoration: none; cursor: pointer; }
  a { background: #4f55e7; color: #fff; border: 1px solid #4f55e7; }
  button { background: #fff; color: #0f172a; border: 1px solid #e4e8f0; }
</style>
</head>
<body>
  <div class="box">
    <h1><b>Coding</b>Edu</h1>
    <p>이 기능은 서버가 필요해서 목업에서는 동작하지 않아요.<br>홈·강의·퀴즈 화면과 학습 지도를 둘러보세요.</p>
    <div class="actions">
      <button type="button" onclick="history.back()">이전 화면</button>
      <a href="index.html">홈으로</a>
    </div>
  </div>
</body>
</html>
'''

if __name__ == '__main__':
    main()
