package com.codingedu.controller;

import com.codingedu.entity.User;
import com.codingedu.security.CustomUserDetails;
import com.codingedu.security.CustomUserDetailsService;
import com.codingedu.service.LessonService;
import com.codingedu.service.PostService;
import com.codingedu.service.QuizService;
import com.codingedu.service.UserService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    private final UserService userService;
    private final CustomUserDetailsService customUserDetailsService;
    private final LessonService lessonService;
    private final QuizService quizService;
    private final PostService postService;

    public HomeController(UserService userService,
                          CustomUserDetailsService customUserDetailsService,
                          LessonService lessonService,
                          QuizService quizService,
                          PostService postService) {
        this.userService = userService;
        this.customUserDetailsService = customUserDetailsService;
        this.lessonService = lessonService;
        this.quizService = quizService;
        this.postService = postService;
    }

    @GetMapping("/")
    public String index(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("siteUserCount", userService.countAll());
        model.addAttribute("siteCourseCount", lessonService.getAllCourses().size());
        model.addAttribute("siteQuizCount", quizService.countAll());

        if (userDetails != null) {
            User user = userService.findByUsername(userDetails.getUsername());
            var allCourses = lessonService.getAllCourses();
            int totalLessons = allCourses.stream().mapToInt(c -> c.getLessonCount()).sum();
            int completed = lessonService.getTotalCompletedCount(user);
            var quizResults = quizService.getUserResults(user);
            int totalQuizzesTaken = quizResults.size();
            String bestGrade = quizResults.stream()
                    .map(r -> r.getGrade())
                    .min((a, b) -> gradeRank(a) - gradeRank(b))
                    .orElse(null);

            // 언어별 학습 진도율 (0~100)
            var completedCountMap = lessonService.getCompletedCountMap(user);
            java.util.Map<String, Integer> langProgressMap = allCourses.stream()
                    .collect(java.util.stream.Collectors.toMap(
                            c -> c.getLang(),
                            c -> c.getLessonCount() > 0
                                    ? (int) Math.round(completedCountMap.getOrDefault(c.getLang(), 0) * 100.0 / c.getLessonCount())
                                    : 0
                    ));

            model.addAttribute("dashUser", user);
            model.addAttribute("totalLessons", totalLessons);
            model.addAttribute("completedLessons", completed);
            model.addAttribute("totalQuizzesTaken", totalQuizzesTaken);
            model.addAttribute("bestGrade", bestGrade);
            model.addAttribute("recentPosts", postService.getRecentPostsByUser(user));
            model.addAttribute("langProgressMap", langProgressMap);
        }
        return "index";
    }

    private int gradeRank(String grade) {
        return switch (grade) {
            case "A" -> 0; case "B" -> 1; case "C" -> 2; case "D" -> 3; default -> 4;
        };
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }
        
        String username = auth.getName();
        try {
            var user = userService.findByUsername(username);
            model.addAttribute("user", user);
            return "settings";
        } catch (IllegalArgumentException e) {
            return "redirect:/login";
        }
    }

    @PostMapping("/settings/nickname")
    public String updateNickname(@RequestParam String newNickname, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }

        String username = auth.getName();
        try {
            userService.updateNickname(username, newNickname);
            model.addAttribute("successMessage", "닉네임이 변경되었습니다.");
            
            // Authentication 갱신
            updateSecurityContext(username);
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
        }
        
        var user = userService.findByUsername(username);
        model.addAttribute("user", user);
        return "settings";
    }

    @PostMapping("/settings/email")
    public String updateEmail(@RequestParam String newEmail, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }

        String username = auth.getName();
        try {
            userService.updateEmail(username, newEmail);
            model.addAttribute("successMessage", "이메일이 변경되었습니다.");
            
            // Authentication 갱신
            updateSecurityContext(username);
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
        }
        
        var user = userService.findByUsername(username);
        model.addAttribute("user", user);
        return "settings";
    }

    @PostMapping("/settings/password")
    public String updatePassword(@RequestParam String oldPassword, 
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }

        String username = auth.getName();
        
        // 새 비밀번호 확인
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("errorMessage", "새로운 비밀번호가 일치하지 않습니다.");
            var user = userService.findByUsername(username);
            model.addAttribute("user", user);
            return "settings";
        }

        // 비밀번호 변경
        boolean success = userService.updatePassword(username, oldPassword, newPassword);
        if (success) {
            model.addAttribute("successMessage", "비밀번호가 변경되었습니다.");
            
            // Authentication 갱신
            updateSecurityContext(username);
        } else {
            model.addAttribute("errorMessage", "이전 비밀번호가 일치하지 않습니다.");
        }
        
        var user = userService.findByUsername(username);
        model.addAttribute("user", user);
        return "settings";
    }

    @PostMapping("/settings/delete-account")
    public String deleteAccount(@RequestParam String confirmPassword,
                                HttpServletRequest request,
                                HttpServletResponse response,
                                Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }

        String username = auth.getName();
        boolean deleted = userService.deleteAccount(username, confirmPassword);
        if (!deleted) {
            model.addAttribute("errorMessage", "비밀번호가 일치하지 않습니다.");
            var user = userService.findByUsername(username);
            model.addAttribute("user", user);
            return "settings";
        }

        new SecurityContextLogoutHandler().logout(request, response, auth);
        return "redirect:/?accountDeleted=true";
    }

    /**
     * SecurityContext의 Authentication 객체를 최신 사용자 정보로 갱신
     */
    private void updateSecurityContext(String username) {
        // 최신 사용자 정보 로드
        CustomUserDetails updatedUserDetails = (CustomUserDetails) customUserDetailsService.loadUserByUsername(username);
        
        // 새로운 Authentication 객체 생성
        UsernamePasswordAuthenticationToken newAuth = 
            new UsernamePasswordAuthenticationToken(
                updatedUserDetails, 
                updatedUserDetails.getPassword(), 
                updatedUserDetails.getAuthorities()
            );
        
        // SecurityContext 업데이트
        SecurityContextHolder.getContext().setAuthentication(newAuth);
    }
}
