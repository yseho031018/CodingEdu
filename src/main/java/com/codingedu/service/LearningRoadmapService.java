package com.codingedu.service;

import com.codingedu.entity.LessonCourse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 홈 화면 학습 로드맵(분야 선택 지도 → 분야별 순서형 경로)에 쓰는 뷰 모델을 만듭니다.
 * 분야와 단계 구성은 여기서 정의하고, 강의명·레슨 수·영상 길이·진도는 실제 데이터에서 가져옵니다.
 */
@Service
public class LearningRoadmapService {

    public static final String DEFAULT_TRACK = "web";

    /** 강의 없이 경로 끝에 표시하는 준비 중 단계 */
    private static final String UPCOMING_STEP = "project";

    private record StepDef(String lang, String name, String summary) {}

    private record TrackDef(String key, String name, String summary, String audience, String level,
                            String goal, String firstPrerequisite, List<StepDef> steps) {}

    private static final List<TrackDef> TRACKS = List.of(
            new TrackDef("web", "웹 개발 입문", "HTML·CSS·JavaScript로 웹의 기초 다지기",
                    "웹사이트 제작을 처음 배우는 학습자", "입문",
                    "나만의 웹 페이지 만들기", "선수 강의 없음",
                    List.of(new StepDef("html", "HTML & Internet", "웹 페이지의 기본 구조와 인터넷 동작 배우기"),
                            new StepDef("css", "CSS", "웹 페이지를 원하는 모습으로 꾸미기"),
                            new StepDef("javascript", "JavaScript", "웹 페이지에 동작과 상호작용 더하기"),
                            new StepDef(UPCOMING_STEP, "미니 프로젝트", "배운 내용으로 나만의 웹 페이지 완성하기"))),
            new TrackDef("frontend", "프론트엔드", "React와 Next.js로 서비스 화면 만들기",
                    "HTML·CSS 기초를 아는 학습자", "초급",
                    "React·Next.js로 서비스 화면 만들기", "권장 선행: HTML·CSS 기초",
                    List.of(new StepDef("javascript", "JavaScript", "React를 위한 JavaScript 기본기 다지기"),
                            new StepDef("react", "React", "컴포넌트로 화면을 나누고 CRUD 흐름 익히기"),
                            new StepDef("nextjs", "Next.js", "라우팅과 서버 렌더링으로 앱 완성하기"))),
            new TrackDef("backend", "백엔드", "서버에서 동작하는 웹 애플리케이션 만들기",
                    "서버와 데이터 처리가 궁금한 학습자", "입문",
                    "서버가 있는 웹 애플리케이션 만들기", "선수 강의 없음",
                    List.of(new StepDef("git", "Git", "버전 관리로 코드를 안전하게 기록하기"),
                            new StepDef("php", "PHP", "폼 처리와 서버 측 웹 애플리케이션 흐름 익히기"),
                            new StepDef("java", "Java 입문", "Java 문법과 객체지향 기초 다지기"))),
            new TrackDef("database", "데이터베이스", "SQL로 데이터를 저장하고 조회하기",
                    "데이터 관리가 처음인 학습자", "입문",
                    "관계형 데이터 설계하고 조회하기", "선수 강의 없음",
                    List.of(new StepDef("mysql", "MySQL", "관계형 데이터베이스와 SQL 기본 문법 익히기"),
                            new StepDef("sqljoin", "SQL JOIN", "테이블을 나누고 JOIN으로 연결하기"))),
            new TrackDef("python", "Python 입문", "가장 쉬운 언어로 프로그래밍 시작하기",
                    "프로그래밍을 처음 배우는 학습자", "입문",
                    "Python으로 첫 프로그램 만들기", "선수 강의 없음",
                    List.of(new StepDef("python", "Python 기초", "변수·조건문·반복문으로 프로그래밍 기초 익히기"),
                            new StepDef("git", "Git", "만든 코드를 버전 관리로 기록하기")))
    );

    public record RoadmapStep(int number, String lang, String name, String summary, String level,
                              int lessonCount, String duration, String prerequisite,
                              String status, int progress, boolean available) {
        public boolean isDone() { return "done".equals(status); }
        public boolean isCurrent() { return "current".equals(status); }
    }

    public record RoadmapTrack(String key, String name, String summary, String audience, String level,
                               String goal, int courseCount, int lessonCount, int progress,
                               List<RoadmapStep> steps) {}

    private final LessonService lessonService;
    private final LessonContentService lessonContentService;

    public LearningRoadmapService(LessonService lessonService, LessonContentService lessonContentService) {
        this.lessonService = lessonService;
        this.lessonContentService = lessonContentService;
    }

