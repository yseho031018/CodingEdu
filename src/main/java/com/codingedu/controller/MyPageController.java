package com.codingedu.controller;

import com.codingedu.entity.LessonCourse;
import com.codingedu.entity.QuizResult;
import com.codingedu.entity.User;
import com.codingedu.service.LessonService;
import com.codingedu.service.PostService;
import com.codingedu.service.QuizService;
import com.codingedu.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/mypage")
public class MyPageController {

    private final UserService userService;
    private final LessonService lessonService;
    private final QuizService quizService;
    private final PostService postService;

    public MyPageController(UserService userService, LessonService lessonService,
                            QuizService quizService, PostService postService) {
        this.userService = userService;
        this.lessonService = lessonService;
        this.quizService = quizService;
        this.postService = postService;
    }

    @GetMapping
    public String myPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername());

        // 학습 진도
        List<LessonCourse> allCourses = lessonService.getAllCourses();
        Map<String, Integer> completedCountMap = lessonService.getCompletedCountMap(user);
        int totalLessonsCompleted = lessonService.getTotalCompletedCount(user);
        int totalLessons = allCourses.stream().mapToInt(LessonCourse::getLessonCount).sum();

        // 퀴즈 이력 (최근 5개)
        List<QuizResult> quizResults = quizService.getUserResults(user);
        List<QuizResult> recentQuizResults = quizResults.size() > 5
                ? quizResults.subList(0, 5) : quizResults;

        // 최고 퀴즈 등급 계산
        String bestGrade = quizResults.stream()
                .map(QuizResult::getGrade)
                .min((a, b) -> gradeRank(a) - gradeRank(b))
                .orElse("-");

        // 커뮤니티 게시글
        int postCount = postService.countPostsByUser(user);

        model.addAttribute("user", user);
        model.addAttribute("allCourses", allCourses);
        model.addAttribute("completedCountMap", completedCountMap);
        model.addAttribute("totalLessonsCompleted", totalLessonsCompleted);
        model.addAttribute("totalLessons", totalLessons);
        model.addAttribute("recentQuizResults", recentQuizResults);
        model.addAttribute("totalQuizzesTaken", quizResults.size());
        model.addAttribute("bestGrade", bestGrade);
        model.addAttribute("recentPosts", postService.getRecentPostsByUser(user));
        model.addAttribute("postCount", postCount);

        return "mypage";
    }

    // A=0, B=1, C=2, D=3, F=4 (낮을수록 좋음)
    private int gradeRank(String grade) {
        return switch (grade) {
            case "A" -> 0;
            case "B" -> 1;
            case "C" -> 2;
            case "D" -> 3;
            default -> 4;
        };
    }
}
