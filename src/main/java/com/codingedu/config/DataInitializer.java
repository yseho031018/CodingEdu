package com.codingedu.config;

import com.codingedu.entity.Challenge;
import com.codingedu.entity.Choice;
import com.codingedu.entity.LessonCourse;
import com.codingedu.entity.Question;
import com.codingedu.entity.Quiz;
import com.codingedu.entity.User;
import com.codingedu.repository.ChallengeRepository;
import com.codingedu.repository.LessonCourseRepository;
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
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(QuizRepository quizRepository,
                           LessonCourseRepository lessonCourseRepository,
                           ChallengeRepository challengeRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.quizRepository = quizRepository;
        this.lessonCourseRepository = lessonCourseRepository;
        this.challengeRepository = challengeRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedAdminUser();
        seedLessonCourses();
        seedChallenges();
        if (quizRepository.count() > 0) return; // 이미 데이터가 있으면 건너뜀

        quizRepository.saveAll(List.of(
            buildQuiz("JavaScript", "⚡", "JavaScript 기초 문법",
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
                }),

            buildQuiz("Java", "☕", "Java 객체지향 개념",
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
                }),

            buildQuiz("SQL", "🗄️", "SQL 쿼리 작성",
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
                }),

            buildQuiz("HTML/CSS", "🎨", "HTML & CSS 기초",
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
                }),

            buildQuiz("Python", "🐍", "Python 자료구조와 함수",
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
                }),

            buildQuiz("Spring Boot", "🌱", "Spring Boot 심화",
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
                })
        ));
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
        System.out.println("[DataInitializer] 관리자 계정 생성 완료 — ID: admin / PW: admin1234!");
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
