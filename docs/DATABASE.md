# 데이터베이스 테이블 구조

CodingEdu 프로젝트의 DB 테이블별 역할을 도메인 단위로 정리한 문서입니다.
(`src/main/java/com/codingedu/entity/*.java` JPA 엔티티 기준)

---

## 👤 사용자

### `users`
회원 정보. 로그인/권한과 프로필을 담당.

| 컬럼 | 타입/제약 | 설명 |
|---|---|---|
| `id` | PK, auto | 사용자 식별자 |
| `username` | NOT NULL, UNIQUE, len 50 | 로그인 아이디 (영문/숫자/_) |
| `password` | NOT NULL | bcrypt 해시된 비밀번호 |
| `nickname` | NOT NULL, UNIQUE, len 50 | 표시용 닉네임 |
| `email` | NOT NULL, len 100 | 이메일 |
| `role` | NOT NULL | `ROLE_USER` / `ROLE_ADMIN` |
| `created_at` | - | 가입 시각 |

---

## 📚 학습(강의)

### `lesson_courses`
강의 카탈로그. 언어/카테고리별 코스 메타데이터.

| 컬럼 | 타입/제약 | 설명 |
|---|---|---|
| `id` | PK | |
| `lang` | NOT NULL, UNIQUE, len 30 | 언어 식별자 (html, css, java, javascript…) |
| `title` | NOT NULL, len 100 | 코스 제목 |
| `icon` | NOT NULL, len 10 | 아이콘(이모지) |
| `category` | NOT NULL, len 20 | web / java / c / mobile 등 |
| `level` | NOT NULL, len 20 | beginner / intermediate / advanced |
| `lesson_count` | NOT NULL | 총 레슨 개수 |
| `description` | TEXT | 소개글 |

### `learning_progress`
사용자의 레슨별 완료 기록.

| 컬럼 | 타입/제약 | 설명 |
|---|---|---|
| `id` | PK | |
| `user_id` | FK → users, NOT NULL | 완료한 사용자 |
| `lang` | NOT NULL, len 30 | 언어 코스 |
| `lesson_idx` | NOT NULL | 해당 언어의 몇 번째 레슨인지 |
| `completed_at` | - | 완료 시각 |
| — | UNIQUE(`user_id`, `lang`, `lesson_idx`) | 같은 레슨 중복 완료 방지 |

### `lesson_notes`
레슨별 개인 학습 노트. 1인 1레슨 1노트.

| 컬럼 | 타입/제약 | 설명 |
|---|---|---|
| `id` | PK | |
| `user_id` | FK → users, NOT NULL | 작성자 |
| `lang`, `lesson_idx` | NOT NULL | 어떤 레슨에 쓴 노트인지 |
| `content` | TEXT, NOT NULL | 노트 본문 |
| `updated_at` | - | 마지막 수정 시각 |
| — | UNIQUE(`user_id`, `lang`, `lesson_idx`) | 같은 레슨에 노트 하나만 |

---

## 🧠 퀴즈

### `quizzes`
퀴즈 세트(시험지) 본체.

| 컬럼 | 타입/제약 | 설명 |
|---|---|---|
| `id` | PK | |
| `topic` | NOT NULL, len 50 | "JavaScript", "Java" 등 |
| `icon` | NOT NULL, len 10 | 아이콘 |
| `title` | NOT NULL, len 200 | 퀴즈 제목 |
| `description` | TEXT, NOT NULL | 소개글 |
| `difficulty` | NOT NULL, len 10 | easy / medium / hard |
| `time_limit` | NOT NULL | 제한시간(분) |
| `created_at` | - | 생성 시각 |

### `questions`
퀴즈에 속한 개별 문항.

| 컬럼 | 타입/제약 | 설명 |
|---|---|---|
| `id` | PK | |
| `quiz_id` | FK → quizzes, NOT NULL | 소속 퀴즈 |
| `question_text` | TEXT, NOT NULL | 문제 본문 |
| `explanation` | TEXT | 정답 해설 (결과 페이지 표시용) |
| `order_num` | NOT NULL | 문항 순서 |

### `choices`
객관식 보기. 정답 여부 플래그 포함.

| 컬럼 | 타입/제약 | 설명 |
|---|---|---|
| `id` | PK | |
| `question_id` | FK → questions, NOT NULL | 소속 문항 |
| `choice_text` | NOT NULL, len 500 | 보기 텍스트 |
| `correct` | NOT NULL | 정답 여부 |

### `quiz_results`
사용자의 퀴즈 응시 결과 헤더(총점/만점).

| 컬럼 | 타입/제약 | 설명 |
|---|---|---|
| `id` | PK | |
| `user_id` | FK → users, NOT NULL | 응시자 |
| `quiz_id` | FK → quizzes, NOT NULL | 응시한 퀴즈 |
| `score` | NOT NULL | 맞은 개수 |
| `total_questions` | NOT NULL | 전체 문항 수 |
| `created_at` | - | 응시 시각 |

### `quiz_result_details`
각 문항별 응시 상세 (어떤 보기를 골랐고 정/오답인지).

| 컬럼 | 타입/제약 | 설명 |
|---|---|---|
| `id` | PK | |
| `result_id` | FK → quiz_results, NOT NULL | 소속 응시 기록 |
| `question_id` | FK → questions, NOT NULL | 대상 문항 |
| `selected_choice_id` | FK → choices, NULL 허용 | 선택한 보기 (미응답 시 null) |
| `correct` | NOT NULL | 정답 여부 |

