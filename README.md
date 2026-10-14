# CodingEdu

CodingEdu는 강의 시청, 학습 진도 관리, 퀴즈, 챌린지, 커뮤니티를 하나로 묶은 코딩 학습 웹사이트입니다.  
Spring Boot와 Thymeleaf로 서버 렌더링하며, 회원별 학습 기록과 활동 데이터를 MySQL 호환 데이터베이스에 저장합니다.

## 주요 기능

### 학습

- HTML, CSS, JavaScript 강의 목록과 상세 레슨 제공
- YouTube 영상 재생 및 레슨별 완료 처리
- 언어별 학습 진도율 표시
- 개인 학습 노트 저장
- 관심 강의 즐겨찾기

강의 본문과 영상 정보는 `src/main/resources/data/lesson-courses.json`에 정의하며, 강의 목록 메타데이터는 `lesson_courses` 테이블과 함께 사용합니다.

### 퀴즈

- 주제와 난이도별 퀴즈 제공
- 제한 시간 기반 문제 풀이
- 점수, 정답, 해설 및 오답 기록 확인
- 마이페이지에서 퀴즈 이력 조회

### 챌린지

- 진행 예정, 진행 중, 종료 챌린지 조회
- 챌린지 참여 및 참여 취소
- 참여한 챌린지 완료 처리

### 커뮤니티

- 게시글 작성, 조회, 수정, 삭제
- 댓글 작성 및 삭제
- 좋아요와 반응 기능
- 게시글 검색 및 카테고리 분류
- 사용자 활동 알림

### 회원 및 관리

- 회원가입, 로그인, 로그아웃, 로그인 유지
- 비밀번호 찾기 및 재설정
- 닉네임, 이메일, 비밀번호 변경과 회원 탈퇴
- 개인 마이페이지와 공개 프로필
- 관리자 페이지에서 사용자 권한, 퀴즈, 문제, 챌린지 관리

## 기술 스택

| 영역 | 기술 |
|---|---|
| Backend | Java 21, Spring Boot 3.4.1 |
| Web | Spring MVC, Thymeleaf |
| Security | Spring Security, BCrypt, CSRF Cookie |
| Database | Spring Data JPA, MySQL / TiDB |
| Frontend | HTML, CSS, JavaScript, Tailwind CSS 3 |
| Test | JUnit 5, Spring Boot Test, MockMvc, H2 |
| Build | Maven, npm |

## 프로젝트 구조

```text
src/
├─ main/
│  ├─ java/com/codingedu/
│  │  ├─ config/       # 보안 설정과 초기 데이터
│  │  ├─ controller/   # 페이지 및 API 요청 처리
│  │  ├─ entity/       # JPA 엔티티
│  │  ├─ repository/   # 데이터 접근 계층
│  │  ├─ security/     # 사용자 인증 연동
│  │  └─ service/      # 도메인 비즈니스 로직
│  └─ resources/
│     ├─ data/         # 강의 콘텐츠 JSON
│     ├─ static/       # CSS와 JavaScript
│     └─ templates/    # Thymeleaf 화면
└─ test/               # 컨트롤러, 서비스, 보안 통합 테스트
```

데이터베이스 테이블에 대한 상세 설명은 [docs/DATABASE.md](docs/DATABASE.md)를 참고하세요.

## 발표 자료

최종 발표 자료는 `회의록계획서/발표자료` 폴더에 정리했습니다.

| 파일 | 설명 |
|---|---|
| [CodingEdu_presentation.html](회의록계획서/발표자료/CodingEdu_presentation.html) | 프로젝트 개요, 기술 스택, 실제 화면 기반 기능 설명 흐름으로 구성한 HTML 발표자료 |
| [CodingEdu_발표대본.md](회의록계획서/발표자료/CodingEdu_발표대본.md) | 슬라이드별 발표 멘트와 실제 시연 순서 정리 |

발표는 `프로젝트 개요와 설명 → 사용한 기술 스택 정리 → 실제 화면을 보여주면서 기능 설명` 순서로 진행합니다.  
실제 시연은 홈, 강의, 퀴즈, 커뮤니티, 마이페이지/관리자 화면 순서로 이동하면 서비스의 학습 흐름을 자연스럽게 설명할 수 있습니다.

## 로컬 실행

### 요구 사항

- JDK 21
- Maven 3.9 이상
- Node.js와 npm
- MySQL 또는 TiDB

### 데이터베이스 설정

`src/main/resources/application-local.properties.example`을 참고하여 Git에 포함되지 않는
`src/main/resources/application-local.properties`를 생성합니다.
기본 실행 시 이 설정을 불러오려면 `src/main/resources/application.properties`에
`spring.profiles.default=local`을 추가합니다. 두 파일 모두 Git에서 제외됩니다.

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/codingedu
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

환경 변수로 설정할 수도 있습니다.

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/codingedu"
$env:DB_USERNAME="YOUR_USERNAME"
$env:DB_PASSWORD="YOUR_PASSWORD"
```

### 애플리케이션 실행

```powershell
mvn spring-boot:run
```

실행 후 `http://localhost:8080`에서 접속할 수 있습니다.

애플리케이션 시작 시 `DataInitializer`가 기본 강의 목록, 퀴즈, 챌린지, 예시 게시글과 개발용 관리자 계정을 생성합니다. 운영 배포 전에는 기본 관리자 계정 생성 로직과 자격 증명을 반드시 변경하거나 제거해야 합니다.

## 프론트엔드 스타일 빌드

의존성을 설치한 후 Tailwind CSS를 빌드합니다.

```powershell
npm install
npm run tw:build
```

개발 중 변경사항을 계속 반영하려면 다음 명령을 사용합니다.

```powershell
npm run tw:watch
```

## 테스트

전체 테스트:

```powershell
mvn test
```

특정 테스트만 실행:

```powershell
mvn -Dtest=LearnControllerTest test
```

테스트 환경은 MySQL 호환 모드의 인메모리 H2 데이터베이스를 사용합니다.

## 주요 경로

| 경로 | 설명 |
|---|---|
| `/` | 홈 |
| `/learn` | 강의 목록 |
| `/learn/{lang}` | 언어별 강의 상세 |
| `/quiz` | 퀴즈 목록 |
| `/challenge` | 챌린지 목록 |
| `/community` | 커뮤니티 |
| `/mypage` | 개인 학습 및 활동 기록 |
| `/settings` | 계정 설정 |
| `/admin` | 관리자 페이지 |

## 배포

운영 환경에서는 `prod` 프로필과 DB 환경 변수를 사용합니다.

```powershell
mvn clean package
java -jar target/codingedu-1.0.0.jar --spring.profiles.active=prod
```

운영 프로필은 스키마 자동 변경 대신 `spring.jpa.hibernate.ddl-auto=validate`를 사용하므로, 배포 전에 데이터베이스 스키마가 엔티티와 일치하는지 확인해야 합니다.

## 보안 주의사항

- `application.properties`, `application-local.properties`, `.env`에는 민감 정보를 저장할 수 있으므로 Git에 커밋하지 않습니다.
- 운영 환경의 DB 정보는 환경 변수 또는 비밀 저장소로 주입합니다.
- 기본 관리자 계정과 remember-me 키는 운영 배포 전에 반드시 변경합니다.
- POST 요청과 AJAX 요청은 CSRF 토큰을 포함해야 합니다.
