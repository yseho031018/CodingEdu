package com.codingedu.controller;

import com.codingedu.service.ChallengeService;
import com.codingedu.service.LessonService;
import com.codingedu.service.PostService;
import com.codingedu.service.QuizService;
import com.codingedu.service.UserService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final QuizService quizService;
    private final ChallengeService challengeService;
    private final PostService postService;
    private final LessonService lessonService;

    public AdminController(UserService userService, QuizService quizService,
                           ChallengeService challengeService, PostService postService,
                           LessonService lessonService) {
        this.userService = userService;
        this.quizService = quizService;
        this.challengeService = challengeService;
        this.postService = postService;
        this.lessonService = lessonService;
    }

    @GetMapping
    public String dashboard(@RequestParam(required = false, defaultValue = "dashboard") String tab,
                            @RequestParam(required = false) Long quizId,
                            Model model) {
        var allUsers = userService.findAllUsers();
        var allQuizzes = quizService.getQuizzesByDifficulty("all");
        var allChallenges = challengeService.getAllChallenges();

        model.addAttribute("userCount", allUsers.size());
        model.addAttribute("quizCount", allQuizzes.size());
        model.addAttribute("challengeCount", allChallenges.size());
        model.addAttribute("courseCount", lessonService.getAllCourses().size());
        model.addAttribute("users", allUsers);
        model.addAttribute("quizzes", allQuizzes);
        model.addAttribute("challenges", allChallenges);
        model.addAttribute("activeTab", tab);

        if (quizId != null) {
            model.addAttribute("selectedQuiz", quizService.getQuizById(quizId));
        }
        return "admin";
    }

    @PostMapping("/users/{id}/role")
    public String updateRole(@PathVariable Long id,
                             @RequestParam String role,
                             RedirectAttributes ra) {
        userService.updateRole(id, role);
        ra.addFlashAttribute("msg", "권한이 변경되었습니다.");
        return "redirect:/admin?tab=users";
    }

    // ── 퀴즈 관리 ───────────────────────────────────────────────

    @PostMapping("/quizzes/add")
    public String addQuiz(@RequestParam String topic,
                          @RequestParam String icon,
                          @RequestParam String title,
                          @RequestParam String description,
                          @RequestParam String difficulty,
                          @RequestParam int timeLimit,
                          RedirectAttributes ra) {
        quizService.createQuiz(topic, icon, title, description, difficulty, timeLimit);
        ra.addFlashAttribute("msg", "퀴즈가 추가되었습니다.");
        return "redirect:/admin?tab=quizzes";
    }

    @PostMapping("/quizzes/{quizId}/questions/add")
    public String addQuestion(@PathVariable Long quizId,
                              @RequestParam String questionText,
                              @RequestParam(required = false, defaultValue = "") String explanation,
                              @RequestParam List<String> choiceTexts,
                              @RequestParam int correctIndex,
                              RedirectAttributes ra) {
        quizService.addQuestion(quizId, questionText, explanation, choiceTexts, correctIndex);
        ra.addFlashAttribute("msg", "문제가 추가되었습니다.");
        return "redirect:/admin?tab=quizzes&quizId=" + quizId;
    }

    @PostMapping("/quizzes/{id}/delete")
    public String deleteQuiz(@PathVariable Long id, RedirectAttributes ra) {
        quizService.deleteQuiz(id);
        ra.addFlashAttribute("msg", "퀴즈가 삭제되었습니다.");
        return "redirect:/admin?tab=quizzes";
    }

    @PostMapping("/quizzes/{quizId}/questions/{questionId}/delete")
    public String deleteQuestion(@PathVariable Long quizId,
                                 @PathVariable Long questionId,
                                 RedirectAttributes ra) {
        quizService.deleteQuestion(quizId, questionId);
        ra.addFlashAttribute("msg", "문항이 삭제되었습니다.");
        return "redirect:/admin?tab=quizzes&quizId=" + quizId;
    }

    // ── 챌린지 관리 ─────────────────────────────────────────────

    @PostMapping("/challenges/add")
    public String addChallenge(@RequestParam String title,
                               @RequestParam String description,
                               @RequestParam String icon,
                               @RequestParam String status,
                               @RequestParam(required = false, defaultValue = "false") boolean featured,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                               @RequestParam int totalTasks,
                               RedirectAttributes ra) {
        challengeService.createChallenge(title, description, icon, status, featured,
                startDate, endDate, totalTasks);
        ra.addFlashAttribute("msg", "챌린지가 추가되었습니다.");
        return "redirect:/admin?tab=challenges";
    }

    @PostMapping("/challenges/{id}/status")
    public String updateChallengeStatus(@PathVariable Long id,
                                        @RequestParam String status,
                                        RedirectAttributes ra) {
        challengeService.updateStatus(id, status);
        ra.addFlashAttribute("msg", "상태가 변경되었습니다.");
        return "redirect:/admin?tab=challenges";
    }

    @PostMapping("/challenges/{id}/delete")
    public String deleteChallenge(@PathVariable Long id, RedirectAttributes ra) {
        challengeService.deleteChallenge(id);
        ra.addFlashAttribute("msg", "챌린지가 삭제되었습니다.");
        return "redirect:/admin?tab=challenges";
    }
}