---

## 🚀 챌린지

### `challenges`
코딩 챌린지 공고(기간/상태/태스크 수).

| 컬럼 | 타입/제약 | 설명 |
|---|---|---|
| `id` | PK | |
| `title` | NOT NULL | 챌린지 제목 |
| `description` | TEXT, NOT NULL | 설명 |
| `icon` | NOT NULL, len 10 | 아이콘 |
| `status` | NOT NULL, len 20 | `active` / `upcoming` |
| `featured` | - | 추천 여부 |
| `start_date` | NOT NULL | 시작일 |
| `end_date` | - | 종료일 |
| `total_tasks` | NOT NULL | 총 과제 개수 |
| `participant_count` | NOT NULL | 시드용 기본 참가자 수 |
| `progress_message` | - | 진행 상태 메시지 |
| `created_at` | - | 생성 시각 |

### `challenge_participations`
사용자의 챌린지 참여/완료 상태와 GitHub 제출 링크.

| 컬럼 | 타입/제약 | 설명 |
|---|---|---|
| `id` | PK | |
| `user_id` | FK → users, NOT NULL | 참여자 |
| `challenge_id` | FK → challenges, NOT NULL | 대상 챌린지 |
| `joined_at` | - | 참여 시각 |
| `completed_at` | NULL 허용 | 완료 시각 (null이면 진행 중) |
| `github_url` | len 300 | 제출 리포지토리 URL |
| — | UNIQUE(`user_id`, `challenge_id`) | 같은 챌린지 중복 참여 방지 |

---

## 💬 커뮤니티

### `posts`
커뮤니티 게시글.

| 컬럼 | 타입/제약 | 설명 |
|---|---|---|
| `id` | PK | |
| `category` | NOT NULL, len 20 | `qna` / `tips` / `study` / `free` |
| `title` | NOT NULL, len 200 | 제목 |
| `content` | TEXT, NOT NULL | 본문 |
| `views` | NOT NULL | 조회수 (비정규화 카운터) |
| `like_count` | NOT NULL | 좋아요 수 캐시 (`post_likes` count와 동기화) |
| `comment_count` | NOT NULL | 댓글 수 캐시 (현재 쓰기 경로 미연결 — 실제 카운트는 COUNT 쿼리로 조회) |
| `created_at` | - | 작성 시각 |
| `user_id` | FK → users, NOT NULL | 작성자 |

### `comments`
게시글 댓글.

| 컬럼 | 타입/제약 | 설명 |
|---|---|---|
| `id` | PK | |
| `post_id` | FK → posts, NOT NULL | 소속 게시글 |
| `user_id` | FK → users, NOT NULL | 작성자 |
| `content` | TEXT, NOT NULL | 댓글 본문 |
| `created_at` | - | 작성 시각 |

### `post_likes`
게시글 좋아요 로그. `posts.like_count`의 원천 데이터.

| 컬럼 | 타입/제약 | 설명 |
|---|---|---|
| `id` | PK | |
| `user_id` | FK → users, NOT NULL | 좋아요 누른 사용자 |
| `post_id` | FK → posts, NOT NULL | 대상 게시글 |
| `created_at` | - | 누른 시각 |
| — | UNIQUE(`user_id`, `post_id`) | 중복 좋아요 방지 |

---

## 🔔 알림

### `notifications`
알림함. 현재는 댓글 알림 위주.

| 컬럼 | 타입/제약 | 설명 |
|---|---|---|
| `id` | PK | |
| `receiver_id` | FK → users, NOT NULL | 수신자 |
| `type` | NOT NULL, len 50 | 알림 종류 (예: `COMMENT`) |
| `message` | NOT NULL, len 300 | 알림 메시지 |
| `post_id` | - | 관련 게시글 id (선택) |
| `is_read` | NOT NULL, 기본 false | 읽음 여부 |
| `created_at` | - | 생성 시각 |

---

## 관계 요약

### 주요 소유 관계 (users ←1:N→ 활동 테이블)
`users`가 다음 테이블의 소유자입니다.
- `posts`, `comments`, `post_likes`
- `learning_progress`, `lesson_notes`
- `quiz_results`, `challenge_participations`
- `notifications`

### 1:N 계층
- `quizzes` → `questions` → `choices`
- `quiz_results` → `quiz_result_details`
- `posts` → `comments`, `post_likes`

### 조인 테이블 성격 (UNIQUE 제약으로 중복 방지)
- `challenge_participations` — (user, challenge)
- `post_likes` — (user, post)
- `learning_progress` — (user, lang, lesson_idx)
- `lesson_notes` — (user, lang, lesson_idx)

### 비정규화 카운터
`posts` 테이블의 다음 컬럼은 성능용 캐시입니다.
- `views` — 상세 조회 시 증가
- `like_count` — `post_likes` 토글 시 동기화
- `comment_count` — **현재 쓰기 경로에 연결되지 않음**. 실제 댓글 수는 `CommentService`에서 `comments` COUNT 쿼리로 조회 중. 스키마상 `NOT NULL` 제약만 남아 있어, 엔티티에 필드 매핑 필요.