    /**
     * @param progressByLang 로그인 사용자의 언어별 진도율(0~100). 비로그인이면 null.
     */
    public List<RoadmapTrack> buildTracks(Map<String, Integer> progressByLang) {
        Map<String, LessonCourse> courses = lessonService.getAllCourses().stream()
                .collect(Collectors.toMap(LessonCourse::getLang, Function.identity()));
        Map<String, Integer> minutes = lessonContentService.getCourseDurationMinutes();

        List<RoadmapTrack> tracks = new ArrayList<>();
        for (TrackDef def : TRACKS) {
            List<RoadmapStep> steps = new ArrayList<>();
            boolean currentAssigned = false;
            boolean nextAssigned = false;
            String previousName = null;
            int lessonTotal = 0;
            int completedWeight = 0;
            for (StepDef stepDef : def.steps()) {
                LessonCourse course = courses.get(stepDef.lang());
                boolean available = course != null;
                int lessons = available ? course.getLessonCount() : 0;
                int pct = available && progressByLang != null ? progressByLang.getOrDefault(stepDef.lang(), 0) : 0;

                String status;
                if (!available) {
                    status = "upcoming";
                } else if (progressByLang != null && pct >= 100) {
                    status = "done";
                } else if (!currentAssigned) {
                    status = "current";
                    currentAssigned = true;
                } else if (!nextAssigned) {
                    status = "next";
                    nextAssigned = true;
                } else {
                    status = "later";
                }

                steps.add(new RoadmapStep(steps.size() + 1, stepDef.lang(), stepDef.name(), stepDef.summary(),
                        available ? levelLabel(course.getLevel()) : "실습",
                        lessons, formatDuration(minutes.get(stepDef.lang())),
                        previousName == null ? def.firstPrerequisite() : "선수 강의: " + previousName,
                        status, pct, available));
                previousName = stepDef.name();
                lessonTotal += lessons;
                completedWeight += lessons * pct;
            }
            int courseCount = (int) steps.stream().filter(RoadmapStep::available).count();
            int trackProgress = lessonTotal > 0 ? Math.round(completedWeight / (float) lessonTotal) : 0;
            tracks.add(new RoadmapTrack(def.key(), def.name(), def.summary(), def.audience(), def.level(),
                    def.goal(), courseCount, lessonTotal, trackProgress, steps));
        }
        return tracks;
    }

    /** 강의 목록·추천 카드에서 WEB1 같은 원래 강의명 대신 보여줄 학습 안내 (예: "웹 개발 입문 · 1단계 / HTML & Internet"). */
    public record CourseGuide(String lang, String trackName, int stepNumber, String name, String summary,
                              String level, String duration, int lessonCount, String prerequisite) {
        public String stepLabel() { return trackName + " · " + stepNumber + "단계"; }
    }

    /** 강의마다 처음 등장하는 분야 기준의 안내를 반환합니다. (JavaScript·Git처럼 여러 분야에 속해도 하나만) */
    public Map<String, CourseGuide> courseGuides() {
        Map<String, CourseGuide> guides = new java.util.LinkedHashMap<>();
        for (RoadmapTrack track : buildTracks(null)) {
            for (RoadmapStep step : track.steps()) {
                if (step.available() && !guides.containsKey(step.lang())) {
                    guides.put(step.lang(), new CourseGuide(step.lang(), track.name(), step.number(), step.name(),
                            step.summary(), step.level(), step.duration(), step.lessonCount(), step.prerequisite()));
                }
            }
        }
        return guides;
    }

    /** 가장 많이 진행했지만 아직 끝내지 않은 강의 — 홈의 "이어서 학습" 대상. 없으면 null. */
    public String continueLang(Map<String, Integer> progressByLang) {
        if (progressByLang == null) {
            return null;
        }
        return progressByLang.entrySet().stream()
                .filter(e -> e.getValue() > 0 && e.getValue() < 100)
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /** 진행 중인 분야가 있으면 그 분야를, 없으면 웹 개발 입문을 먼저 펼칩니다. */
    public String initialTrack(List<RoadmapTrack> tracks) {
        return tracks.stream()
                .filter(t -> t.progress() > 0 && t.progress() < 100)
                .max((a, b) -> Integer.compare(a.progress(), b.progress()))
                .map(RoadmapTrack::key)
                .orElse(DEFAULT_TRACK);
    }

    private static String levelLabel(String level) {
        return switch (level == null ? "" : level) {
            case "intermediate" -> "중급";
            case "advanced" -> "고급";
            default -> "입문";
        };
    }

    private static String formatDuration(Integer totalMinutes) {
        if (totalMinutes == null || totalMinutes <= 0) {
            return null;
        }
        if (totalMinutes < 60) {
            return "약 " + totalMinutes + "분";
        }
        int rounded = Math.round(totalMinutes / 10f) * 10;
        int hours = rounded / 60;
        int mins = rounded % 60;
        return "약 " + hours + "시간" + (mins > 0 ? " " + mins + "분" : "");
    }
}
