package com.codingedu.controller;

import com.codingedu.entity.User;
import com.codingedu.service.ChallengeService;
import com.codingedu.service.LessonService;
import com.codingedu.service.PostService;
import com.codingedu.service.QuizService;
import com.codingedu.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class UserProfileController {

    private final UserService userService;
    private final LessonService lessonService;
    private final QuizService quizService;
    private final PostService postService;
    private final ChallengeService challengeService;

    public UserProfileController(UserService userService, LessonService lessonService,
                                 QuizService quizService, PostService postService,
                                 ChallengeService challengeService) {
        this.userService = userService;
        this.lessonService = lessonService;
        this.quizService = quizService;
        this.postService = postService;
        this.challengeService = challengeService;
    }

    @GetMapping("/user/{nickname}")
    public String profile(@PathVariable String nickname, Model model) {
        User user;
        try {
            user = userService.findByNickname(nickname);
        } catch (IllegalArgumentException e) {
            return "redirect:/community";
        }

        var allCourses = lessonService.getAllCourses();
        var completedCountMap = lessonService.getCompletedCountMap(user);
        int totalLessons = allCourses.stream().mapToInt(c -> c.getLessonCount()).sum();
        int completedLessons = lessonService.getTotalCompletedCount(user);

        var quizResults = quizService.getUserResults(user);
        int totalQuizzes = quizResults.size();
        String bestGrade = quizResults.stream()
                .map(r -> r.getGrade())
                .min((a, b) -> gradeRank(a) - gradeRank(b))
                .orElse(null);

        int postCount = postService.countPostsByUser(user);

        var completedChallenges = challengeService.getJoinedParticipations(user).stream()
                .filter(p -> p.isCompleted())
                .collect(Collectors.toList());

        // 진도율 맵 (언어별 %)
        var langProgressMap = allCourses.stream()
                .filter(c -> completedCountMap.getOrDefault(c.getLang(), 0) > 0)
                .collect(Collectors.toMap(
                        c -> c.getLang(),
                        c -> c.getLessonCount() > 0
                                ? (int) Math.round(completedCountMap.get(c.getLang()) * 100.0 / c.getLessonCount())
                                : 0
                ));

        model.addAttribute("profileUser", user);
        model.addAttribute("allCourses", allCourses);
        model.addAttribute("completedCountMap", completedCountMap);
        model.addAttribute("langProgressMap", langProgressMap);
        model.addAttribute("totalLessons", totalLessons);
        model.addAttribute("completedLessons", completedLessons);
        model.addAttribute("totalQuizzes", totalQuizzes);
        model.addAttribute("bestGrade", bestGrade);
        model.addAttribute("postCount", postCount);
        model.addAttribute("completedChallenges", completedChallenges);
        model.addAttribute("recentPosts", postService.getRecentPostsByUser(user));
        model.addAttribute("recentQuizResults", quizResults.size() > 5 ? quizResults.subList(0, 5) : quizResults);

        return "user-profile";
    }

    private int gradeRank(String g) {
        return switch (g) { case "A" -> 0; case "B" -> 1; case "C" -> 2; case "D" -> 3; default -> 4; };
    }
}
