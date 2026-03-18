package com.codingedu.controller;

import com.codingedu.security.CustomUserDetails;
import com.codingedu.security.CustomUserDetailsService;
import com.codingedu.service.UserService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    private static final java.util.Set<String> VALID_LANGS = java.util.Set.of(
        "html", "css", "javascript", "typescript", "java", "kotlin", "c", "cpp", "swift", "python"
    );

    private final UserService userService;
    private final CustomUserDetailsService customUserDetailsService;

    public HomeController(UserService userService, CustomUserDetailsService customUserDetailsService) {
        this.userService = userService;
        this.customUserDetailsService = customUserDetailsService;
    }

    @GetMapping("/")
    public String index() {
        return "index"; // templates/index.html
    }

    @GetMapping("/challenge")
    public String challenge() {
        return "challenge"; // templates/challenge.html
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
