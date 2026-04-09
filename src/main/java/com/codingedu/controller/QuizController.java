package com.codingedu.controller;

import com.codingedu.entity.Choice;
import com.codingedu.entity.Question;
import com.codingedu.entity.Quiz;
import com.codingedu.entity.QuizResult;
import com.codingedu.entity.User;
import com.codingedu.security.CustomUserDetails;
import com.codingedu.service.QuizService;
import com.codingedu.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class QuizController {

    private final QuizService quizService;
    private final UserService userService;

    public QuizController(QuizService quizService, UserService userService) {
        this.quizService = quizService;
        this.userService = userService;
    }

    // 1. DB 기반 퀴즈 목록 (메인 퀴즈 페이지)
    @GetMapping("/quiz")
    public String list(@RequestParam(name = "difficulty", defaultValue = "all") String difficulty,
                       Model model) {
        model.addAttribute("quizzes", quizService.getQuizzesByDifficulty(difficulty));
        model.addAttribute("currentDifficulty", difficulty);
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
}
