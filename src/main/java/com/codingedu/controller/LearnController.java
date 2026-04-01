package com.codingedu.controller;

import com.codingedu.entity.LessonCourse;
import com.codingedu.entity.User;
import com.codingedu.security.CustomUserDetails;
import com.codingedu.service.LessonService;
import com.codingedu.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
public class LearnController {

    private static final java.util.Set<String> VALID_LANGS = java.util.Set.of(
        "html", "css", "javascript", "typescript", "java", "kotlin",
        "c", "cpp", "swift", "python"
    );

    private final LessonService lessonService;
    private final UserService userService;

    public LearnController(LessonService lessonService, UserService userService) {
        this.lessonService = lessonService;
        this.userService = userService;
    }

    // 학습 목록 페이지
    @GetMapping("/learn")
    public String learn(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        List<LessonCourse> courses = lessonService.getAllCourses();
        model.addAttribute("courses", courses);

        if (userDetails != null) {
            User user = userService.findByUsername(userDetails.getUsername());
            model.addAttribute("completedCounts", lessonService.getCompletedCountMap(user));
        }
        return "learn";
    }

    // 강의 상세 페이지
    @GetMapping("/learn/{lang}")
    public String learnDetail(@PathVariable String lang,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              Model model) {
        String safeLang = VALID_LANGS.contains(lang) ? lang : "html";
        LessonCourse course = lessonService.getCourseByLang(safeLang);

        model.addAttribute("lang", safeLang);
        model.addAttribute("langTitle", safeLang.substring(0, 1).toUpperCase() + safeLang.substring(1));
        model.addAttribute("course", course);
        model.addAttribute("isLoggedIn", userDetails != null);

        if (userDetails != null) {
            User user = userService.findByUsername(userDetails.getUsername());
            Set<Integer> completedIndices = lessonService.getCompletedIndices(user, safeLang);
            model.addAttribute("completedIndices", completedIndices);
        }
        return "learn-detail";
    }

    // 강의 완료 API (AJAX)
    @PostMapping("/api/learn/{lang}/complete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markComplete(
            @PathVariable String lang,
            @RequestBody Map<String, Integer> body,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }
        if (!VALID_LANGS.contains(lang)) {
            return ResponseEntity.badRequest().body(Map.of("success", false));
        }
        int lessonIdx = body.getOrDefault("lessonIdx", -1);
        if (lessonIdx < 0) {
            return ResponseEntity.badRequest().body(Map.of("success", false));
        }

        User user = userService.findByUsername(userDetails.getUsername());
        lessonService.markComplete(user, lang, lessonIdx);

        LessonCourse course = lessonService.getCourseByLang(lang);
        int completed = lessonService.getCompletedIndices(user, lang).size();
        int total = (course != null) ? course.getLessonCount() : 0;

        return ResponseEntity.ok(Map.of("success", true, "completed", completed, "total", total));
    }
}
