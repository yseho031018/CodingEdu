package com.codingedu.controller;

import com.codingedu.entity.Choice;
import com.codingedu.entity.Question;
import com.codingedu.entity.Quiz;
import com.codingedu.entity.QuizResult;
import com.codingedu.entity.User;
import com.codingedu.security.CustomUserDetails;
import com.codingedu.service.QuizAiCoachService;
import com.codingedu.service.QuizService;
import com.codingedu.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class QuizController {

    private final QuizService quizService;
    private final UserService userService;
    private final QuizAiCoachService quizAiCoachService;

    public QuizController(QuizService quizService, UserService userService,
                          QuizAiCoachService quizAiCoachService) {
        this.quizService = quizService;
        this.userService = userService;
        this.quizAiCoachService = quizAiCoachService;
    }

    // 1. DB 기반 퀴즈 목록 (메인 퀴즈 페이지)
    @GetMapping("/quiz")
    public String list(@RequestParam(name = "difficulty", defaultValue = "all") String difficulty,
                       @RequestParam(name = "topic", defaultValue = "all") String topic,
                       @RequestParam(name = "sort", defaultValue = "latest") String sort,
                       @AuthenticationPrincipal CustomUserDetails userDetails,
                       Model model) {
        List<Quiz> quizzes = new ArrayList<>(quizService.getQuizzes(difficulty, topic));

        List<QuizResult> userResults = List.of();
        if (userDetails != null) {
            User user = userService.findByUsername(userDetails.getUsername());
            userResults = quizService.getUserResults(user);
        }

        Map<Long, Integer> userBestPercentages = buildUserBestPercentages(userResults);
        Map<Long, Integer> globalAveragePercentages = quizService.getAveragePercentageByQuizId();
        sortQuizzes(quizzes, sort, userBestPercentages, globalAveragePercentages);

        List<QuizCard> quizCards = quizzes.stream()
                .map(quiz -> toQuizCard(quiz, userBestPercentages, globalAveragePercentages))
                .toList();

        model.addAttribute("quizzes", quizzes);
        model.addAttribute("quizCards", quizCards);
        model.addAttribute("quizStats", buildQuizStats(userResults, quizService.countAll()));
        model.addAttribute("recentQuizActivities", buildRecentActivities(userResults));
        model.addAttribute("leaderboard", quizService.getLeaderboard(5));
        model.addAttribute("currentDifficulty", difficulty);
        model.addAttribute("currentTopic", topic);
        model.addAttribute("currentSort", sort);
        model.addAttribute("topics", quizService.getDistinctTopics());
        return "quiz";
    }

    // 빠른 연습 퀴즈 (프론트엔드 자체 문제)
    @GetMapping("/quiz/practice")
    public String practiceQuiz() {
        return "coding-quiz";
    }

    // 2. 퀴즈 풀기 페이지 (로그인 필요)
    @GetMapping("/quiz/{id}")
    public String take(@PathVariable(name = "id") Long id,
                       @AuthenticationPrincipal CustomUserDetails userDetails,
                       HttpServletRequest request,
                       Model model) {
        if (userDetails == null) return "redirect:/login";
        request.getSession().setAttribute("quiz_start_" + id, java.time.LocalDateTime.now());
        model.addAttribute("quiz", quizService.getQuizById(id));
        return "quiz-take";
    }

    // 3. 퀴즈 제출
    @PostMapping("/quiz/{id}/submit")
    public String submit(@PathVariable(name = "id") Long id,
                         HttpServletRequest request,
                         @AuthenticationPrincipal CustomUserDetails userDetails,
                         RedirectAttributes redirectAttributes) {
        if (userDetails == null) return "redirect:/login";

        Map<Long, Long> userAnswers = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (key.startsWith("answer_") && values.length > 0) {
                try {
                    Long questionId = Long.parseLong(key.substring(7));
                    Long choiceId = Long.parseLong(values[0]);
                    userAnswers.put(questionId, choiceId);
                } catch (NumberFormatException ignored) {}
            }
        });

        User user = userService.findByUsername(userDetails.getUsername());
        java.time.LocalDateTime startedAt = (java.time.LocalDateTime)
                request.getSession().getAttribute("quiz_start_" + id);
        request.getSession().removeAttribute("quiz_start_" + id);
        QuizResult result = quizService.submitQuiz(id, userAnswers, user, startedAt);
        redirectAttributes.addFlashAttribute("userAnswers", userAnswers);
        return "redirect:/quiz/" + id + "/result/" + result.getId();
    }

    // 4. 퀴즈 결과 페이지
    @GetMapping("/quiz/{id}/result/{resultId}")
    public String result(@PathVariable(name = "id") Long id,
                         @PathVariable(name = "resultId") Long resultId,
                         @AuthenticationPrincipal CustomUserDetails userDetails,
                         Model model) {
        if (userDetails == null) return "redirect:/login";

        QuizResult quizResult = quizService.getResultById(resultId);
        if (!quizResult.getUser().getUsername().equals(userDetails.getUsername())) {
            return "redirect:/quiz";
        }

        Quiz quiz = quizService.getQuizById(id);

        // DB에 저장된 answer details로 뷰 모델 생성
        var details = quizService.getResultDetails(quizResult);
        List<QuestionView> questionViews = buildQuestionViewsFromDetails(details);

        model.addAttribute("quizResult", quizResult);
        model.addAttribute("quiz", quiz);
        model.addAttribute("questionViews", questionViews);
        model.addAttribute("aiFeedback", quizAiCoachService.analyze(quiz, quizResult, details));
        return "quiz-result";
    }

    // ── 헬퍼: DB details로 채점 뷰 모델 생성 ───────────────────────
    private List<QuestionView> buildQuestionViewsFromDetails(
            List<com.codingedu.entity.QuizResultDetail> details) {
        List<QuestionView> views = new ArrayList<>();
        for (var detail : details) {
            Question question = detail.getQuestion();
            Long selectedId = detail.getSelectedChoice() != null ? detail.getSelectedChoice().getId() : null;
            List<ChoiceView> choiceViews = new ArrayList<>();
            for (Choice choice : question.getChoices()) {
                boolean selected = choice.getId().equals(selectedId);
                String status;
                if (choice.isCorrect()) status = "correct";
                else if (selected) status = "wrong-selected";
                else status = "neutral";
                choiceViews.add(new ChoiceView(choice, status));
            }
            String cardStatus = (selectedId == null) ? "unanswered" : (detail.isCorrect() ? "correct" : "wrong");
            views.add(new QuestionView(question, choiceViews, cardStatus, detail.isCorrect()));
        }
        return views;
    }

    // ── 뷰 모델 레코드 ────────────────────────────────────────────────
    public record QuestionView(Question question, List<ChoiceView> choices, String status, boolean correct) {}
    public record ChoiceView(Choice choice, String status) {}
    public record QuizStats(int totalAttempts, int accuracy, int streakDays, int points,
                            int completedQuizzes, int fullMarkQuizzes, long availableQuizzes,
                            int progressPercent) {}
    public record QuizCard(Quiz quiz, String mark, String theme, String difficultyLabel,
                           String difficultyTone, Integer averageAccuracy, Integer myBestAccuracy,
                           String progressTone) {}
    public record QuizActivity(String title, int percentage, String whenLabel, String tone) {}

    private void sortQuizzes(List<Quiz> quizzes, String sort, Map<Long, Integer> userBestPercentages, Map<Long, Integer> globalAveragePercentages) {
        if ("oldest".equals(sort)) {
            quizzes.sort(Comparator.comparing(Quiz::getCreatedAt,
                    Comparator.nullsLast(Comparator.naturalOrder())));
            return;
        }
        if ("difficulty".equals(sort)) {
            quizzes.sort(Comparator.comparingInt(q -> difficultyOrder(q.getDifficulty())));
            return;
        }
        if ("accuracy".equals(sort)) {
            // 카드에 표시하는 "전체 사용자 평균 정답률" 기준. 응시 기록이 없는 퀴즈는 뒤로 보냅니다.
            quizzes.sort((q1, q2) -> Integer.compare(
                    globalAveragePercentages.getOrDefault(q2.getId(), -1),
                    globalAveragePercentages.getOrDefault(q1.getId(), -1)));
            return;
        }
        quizzes.sort(Comparator.comparing(Quiz::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed());
    }

    private int difficultyOrder(String difficulty) {
        return switch (difficulty) {
            case "easy" -> 0;
            case "medium" -> 1;
            case "hard" -> 2;
            default -> 3;
        };
    }

    private Map<Long, Integer> buildUserBestPercentages(List<QuizResult> results) {
        Map<Long, Integer> best = new HashMap<>();
        for (QuizResult result : results) {
            if (result.getQuiz() == null || result.getQuiz().getId() == null) continue;
            best.merge(result.getQuiz().getId(), result.getPercentage(), Math::max);
        }
        return best;
    }

    private QuizStats buildQuizStats(List<QuizResult> results, long availableQuizzes) {
        int totalAttempts = results.size();
        int correctAnswers = results.stream().mapToInt(QuizResult::getScore).sum();
        int totalQuestions = results.stream().mapToInt(QuizResult::getTotalQuestions).sum();
        int accuracy = totalQuestions > 0 ? correctAnswers * 100 / totalQuestions : 0;
        int completedQuizzes = (int) results.stream()
                .map(r -> r.getQuiz() != null ? r.getQuiz().getId() : null)
                .filter(id -> id != null)
                .distinct()
                .count();
        int fullMarkQuizzes = (int) results.stream()
                .filter(result -> result.getTotalQuestions() > 0 && result.getScore() == result.getTotalQuestions())
                .count();
        int progressPercent = availableQuizzes > 0
                ? (int) Math.min(100, completedQuizzes * 100 / availableQuizzes)
                : 0;
        int points = correctAnswers * 50 + totalAttempts * 25;
        return new QuizStats(totalAttempts, accuracy, calculateStreakDays(results), points,
                completedQuizzes, fullMarkQuizzes, availableQuizzes, progressPercent);
    }

    private int calculateStreakDays(List<QuizResult> results) {
        List<LocalDate> dates = results.stream()
                .map(QuizResult::getCreatedAt)
                .filter(createdAt -> createdAt != null)
                .map(java.time.LocalDateTime::toLocalDate)
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();
        if (dates.isEmpty()) return 0;

        int streak = 0;
        LocalDate cursor = LocalDate.now();
        for (LocalDate date : dates) {
            if (date.equals(cursor)) {
                streak++;
                cursor = cursor.minusDays(1);
            } else if (streak == 0 && date.equals(cursor.minusDays(1))) {
                streak++;
                cursor = date.minusDays(1);
            } else if (date.isBefore(cursor)) {
                break;
            }
        }
        return streak;
    }

    private List<QuizActivity> buildRecentActivities(List<QuizResult> results) {
        return results.stream()
                .limit(3)
                .map(result -> new QuizActivity(
                        result.getQuiz() != null ? result.getQuiz().getTitle() : "퀴즈",
                        result.getPercentage(),
                        formatWhen(result.getCreatedAt()),
                        result.getPercentage() >= 80 ? "good" : "review"))
                .toList();
    }

    private String formatWhen(java.time.LocalDateTime createdAt) {
        if (createdAt == null) return "방금 전";
        java.time.Duration duration = java.time.Duration.between(createdAt, java.time.LocalDateTime.now());
        long minutes = duration.toMinutes();
        if (minutes < 1) return "방금 전";
        if (minutes < 60) return minutes + "분 전";
        long hours = duration.toHours();
        if (hours < 24) return hours + "시간 전";
        long days = duration.toDays();
        if (days == 1) return "어제";
        if (days < 7) return days + "일 전";
        return createdAt.toLocalDate().toString();
    }

    private QuizCard toQuizCard(Quiz quiz, Map<Long, Integer> userBestPercentages,
                               Map<Long, Integer> globalAveragePercentages) {
        // 전체 사용자 평균과 내 최고 기록을 섞지 않고 각각 전달합니다. 기록이 없으면 null (임의 값으로 채우지 않음).
        Integer average = globalAveragePercentages.get(quiz.getId());
        return new QuizCard(
                quiz,
                markFor(quiz),
                themeFor(quiz),
                difficultyLabel(quiz.getDifficulty()),
                quiz.getDifficulty(),
                average,
                userBestPercentages.get(quiz.getId()),
                progressTone(average != null ? average : 0)
        );
    }

    private String difficultyLabel(String difficulty) {
        return switch (difficulty) {
            case "easy" -> "쉬움";
            case "medium" -> "보통";
            case "hard" -> "어려움";
            default -> "기본";
        };
    }

    private String markFor(Quiz quiz) {
        String topic = quiz.getTopic() == null ? "" : quiz.getTopic().toLowerCase();
        if (topic.contains("javascript")) return "JS";
        if (topic.contains("typescript")) return "TS";
        if (topic.contains("html")) return "5";
        if (topic.contains("css")) return "3";
        if (topic.contains("java")) return "J";
        if (topic.contains("python")) return "PY";
        if (topic.contains("sql")) return "DB";
        if (topic.contains("react")) return "RX";
        if (topic.contains("git")) return "G";
        if (topic.contains("알고리즘")) return "AL";
        return quiz.getIcon() != null && !quiz.getIcon().isBlank() ? quiz.getIcon() : "Q";
    }

    private String themeFor(Quiz quiz) {
        String topic = quiz.getTopic() == null ? "" : quiz.getTopic().toLowerCase();
        if (topic.contains("javascript")) return "js";
        if (topic.contains("typescript")) return "ts";
        if (topic.contains("html")) return "html";
        if (topic.contains("css")) return "css";
        if (topic.contains("java") || topic.contains("spring")) return "java";
        if (topic.contains("python")) return "python";
        if (topic.contains("sql")) return "sql";
        if (topic.contains("react")) return "react";
        if (topic.contains("git")) return "git";
        return "project";
    }

    private String progressTone(int accuracy) {
        if (accuracy >= 85) return "green";
        if (accuracy >= 70) return "blue";
        if (accuracy >= 55) return "violet";
        return "orange";
    }
}
