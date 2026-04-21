package com.codingedu.config;

import com.codingedu.entity.Challenge;
import com.codingedu.entity.Choice;
import com.codingedu.entity.LessonCourse;
import com.codingedu.entity.Post;
import com.codingedu.entity.Question;
import com.codingedu.entity.Quiz;
import com.codingedu.entity.User;
import com.codingedu.repository.ChallengeRepository;
import com.codingedu.repository.LessonCourseRepository;
import com.codingedu.repository.PostRepository;
import com.codingedu.repository.QuizRepository;
import com.codingedu.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final QuizRepository quizRepository;
    private final LessonCourseRepository lessonCourseRepository;
    private final ChallengeRepository challengeRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(QuizRepository quizRepository,
                           LessonCourseRepository lessonCourseRepository,
                           ChallengeRepository challengeRepository,
                           UserRepository userRepository,
                           PostRepository postRepository,
                           PasswordEncoder passwordEncoder) {
        this.quizRepository = quizRepository;
        this.lessonCourseRepository = lessonCourseRepository;
        this.challengeRepository = challengeRepository;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedAdminUser();
        seedLessonCourses();
        seedChallenges();
        seedQuizzes();
        seedPosts();
    }

    // ── 퀴즈 시드 (제목 기준 upsert) ────────────────────────────────
    private void seedQuizzes() {
        seedQuiz("JavaScript", "⚡", "JavaScript 기초 문법",
            "변수 선언, 조건문(if-else), 반복문(for, while) 등 기초 문법을 확실히 익혔는지 테스트합니다.",
            "easy", 15,
            new Q[]{
                q("JavaScript에서 변수를 선언하는 키워드가 아닌 것은?",
                    "var를 제외하면 let과 const는 ES6에서 도입된 키워드입니다. int는 Java/C 계열의 타입 선언 키워드입니다.",
                    c("var", false), c("let", false), c("const", false), c("int", true)),
                q("다음 중 JavaScript의 원시 타입(Primitive Type)이 아닌 것은?",
                    "JavaScript의 원시 타입은 string, number, boolean, null, undefined, symbol, bigint입니다. object는 참조 타입입니다.",
                    c("string", false), c("number", false), c("object", true), c("boolean", false)),
                q("`console.log(typeof null)`의 출력 결과는?",
                    "typeof null이 \"object\"를 반환하는 것은 JavaScript 초기 설계의 버그로, 하위 호환성 때문에 수정되지 않고 있습니다.",
                    c("\"null\"", false), c("\"undefined\"", false), c("\"object\"", true), c("\"number\"", false)),
                q("배열의 마지막 요소를 제거하고 반환하는 메서드는?",
                    "pop()은 배열의 마지막 요소를 제거하고 반환합니다. shift()는 첫 번째 요소를 제거합니다.",
                    c("shift()", false), c("pop()", true), c("splice()", false), c("slice()", false)),
                q("`===` 연산자와 `==` 연산자의 차이는?",
                    "===는 값과 타입을 모두 비교하는 일치 연산자입니다. ==는 타입 변환 후 값만 비교합니다.",
                    c("둘 다 동일하다", false), c("===는 값과 타입을 모두 비교한다", true), c("==가 더 엄격하다", false), c("===는 값만 비교한다", false))
            });

        seedQuiz("Java", "☕", "Java 객체지향 개념",
            "클래스, 상속, 다형성, 캡슐화 등 자바의 핵심인 객체지향(OOP) 개념을 테스트합니다.",
            "medium", 20,
            new Q[]{
                q("Java에서 상속을 나타내는 키워드는?",
                    "Java에서 클래스 상속은 extends 키워드를 사용합니다. implements는 인터페이스 구현에 사용합니다.",
                    c("implements", false), c("extends", true), c("inherits", false), c("super", false)),
                q("메서드 오버로딩(Overloading)의 조건은?",
                    "오버로딩은 같은 클래스 내에서 메서드 이름은 같고 매개변수의 타입, 개수, 순서가 달라야 합니다.",
                    c("메서드 이름이 다를 것", false), c("리턴 타입이 다를 것", false), c("매개변수(타입/개수)가 다를 것", true), c("접근 제어자가 다를 것", false)),
                q("인터페이스를 구현(implementation)할 때 사용하는 키워드는?",
                    "인터페이스를 구현할 때는 implements 키워드를 사용합니다. extends는 클래스 상속에 사용합니다.",
                    c("extends", false), c("implements", true), c("inherits", false), c("override", false)),
                q("추상 클래스(abstract class)에 대한 설명으로 옳은 것은?",
                    "추상 클래스는 인스턴스를 직접 생성할 수 없습니다. 추상 메서드를 포함할 수 있지만 모두 추상일 필요는 없습니다.",
                    c("직접 인스턴스화할 수 없다", true), c("모든 메서드가 추상이어야 한다", false), c("다중 상속이 가능하다", false), c("생성자를 가질 수 없다", false)),
                q("Java에서 접근 제어자의 범위가 가장 넓은 것은?",
                    "public은 모든 곳에서 접근 가능합니다. 범위: public > protected > (default) > private",
                    c("private", false), c("protected", false), c("default", false), c("public", true))
            });

        seedQuiz("SQL", "🗄️", "SQL 쿼리 작성",
            "SELECT, JOIN, GROUP BY, HAVING 등 실무에서 자주 쓰이는 데이터베이스 쿼리 작성을 테스트합니다.",
            "medium", 25,
            new Q[]{
                q("두 테이블에서 조건이 일치하는 행만 반환하는 JOIN은?",
                    "INNER JOIN은 두 테이블 모두에 일치하는 행만 반환합니다. LEFT JOIN은 왼쪽 테이블의 모든 행을 반환합니다.",
                    c("LEFT JOIN", false), c("RIGHT JOIN", false), c("INNER JOIN", true), c("FULL OUTER JOIN", false)),
                q("GROUP BY와 함께 집계 함수의 결과에 조건을 적용하는 절은?",
                    "HAVING은 GROUP BY로 그룹화된 결과에 조건을 적용합니다. WHERE는 그룹화 전에 적용됩니다.",
                    c("WHERE", false), c("HAVING", true), c("FILTER", false), c("CONDITION", false)),
                q("중복 행을 제거하고 고유한 값만 반환하는 키워드는?",
                    "DISTINCT 키워드를 사용하면 SELECT 결과에서 중복된 행을 제거할 수 있습니다.",
                    c("UNIQUE", false), c("DISTINCT", true), c("DIFFERENT", false), c("ONLY", false)),
                q("SELECT 결과를 내림차순으로 정렬하는 구문은?",
                    "ORDER BY 컬럼명 DESC는 내림차순(큰 값 → 작은 값) 정렬입니다. 기본값(ASC)은 오름차순입니다.",
                    c("ORDER BY col ASC", false), c("ORDER BY col DESC", true), c("SORT BY col DESC", false), c("ARRANGE BY col DESC", false)),
                q("NULL 값을 가진 행을 찾는 올바른 SQL 구문은?",
                    "NULL 값 비교는 = NULL이 아니라 IS NULL을 사용해야 합니다. = NULL은 항상 false를 반환합니다.",
                    c("WHERE col = NULL", false), c("WHERE col IS NULL", true), c("WHERE col == NULL", false), c("WHERE NULL(col)", false))
            });

        seedQuiz("HTML/CSS", "🎨", "HTML & CSS 기초",
            "시맨틱 태그, 선택자, 박스 모델(Margin/Padding), Flexbox 등 웹의 화면을 구성하는 기초를 테스트합니다.",
            "easy", 15,
            new Q[]{
                q("HTML에서 하이퍼링크를 만드는 태그는?",
                    "<a> 태그는 href 속성으로 연결할 URL을 지정합니다. Anchor의 약자입니다.",
                    c("<link>", false), c("<a>", true), c("<href>", false), c("<url>", false)),
                q("CSS Flexbox에서 주축(main axis) 방향으로 아이템을 정렬하는 속성은?",
                    "justify-content는 주축(flex-direction이 row면 수평) 방향 정렬입니다. align-items는 교차축 방향입니다.",
                    c("align-items", false), c("justify-content", true), c("flex-direction", false), c("flex-wrap", false)),
                q("HTML5에서 도입된 시맨틱(Semantic) 태그가 아닌 것은?",
                    "<div>는 HTML4부터 있던 비시맨틱 태그입니다. <header>, <section>, <article>은 HTML5에서 추가된 시맨틱 태그입니다.",
                    c("<header>", false), c("<section>", false), c("<div>", true), c("<article>", false)),
                q("CSS Box Model에서 border 바깥쪽의 여백을 지정하는 속성은?",
                    "margin은 요소 바깥 여백, padding은 요소 안쪽 여백입니다. Box Model 순서: content → padding → border → margin",
                    c("padding", false), c("margin", true), c("border-spacing", false), c("outline", false)),
                q("CSS에서 클래스 선택자를 나타내는 기호는?",
                    "클래스 선택자는 . (점), ID 선택자는 # (해시)로 시작합니다.",
                    c("#", false), c(".", true), c("@", false), c("*", false))
            });

        seedQuiz("Python", "🐍", "Python 자료구조와 함수",
            "리스트, 튜플, 딕셔너리와 같은 자료구조의 활용법 및 기본적인 함수 작성법을 테스트합니다.",
            "easy", 15,
            new Q[]{
                q("Python에서 변경이 불가능한(immutable) 자료구조는?",
                    "tuple은 한 번 생성하면 수정할 수 없습니다. list, dict, set은 변경 가능(mutable)합니다.",
                    c("list", false), c("dict", false), c("tuple", true), c("set", false)),
                q("딕셔너리에서 특정 키의 값을 안전하게 가져오는 메서드는?",
                    "get()은 키가 없을 때 기본값(None)을 반환합니다. d[key]는 키가 없으면 KeyError를 발생시킵니다.",
                    c("find()", false), c("get()", true), c("fetch()", false), c("retrieve()", false)),
                q("Python 함수에서 가변 위치 인자(임의 개수)를 받을 때 사용하는 것은?",
                    "*args는 위치 인자를 튜플로 받습니다. **kwargs는 키워드 인자를 딕셔너리로 받습니다.",
                    c("&args", false), c("*args", true), c("**kwargs (키워드 인자)", false), c("@args", false)),
                q("[0, 1, 4, 9, 16]을 리스트 컴프리헨션으로 생성하는 올바른 코드는?",
                    "리스트 컴프리헨션: [표현식 for 변수 in 이터러블]. x**2는 x의 제곱입니다.",
                    c("[x^2 for x in range(5)]", false), c("[x**2 for x in range(5)]", true), c("[pow(x) for x in range(5)]", false), c("[x*x for x in range(1, 6)]", false)),
                q("Python에서 None을 확인하는 올바른 방법은?",
                    "None은 is 연산자로 비교해야 합니다. ==는 __eq__ 메서드를 호출하지만, is는 객체 동일성(identity)을 비교합니다.",
                    c("if x == None:", false), c("if x is None:", true), c("if x === None:", false), c("if None(x):", false))
            });

        seedQuiz("Spring Boot", "🌱", "Spring Boot 심화",
            "의존성 주입(DI), AOP(관점 지향 프로그래밍), Spring Security 구조 등 심화 개념을 테스트합니다.",
            "hard", 30,
            new Q[]{
                q("Spring에서 빈(Bean)의 기본 스코프(scope)는?",
                    "Spring 빈의 기본 스코프는 singleton으로, 애플리케이션 컨텍스트당 하나의 인스턴스만 생성됩니다.",
                    c("prototype", false), c("singleton", true), c("request", false), c("session", false)),
                q("@Transactional의 기본 격리 수준(Isolation Level)은?",
                    "DEFAULT는 사용하는 DB의 기본 격리 수준을 따릅니다. MySQL InnoDB의 기본값은 REPEATABLE READ입니다.",
                    c("READ_UNCOMMITTED", false), c("READ_COMMITTED", false), c("DEFAULT (DB 기본값)", true), c("SERIALIZABLE", false)),
                q("@RestController 어노테이션은 어떤 두 어노테이션의 조합인가?",
                    "@RestController = @Controller + @ResponseBody입니다. @ResponseBody가 있으면 반환값이 뷰가 아닌 HTTP 응답 본문으로 직렬화됩니다.",
                    c("@Controller + @RequestMapping", false), c("@Controller + @ResponseBody", true), c("@Service + @ResponseBody", false), c("@Component + @ResponseBody", false)),
                q("Spring Security에서 인증(Authentication)과 인가(Authorization)의 올바른 설명은?",
                    "인증(Authentication)은 '당신이 누구인가?'를 확인하는 과정, 인가(Authorization)는 '당신이 무엇을 할 수 있는가?'를 확인하는 과정입니다.",
                    c("인증=권한확인, 인가=신원확인", false), c("인증=신원확인, 인가=권한확인", true), c("둘 다 동일한 개념이다", false), c("인증=로그아웃, 인가=로그인", false)),
                q("JPA에서 지연 로딩(Lazy Loading)을 설정하는 올바른 방법은?",
                    "fetch = FetchType.LAZY로 설정하면 연관 엔티티를 실제로 사용할 때까지 DB 조회를 미룹니다.",
                    c("@FetchType.EAGER", false), c("fetch = FetchType.LAZY", true), c("@Lazy", false), c("@LazyLoad", false))
            });

        // ── 신규 퀴즈 ──────────────────────────────────────────────────

        seedQuiz("TypeScript", "💙", "TypeScript 타입 시스템",
            "정적 타입, 인터페이스, 제네릭 등 TypeScript만의 핵심 기능을 이해하는지 테스트합니다.",
            "medium", 20,
            new Q[]{
                q("TypeScript에서 `any` 타입을 지양해야 하는 주된 이유는?",
                    "any를 사용하면 타입 검사를 완전히 우회하여 컴파일 타임 오류 감지 능력을 잃습니다. TypeScript를 사용하는 핵심 이점이 사라집니다.",
                    c("런타임 성능이 느려진다", false), c("타입 검사를 우회해 타입 안전성을 잃는다", true), c("컴파일 시간이 길어진다", false), c("메모리를 더 사용한다", false)),
                q("옵셔널 프로퍼티(Optional Property)를 정의하는 올바른 TypeScript 구문은?",
                    "? 기호를 타입 뒤에 붙이면 해당 프로퍼티가 있어도 되고 없어도 된다는 의미입니다.",
                    c("name: string | null", false), c("name?: string", true), c("name: optional string", false), c("name: string?", false)),
                q("TypeScript의 `interface`와 `type alias`에 대한 설명으로 옳은 것은?",
                    "interface는 같은 이름으로 여러 번 선언하면 자동으로 합쳐지는 선언 병합(Declaration Merging)이 가능합니다. type alias는 불가능합니다.",
                    c("type만 유니온 타입을 정의할 수 있다", false), c("interface만 클래스에 implements로 사용할 수 있다", false), c("interface는 선언 병합(Declaration Merging)이 가능하다", true), c("둘은 완전히 동일하다", false)),
                q("제네릭(Generic)을 사용하는 주된 목적은?",
                    "제네릭은 타입을 변수처럼 다루어 코드를 재사용하면서도 타입 안전성을 유지할 수 있게 합니다.",
                    c("코드 실행 속도를 높이기 위해", false), c("타입 안전성을 유지하면서 재사용 가능한 코드를 작성하기 위해", true), c("컴파일 에러를 무시하기 위해", false), c("변수 이름을 단순화하기 위해", false)),
                q("`readonly` 키워드의 역할로 올바른 것은?",
                    "readonly로 선언된 프로퍼티는 초기화 이후 값을 변경할 수 없습니다. 의도치 않은 변경을 방지합니다.",
                    c("변수를 전역으로 선언한다", false), c("메서드를 오버라이드할 수 없게 한다", false), c("프로퍼티를 초기화 이후 변경 불가능하게 만든다", true), c("함수의 반환값을 고정한다", false))
            });

        seedQuiz("React", "⚛️", "React 핵심 개념",
            "컴포넌트, Hook(useState/useEffect), Virtual DOM 등 React의 핵심 개념을 테스트합니다.",
            "medium", 20,
            new Q[]{
                q("React에서 컴포넌트의 상태(state)를 관리하는 Hook은?",
                    "useState는 [현재값, 업데이트 함수] 쌍을 반환합니다. 상태가 변경되면 컴포넌트가 리렌더링됩니다.",
                    c("useContext", false), c("useState", true), c("useRef", false), c("useMemo", false)),
                q("컴포넌트의 마운트, 업데이트, 언마운트 시 부수 효과(side effect)를 처리하는 Hook은?",
                    "useEffect는 두 번째 인자로 의존성 배열을 받습니다. 빈 배열([])을 전달하면 마운트 시에만 실행됩니다.",
                    c("useState", false), c("useCallback", false), c("useEffect", true), c("useReducer", false)),
                q("리스트 렌더링 시 `key` prop이 필요한 이유는?",
                    "key는 각 요소를 고유하게 식별하여 React가 어떤 항목이 변경/추가/제거됐는지 파악하고 최소한의 DOM 업데이트만 수행하게 합니다.",
                    c("CSS 스타일을 적용하기 위해", false), c("각 요소를 고유 식별하여 효율적인 DOM 업데이트를 위해", true), c("서버로 데이터를 전송하기 위해", false), c("컴포넌트의 순서를 정렬하기 위해", false)),
                q("`props`와 `state`의 차이로 옳은 것은?",
                    "props는 부모 컴포넌트에서 자식으로 전달되며 읽기 전용입니다. state는 컴포넌트 내부에서 관리하며 변경 가능합니다.",
                    c("props는 컴포넌트 내부에서 변경할 수 있다", false), c("state는 부모 컴포넌트가 전달한다", false), c("props는 부모로부터 전달받고 state는 내부에서 관리한다", true), c("둘 다 동일한 역할을 한다", false)),
                q("React의 Virtual DOM의 주요 역할은?",
                    "Virtual DOM은 실제 DOM의 가벼운 사본으로, 변경 전후를 비교(diffing)하여 실제로 변경된 부분만 실제 DOM에 업데이트합니다.",
                    c("실제 DOM을 완전히 대체한다", false), c("실제 DOM과 차이를 계산해 최소한의 업데이트만 수행한다", true), c("서버 사이드 렌더링을 처리한다", false), c("CSS 애니메이션을 최적화한다", false))
            });

        seedQuiz("Git", "🔀", "Git 버전 관리 기초",
            "commit, branch, merge, push/pull 등 협업에 필수인 Git 기본 명령어를 테스트합니다.",
            "easy", 15,
            new Q[]{
                q("변경된 파일을 스테이징 영역(Staging Area)에 추가하는 명령어는?",
                    "git add는 변경 사항을 다음 커밋에 포함시키기 위해 스테이징 영역에 등록합니다. git add . 은 모든 변경 파일을 추가합니다.",
                    c("git commit", false), c("git push", false), c("git add", true), c("git stage", false)),
                q("스테이징된 변경사항을 메시지와 함께 저장하는 명령어는?",
                    "git commit -m \"메시지\"는 스테이징된 변경사항을 로컬 저장소에 영구 기록합니다.",
                    c("git push -m \"메시지\"", false), c("git commit -m \"메시지\"", true), c("git save -m \"메시지\"", false), c("git add -m \"메시지\"", false)),
                q("원격 저장소의 변경사항을 로컬로 가져와 현재 브랜치에 병합하는 명령어는?",
                    "git pull = git fetch + git merge입니다. 원격의 최신 내용을 가져와 로컬에 자동으로 합칩니다.",
                    c("git fetch", false), c("git clone", false), c("git pull", true), c("git download", false)),
                q("새 브랜치를 생성하고 즉시 해당 브랜치로 전환하는 명령어는?",
                    "-b 옵션은 브랜치를 새로 만들면서 동시에 체크아웃합니다. git branch + git checkout 두 명령의 단축형입니다.",
                    c("git branch new-branch", false), c("git checkout -b new-branch", true), c("git switch new-branch", false), c("git create new-branch", false)),
                q("다른 브랜치의 커밋 이력을 현재 브랜치에 통합하는 명령어는?",
                    "git merge [브랜치명]은 지정한 브랜치의 변경사항을 현재 브랜치로 합칩니다. 충돌(conflict)이 발생하면 수동으로 해결해야 합니다.",
                    c("git rebase", false), c("git merge", true), c("git fetch", false), c("git push", false))
            });

        seedQuiz("알고리즘", "🧮", "알고리즘과 자료구조",
            "시간 복잡도, 정렬 알고리즘, 스택/큐, 이진 탐색, 동적 프로그래밍 등 핵심 CS 개념을 테스트합니다.",
            "hard", 30,
            new Q[]{
                q("평균 시간 복잡도가 O(n²)인 정렬 알고리즘은?",
                    "버블 정렬은 인접한 두 원소를 반복적으로 비교·교환하여 O(n²)의 시간이 소요됩니다. 병합/퀵/힙 정렬은 평균 O(n log n)입니다.",
                    c("병합 정렬(Merge Sort)", false), c("퀵 정렬(Quick Sort)", false), c("힙 정렬(Heap Sort)", false), c("버블 정렬(Bubble Sort)", true)),
                q("스택(Stack)의 데이터 접근 방식으로 올바른 것은?",
                    "스택은 LIFO(Last In, First Out) 구조입니다. 가장 나중에 삽입한 데이터가 가장 먼저 제거됩니다. 함수 호출 스택, 뒤로 가기 기능에 활용됩니다.",
                    c("선입선출 (FIFO)", false), c("후입선출 (LIFO)", true), c("우선순위 기반 접근", false), c("랜덤 접근", false)),
                q("이진 탐색(Binary Search)이 동작하기 위한 전제 조건은?",
                    "이진 탐색은 중간값과 비교해 범위를 절반씩 줄이는 방식으로, 반드시 정렬된 배열에서만 사용 가능합니다. 시간 복잡도 O(log n).",
                    c("데이터에 중복이 없어야 한다", false), c("데이터가 정렬되어 있어야 한다", true), c("데이터 크기가 2의 거듭제곱이어야 한다", false), c("데이터가 정수여야 한다", false)),
                q("동적 프로그래밍(Dynamic Programming)의 핵심 개념은?",
                    "DP는 큰 문제를 작은 부분 문제로 쪼개고, 이미 계산한 결과를 메모이제이션으로 저장하여 중복 계산을 제거합니다.",
                    c("항상 재귀를 사용하지 않는 것", false), c("이미 계산한 값을 저장해 중복 계산을 피하는 것", true), c("항상 탐욕적으로 최적 해를 선택하는 것", false), c("문제를 무조건 두 개로 분할하는 것", false)),
                q("해시 테이블(Hash Table)에서 충돌(Collision)이 발생하는 원인은?",
                    "서로 다른 키가 해시 함수를 통해 같은 인덱스(버킷)에 매핑될 때 충돌이 발생합니다. 체이닝이나 개방 주소법으로 해결합니다.",
                    c("키(Key)의 길이가 너무 길어서", false), c("해시 함수가 다른 키에 동일한 인덱스를 반환해서", true), c("메모리가 부족해서", false), c("테이블 크기가 짝수여서", false))
            });
    }

    private void seedQuiz(String topic, String icon, String title, String description,
                          String difficulty, int timeLimit, Q[] qs) {
        if (quizRepository.existsByTitle(title)) return;
        quizRepository.save(buildQuiz(topic, icon, title, description, difficulty, timeLimit, qs));
    }

    // ── 관리자 계정 자동 생성 ────────────────────────────────────────
    private void seedAdminUser() {
        if (userRepository.findByUsername("admin").isPresent()) return;
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin1234!"));
        admin.setNickname("관리자");
        admin.setEmail("admin@codingedu.com");
        admin.setRole("ROLE_ADMIN");
        userRepository.save(admin);
        System.out.println("[DataInitializer] 관리자 계정 생성 완료 (admin)");
    }

    // ── 강의 코스 시드 (upsert) ──────────────────────────────────────
    private void seedLessonCourses() {
        upsertLc("html",       "WEB1 - HTML",          "🌐", "web",    "beginner",     9, "웹 페이지의 뼈대를 만드는 마크업 언어");
        upsertLc("css",        "WEB2 - CSS",            "🎨", "web",    "beginner",     6, "웹 페이지를 아름답게 꾸미는 스타일 언어");
        upsertLc("javascript", "WEB3 - JavaScript",     "⚡", "web",    "beginner",     6, "웹 페이지에 동작을 추가하는 프로그래밍 언어");
        upsertLc("typescript", "TypeScript",            "💙", "web",    "intermediate", 5, "타입 안전성을 갖춘 JavaScript 상위 집합");
        upsertLc("java",       "Java 기초",             "☕", "java",   "beginner",     5, "객체지향 프로그래밍의 대표 언어");
        upsertLc("kotlin",     "Kotlin",                "🟣", "java",   "intermediate", 4, "Android 공식 개발 언어");
        upsertLc("c",          "C언어 기초",            "💻", "c",      "beginner",     5, "프로그래밍의 기본을 다지는 시스템 언어");
        upsertLc("cpp",        "C++ 기초",              "⚙️", "c",      "intermediate", 4, "고성능 애플리케이션 개발 언어");
        upsertLc("swift",      "Swift",                 "🦅", "mobile", "intermediate", 4, "iOS/macOS 앱 개발 언어");
        upsertLc("python",     "Python",                "🐍", "etc",    "beginner",     5, "데이터 과학, AI, 웹 개발에 사용되는 언어");
    }

    private void upsertLc(String lang, String title, String icon,
                           String category, String level, int lessonCount, String desc) {
        LessonCourse c = lessonCourseRepository.findByLang(lang).orElse(new LessonCourse());
        c.setLang(lang);
        c.setTitle(title);
        c.setIcon(icon);
        c.setCategory(category);
        c.setLevel(level);
        c.setLessonCount(lessonCount);
        c.setDescription(desc);
        lessonCourseRepository.save(c);
    }

    // ── 챌린지 시드 ──────────────────────────────────────────────────
    private void seedChallenges() {
        if (challengeRepository.count() > 0) return;
        LocalDate today = LocalDate.now();
        challengeRepository.saveAll(List.of(
            ch("30일 알고리즘 챌린지", "🚀",
                "하루에 한 문제씩 꾸준히 풀며 코딩 테스트 실력을 확실하게 키워보세요! 스터디 그룹원들과 코드 리뷰도 진행합니다.",
                "active", true,
                today.minusDays(15), today.plusDays(15), 30, 508,
                "🔥 절반이나 왔어요! 지금 시작해도 충분합니다"),
            ch("웹사이트 클론 코딩", "💻",
                "유명한 웹사이트(넷플릭스, 에어비앤비 등)의 껍데기를 진짜 똑같이 만들어보며 프론트엔드 실전 경험을 쌓습니다!",
                "active", false,
                today.minusDays(9), today.plusDays(21), 3, 297,
                "🎨 화면이 점점 멋져지네요! 지금 참여하세요"),
            ch("백엔드 REST API 설계", "🎯",
                "회원가입, 로그인, 게시판 기능이 있는 서버의 RESTful API를 직접 설계하고, Postman으로 테스트해 봅니다.",
                "active", false,
                today.minusDays(5), today.plusDays(25), 12, 174,
                "⚙️ 서버가 돌아가기 시작했어요! 아직 늦지 않았어요"),
            ch("Python 데이터 분석 시각화", "📊",
                "Pandas와 Matplotlib을 사용해서 넷플릭스 영화 데이터를 직접 분석하고 예쁜 차트로 시각화해 봅니다.",
                "upcoming", false,
                today.plusDays(28), today.plusDays(42), 14, 0,
                null),
            ch("Flutter 나만의 할 일 앱 만들기", "📱",
                "iOS와 Android 양쪽에서 모두 동작하는 하이브리드 모바일 앱을 Flutter로 2주 안에 완성합니다.",
                "upcoming", false,
                today.plusDays(44), today.plusDays(58), 14, 0,
                null)
        ));
    }

    private Challenge ch(String title, String icon, String description,
                          String status, boolean featured,
                          LocalDate startDate, LocalDate endDate,
                          int totalTasks, int participantCount, String progressMessage) {
        Challenge c = new Challenge();
        c.setTitle(title);
        c.setIcon(icon);
        c.setDescription(description);
        c.setStatus(status);
        c.setFeatured(featured);
        c.setStartDate(startDate);
        c.setEndDate(endDate);
        c.setTotalTasks(totalTasks);
        c.setParticipantCount(participantCount);
        c.setProgressMessage(progressMessage);
        return c;
    }

    // ── 게시글 시드 ────────────────────────────────────────────────
    private void seedPosts() {
        User admin = userRepository.findByUsername("admin").orElse(null);
        if (admin == null) return;

        // 더미 작성자 계정 생성 (없으면)
        User u1 = seedDummyUser("에러해결사", "error_king", "error_king@example.com");
        User u2 = seedDummyUser("백엔드장인", "backend_pro", "backend_pro@example.com");
        User u3 = seedDummyUser("프론트깎는노인", "frontend_old", "frontend_old@example.com");
        User u4 = seedDummyUser("Java초보자", "java_newbie", "java_newbie@example.com");
        User u5 = seedDummyUser("스터디장", "study_leader", "study_leader@example.com");

        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        // qna 카테고리
        post(u4, "qna", "NullPointerException이 자꾸 발생하는데 원인을 모르겠어요",
            "Spring Boot로 간단한 CRUD를 만들고 있는데 서비스 레이어에서 NullPointerException이 계속 발생합니다.\n\n" +
            "```java\n@Service\npublic class UserService {\n    @Autowired\n    private UserRepository userRepository;\n\n    public User findUser(Long id) {\n        return userRepository.findById(id).get(); // 여기서 터짐\n    }\n}\n```\n\n" +
            "findById()에서 값이 없으면 NoSuchElementException이 발생한다는 건 알겠는데, 왜 NPE가 같이 뜨는지 모르겠습니다. 혹시 아시는 분 계신가요?",
            52, 3, now.minusDays(5));

        post(u1, "qna", "Spring Security 로그인 후 리다이렉트가 이상하게 동작해요",
            "로그인 성공 후 원래 접근하려던 페이지로 가야 하는데 무조건 홈으로만 가는 문제가 있습니다.\n\n" +
            "SecurityConfig에서 defaultSuccessUrl을 설정해봤는데도 똑같이 홈으로 이동하네요.\n\n" +
            "혹시 SavedRequestAwareAuthenticationSuccessHandler를 써야 하나요? 사용법을 아시는 분 계시면 도움 부탁드립니다 🙏",
            38, 7, now.minusDays(3));

        post(u4, "qna", "JPA에서 LazyInitializationException 해결 방법이 뭔가요?",
            "연관 엔티티를 LAZY로 설정했더니 트랜잭션 밖에서 접근할 때 LazyInitializationException이 발생합니다.\n\n" +
            "```\norg.hibernate.LazyInitializationException: could not initialize proxy - no Session\n```\n\n" +
            "EAGER로 바꾸면 되긴 하는데, 성능 문제가 생길 것 같아서요. fetch join이나 @Transactional을 써야 하나요? N+1 문제도 같이 해결하고 싶어요.",
            91, 15, now.minusDays(1));

        post(admin, "qna", "React에서 useState 업데이트가 즉시 반영 안 되는 이유가 뭔가요?",
            "버튼 클릭 시 setState를 호출한 직후에 콘솔로 값을 찍어보면 여전히 이전 값이 나옵니다.\n\n" +
            "```javascript\nconst [count, setCount] = useState(0);\n\nconst handleClick = () => {\n    setCount(count + 1);\n    console.log(count); // 업데이트 전 값이 찍힘\n};\n```\n\n" +
            "비동기로 동작하는 건 알겠는데, 업데이트된 값을 즉시 사용하려면 어떻게 해야 하나요?",
            67, 11, now.minusHours(8));

        // tips 카테고리
        post(u2, "tips", "IntelliJ 생산성 단축키 모음 - 알면 개발 속도 2배!",
            "자주 쓰는 IntelliJ 단축키를 정리했습니다. 처음엔 외우기 힘들지만 한번 익히면 정말 편해요!\n\n" +
            "**코드 작성**\n- `Ctrl+Space`: 코드 자동완성\n- `Alt+Enter`: 빠른 수정 제안\n- `Ctrl+P`: 파라미터 정보\n- `Ctrl+B`: 선언부로 이동\n\n" +
            "**리팩토링**\n- `Shift+F6`: 이름 변경\n- `Ctrl+Alt+M`: 메서드 추출\n- `Ctrl+Alt+V`: 변수 추출\n\n" +
            "**탐색**\n- `Ctrl+Shift+F`: 전체 파일 검색\n- `Ctrl+E`: 최근 파일\n- `Ctrl+G`: 라인 이동\n\n" +
            "특히 Alt+Enter는 무조건 외우세요. 에러 해결의 신입니다 😄",
            203, 45, now.minusDays(7));

        post(u1, "tips", "Git 실수 복구 명령어 총정리 - 겁 없이 커밋하세요",
            "개발하다 보면 Git에서 실수를 많이 하죠. 자주 쓰는 복구 명령어 정리했습니다.\n\n" +
            "**커밋 취소**\n```bash\n# 마지막 커밋 취소 (변경사항 유지)\ngit reset --soft HEAD~1\n\n# 마지막 커밋 취소 (변경사항도 삭제)\ngit reset --hard HEAD~1\n```\n\n" +
            "**실수로 삭제한 파일 복구**\n```bash\ngit checkout HEAD -- 파일명\n```\n\n" +
            "**푸시한 커밋 되돌리기**\n```bash\n# 되돌리는 새 커밋 생성 (협업 시 안전)\ngit revert HEAD\n```\n\n" +
            "**stash 활용**\n```bash\n# 현재 작업 임시 저장\ngit stash\n\n# 저장한 작업 불러오기\ngit stash pop\n```\n\n" +
            "force push는 웬만하면 쓰지 마세요. 팀원들이 싫어합니다 😅",
            178, 38, now.minusDays(4));

        post(u3, "tips", "CSS Flexbox 완전 정복 - 레이아웃 잡기 이제 쉽습니다",
            "Flexbox는 처음엔 헷갈리지만 제대로 이해하면 레이아웃이 정말 쉬워져요!\n\n" +
            "**기본 설정**\n```css\n.container {\n    display: flex;\n    flex-direction: row; /* 가로 정렬 (기본값) */\n}\n```\n\n" +
            "**자주 쓰는 패턴**\n```css\n/* 완전 중앙 정렬 */\n.center {\n    display: flex;\n    justify-content: center; /* 가로축 */\n    align-items: center;    /* 세로축 */\n}\n\n/* 양 끝 정렬 */\n.space-between {\n    display: flex;\n    justify-content: space-between;\n}\n```\n\n" +
            "**flex-grow vs flex-shrink**\n- `flex-grow`: 남은 공간을 얼마나 차지할지\n- `flex-shrink`: 공간 부족 시 얼마나 줄어들지\n- `flex: 1`이면 `flex-grow: 1; flex-shrink: 1; flex-basis: 0`\n\n" +
            "Grid와 함께 쓰면 대부분의 레이아웃을 처리할 수 있어요! 🎨",
            156, 29, now.minusDays(2));

        post(u2, "tips", "Spring Boot 성능 최적화 - 실무에서 바로 써먹는 팁",
            "Spring Boot 프로젝트에서 성능을 개선한 경험을 공유합니다.\n\n" +
            "**1. N+1 문제 해결**\nJPA에서 가장 흔한 성능 이슈입니다.\n```java\n// 나쁜 예\nList<User> users = userRepository.findAll();\nusers.forEach(u -> System.out.println(u.getPosts().size())); // N번 쿼리\n\n// 좋은 예 - fetch join 사용\n@Query(\"SELECT u FROM User u LEFT JOIN FETCH u.posts\")\nList<User> findAllWithPosts();\n```\n\n" +
            "**2. 캐싱 적용**\n```java\n@Cacheable(\"users\")\npublic User findById(Long id) { ... }\n```\n\n" +
            "**3. 페이지네이션**\nfindAll() 대신 Page 객체를 써서 대용량 데이터를 처리하세요.\n\n" +
            "이 세 가지만 챙겨도 응답 속도가 눈에 띄게 빨라집니다!",
            142, 31, now.minusHours(3));

        // study 카테고리
        post(u5, "study", "Spring Boot + React 풀스택 스터디 모집 (주 2회, 온라인)",
            "안녕하세요! Spring Boot와 React를 함께 공부할 스터디원을 모집합니다 🙌\n\n" +
            "**스터디 목표**\n- Spring Boot로 REST API 설계 및 구현\n- React로 프론트엔드 개발\n- 포트폴리오용 풀스택 프로젝트 완성\n\n" +
            "**모집 대상**\n- Java/Spring 기초 지식 있으신 분\n- 주 2회 (화, 목 저녁 8시) 참여 가능하신 분\n- 적극적으로 코드 리뷰 하고 싶으신 분\n\n" +
            "**모집 인원**: 4~5명\n**기간**: 3개월\n**방식**: Discord + 화상회의\n\n" +
            "관심 있으신 분은 댓글 남겨주세요! 함께 성장해요 💪",
            89, 19, now.minusDays(6));

        post(admin, "study", "알고리즘 스터디 모집 - 코딩테스트 같이 준비해요",
            "취업/이직을 위해 코딩테스트를 준비하는 스터디입니다!\n\n" +
            "**커리큘럼**\n1주차: 시간복잡도, 정렬\n2주차: 탐색(BFS/DFS)\n3주차: 동적 프로그래밍\n4주차: 그리디, 백트래킹\n...\n\n" +
            "**플랫폼**: 백준 + 프로그래머스\n**언어**: Java / Python 모두 가능\n**주 1회** (토요일 오후 2시)\n**기간**: 12주\n\n" +
            "매주 3문제씩 풀고 풀이를 공유하는 방식입니다. 혼자 풀면 막막한데 같이 하면 확실히 달라요! 현재 2명 확정이고 3명 더 모집합니다.",
            73, 14, now.minusDays(2));

        post(u3, "study", "CSS/디자인 감각 키우기 스터디 - 프론트개발자 필수!",
            "백엔드 개발자도 기본 UI는 만들 줄 알아야 한다고 생각해서 스터디를 만들었어요!\n\n" +
            "**다룰 내용**\n- Figma로 와이어프레임 만들기\n- CSS 그리드 & 플렉스박스 마스터\n- 반응형 웹 디자인\n- Tailwind CSS 실전 활용\n- 애니메이션 & 트랜지션\n\n" +
            "매주 하나의 UI 컴포넌트를 직접 만들어보는 방식입니다. 노션에 결과물 아카이빙해서 포폴에도 써먹을 수 있어요!\n\n" +
            "주 1회 비동기 스터디라 시간 부담 적습니다. 관심 있으신 분 DM 주세요 😊",
            61, 8, now.minusDays(1));

        // free 카테고리
        post(u4, "free", "개발 공부 6개월째... 슬럼프 극복한 방법 공유",
            "안녕하세요. 비전공자로 개발 공부를 시작한지 6개월 됐습니다.\n\n" +
            "처음 3개월은 정말 재밌었는데 4개월차부터 슬럼프가 왔어요. '내가 과연 개발자가 될 수 있을까?' 라는 생각이 매일 들었고, 코드 한 줄 짜기도 싫어지더라고요.\n\n" +
            "그때 제가 한 것들:\n1. **작은 프로젝트 완성하기** - 투두리스트, 날씨앱 등 작아도 '완성'의 경험이 중요했어요\n2. **커뮤니티 참여** - 혼자 공부하면 지치는데 여기처럼 커뮤니티에서 글 보는 것만으로도 힘이 됐습니다\n3. **기대치 낮추기** - 처음부터 완벽한 코드 쓰려다 지친 것 같아요. 일단 돌아가면 장땡!\n\n" +
            "지금은 다시 재밌어졌어요. 슬럼프 오신 분들 화이팅입니다! 💪",
            134, 42, now.minusDays(8));

        post(u5, "free", "개발자 면접 후기 - 신입 기술면접에서 자주 나오는 질문",
            "최근 스타트업 3군데 면접을 보고 왔습니다. 공부하시는 분들께 도움 되시길!\n\n" +
            "**백엔드 (Java/Spring)**\n- JVM 메모리 구조 설명해보세요\n- 스프링 빈 생명주기는?\n- @Transactional 동작 원리\n- 인덱스는 언제 쓰나요?\n- REST와 RESTful의 차이\n\n" +
            "**CS 기초**\n- 프로세스 vs 스레드\n- HTTP vs HTTPS\n- TCP 3-way handshake\n- 데이터베이스 정규화\n\n" +
            "면접관분들이 답을 외웠냐보다 '왜 그런지 이해하고 있냐'를 더 중요하게 보시더라고요. 두루뭉술하게 알기보다 하나를 제대로 설명할 수 있는 게 나은 것 같았습니다!\n\n" +
            "다들 좋은 결과 있으시길 바라요 🙏",
            287, 76, now.minusDays(3));

        post(u3, "free", "비전공자가 1년 만에 첫 취업한 회고",
            "안녕하세요! 문과 출신으로 독학해서 작년에 첫 개발자 취업에 성공했습니다. 공부하시는 분들께 도움이 될까 해서 회고를 써봤어요.\n\n" +
            "**타임라인**\n- 1~3개월: Java 기초, 자료구조\n- 4~6개월: Spring Boot 공부, 미니 프로젝트\n- 7~9개월: 포트폴리오 프로젝트 (팀 프로젝트 1개, 개인 프로젝트 1개)\n- 10~12개월: 취업 준비, 코테, 면접\n\n" +
            "**가장 중요했던 것**\n결국 '내가 직접 만든 것'이 있어야 한다는 거예요. 강의 100개 듣는 것보다 프로젝트 1개 제대로 완성하는 게 훨씬 값집니다.\n\n" +
            "궁금하신 거 댓글로 달아주시면 아는 범위에서 답해드릴게요!",
            412, 93, now.minusDays(10));

        post(u2, "free", "추천 개발 유튜브 채널 & 블로그 모음",
            "공부하면서 도움 많이 받은 자료들을 공유합니다!\n\n" +
            "**유튜브 (한국)**\n- 생활코딩: 웹 기초, 입문자에게 강추\n- 코딩애플: 트렌디한 설명, 재미있음\n- 우아한테크: 실무 관련 고품질 강연\n\n" +
            "**유튜브 (영어)**\n- Fireship: 짧고 임팩트 있는 기술 설명\n- Traversy Media: 웹개발 전반\n- Amigoscode: Spring Boot 강의 퀄리티 최고\n\n" +
            "**블로그**\n- 우아한형제들 기술블로그\n- 카카오 기술블로그\n- 네이버 D2\n\n" +
            "영어 자료가 한국어보다 훨씬 많고 최신이에요. 영어 공부도 함께 하시면 정말 좋습니다!\n\n" +
            "다른 좋은 자료 아시는 분들은 댓글로 공유해주세요 🙌",
            198, 55, now.minusDays(5));
    }

    private User seedDummyUser(String nickname, String username, String email) {
        return userRepository.findByUsername(username).orElseGet(() -> {
            User u = new User();
            u.setUsername(username);
            u.setNickname(nickname);
            u.setEmail(email);
            u.setPassword(passwordEncoder.encode("dummy1234!"));
            u.setRole("ROLE_USER");
            return userRepository.save(u);
        });
    }

    private void post(User author, String category, String title, String content,
                      int views, int likeCount,
                      java.time.LocalDateTime createdAt) {
        if (postRepository.existsByTitle(title)) return;
        Post p = new Post();
        p.setAuthor(author);
        p.setCategory(category);
        p.setTitle(title);
        p.setContent(content);
        p.setViews(views);
        p.setLikeCount(likeCount);
        p.setCreatedAt(createdAt);
        postRepository.save(p);
    }

    // ── 헬퍼 레코드/메서드 ──────────────────────────────────────────
    private record Q(String text, String explanation, Choice[] choices) {}
    private record C(String text, boolean correct) {}

    private Q q(String text, String explanation, C... choices) {
        Choice[] choiceEntities = new Choice[choices.length];
        for (int i = 0; i < choices.length; i++) {
            Choice c = new Choice();
            c.setChoiceText(choices[i].text());
            c.setCorrect(choices[i].correct());
            choiceEntities[i] = c;
        }
        return new Q(text, explanation, choiceEntities);
    }

    private C c(String text, boolean correct) {
        return new C(text, correct);
    }

    private Quiz buildQuiz(String topic, String icon, String title, String description,
                           String difficulty, int timeLimit, Q[] qs) {
        Quiz quiz = new Quiz();
        quiz.setTopic(topic);
        quiz.setIcon(icon);
        quiz.setTitle(title);
        quiz.setDescription(description);
        quiz.setDifficulty(difficulty);
        quiz.setTimeLimit(timeLimit);

        for (int i = 0; i < qs.length; i++) {
            Question question = new Question();
            question.setQuestionText(qs[i].text());
            question.setExplanation(qs[i].explanation());
            question.setOrderNum(i + 1);
            question.setQuiz(quiz);
            for (Choice choice : qs[i].choices()) {
                choice.setQuestion(question);
                question.getChoices().add(choice);
            }
            quiz.getQuestions().add(question);
        }
        return quiz;
    }
}
