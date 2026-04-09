package com.codingedu.controller;

import com.codingedu.entity.LessonCourse;
import com.codingedu.entity.LessonNote;
import com.codingedu.entity.User;
import com.codingedu.repository.LessonNoteRepository;
import com.codingedu.security.CustomUserDetails;
import com.codingedu.service.LessonContentService;
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
    private final LessonContentService lessonContentService;
    private final UserService userService;
    private final LessonNoteRepository noteRepository;

    public LearnController(LessonService lessonService,
                           LessonContentService lessonContentService,
                           UserService userService,
                           LessonNoteRepository noteRepository) {
        this.lessonService = lessonService;
        this.lessonContentService = lessonContentService;
        this.userService = userService;
        this.noteRepository = noteRepository;
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
            // 노트 맵: lessonIdx -> content
            Map<Integer, String> noteMap = noteRepository.findByUserAndLang(user, safeLang).stream()
                    .collect(java.util.stream.Collectors.toMap(
                            LessonNote::getLessonIdx, LessonNote::getContent));
            model.addAttribute("noteMap", noteMap);
        }
        model.addAttribute("coursesPayload",
                lessonContentService.buildCoursesPayload(lessonService.getAllCourses()));
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

    // 노트 저장 API (AJAX)
    @PostMapping("/api/learn/{lang}/note")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveNote(
            @PathVariable String lang,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) return ResponseEntity.status(401).body(Map.of("success", false));
        if (!VALID_LANGS.contains(lang)) return ResponseEntity.badRequest().body(Map.of("success", false));

        int lessonIdx;
        try { lessonIdx = ((Number) body.getOrDefault("lessonIdx", -1)).intValue(); }
        catch (ClassCastException e) { return ResponseEntity.badRequest().body(Map.of("success", false)); }
        String content = (String) body.getOrDefault("content", "");
        if (lessonIdx < 0) return ResponseEntity.badRequest().body(Map.of("success", false));

        User user = userService.findByUsername(userDetails.getUsername());
        LessonNote note = noteRepository.findByUserAndLangAndLessonIdx(user, lang, lessonIdx)
                .orElse(new LessonNote());
        note.setUser(user);
        note.setLang(lang);
        note.setLessonIdx(lessonIdx);
        note.setContent(content);
        noteRepository.save(note);

        return ResponseEntity.ok(Map.of("success", true));
    }

    // 노트 조회 API (AJAX)
    @GetMapping("/api/learn/{lang}/note")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getNote(
            @PathVariable String lang,
            @RequestParam int lessonIdx,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) return ResponseEntity.status(401).body(Map.of("success", false));
        if (!VALID_LANGS.contains(lang)) return ResponseEntity.badRequest().body(Map.of("success", false));
        User user = userService.findByUsername(userDetails.getUsername());
        String content = noteRepository.findByUserAndLangAndLessonIdx(user, lang, lessonIdx)
                .map(LessonNote::getContent).orElse("");
        return ResponseEntity.ok(Map.of("success", true, "content", content));
    }
}
