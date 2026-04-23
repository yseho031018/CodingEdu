package com.codingedu.controller;

import com.codingedu.entity.User;
import com.codingedu.security.CustomUserDetails;
import com.codingedu.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // 로그인 화면
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // 회원가입 화면
    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    // 회원가입 폼 제출시 DB 저장 처리
    @PostMapping("/register")
    public String registerProcess(@Valid User user, BindingResult bindingResult, Model model,
                                  HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();

        // Bean Validation 에러를 필드별로 수집
        if (bindingResult.hasErrors()) {
            bindingResult.getFieldErrors().forEach(e ->
                fieldErrors.putIfAbsent(e.getField(), e.getDefaultMessage()));
        }

        // 중복 체크 (형식은 맞지만 이미 사용 중인 경우)
        if (!fieldErrors.containsKey("username") && userService.isUsernameTaken(user.getUsername())) {
            fieldErrors.put("username", "이미 존재하는 아이디입니다.");
        }
        if (!fieldErrors.containsKey("nickname") && userService.isNicknameTaken(user.getNickname())) {
            fieldErrors.put("nickname", "이미 사용하는 닉네임입니다.");
        }

        if (!fieldErrors.isEmpty()) {
            model.addAttribute("fieldErrors", fieldErrors);
            return "register";
        }

        userService.register(user);

        // 가입 완료 후 자동 로그인 처리
        CustomUserDetails userDetails = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        HttpSession session = request.getSession(true);
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext()
        );

        return "redirect:/";
    }

    // 비밀번호 찾기 - 1단계: 본인 확인 폼
    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    // 비밀번호 찾기 - 1단계: 아이디 + 이메일 검증
    @PostMapping("/forgot-password/verify")
    public String forgotPasswordVerify(@RequestParam(name = "username") String username,
                                       @RequestParam(name = "email") String email,
                                       HttpSession session,
                                       Model model) {
        if (!userService.verifyUsernameAndEmail(username, email)) {
            model.addAttribute("verifyError", "아이디 또는 이메일이 일치하지 않습니다.");
            return "forgot-password";
        }
        // 세션에 인증된 계정 정보 저장 (2단계에서 사용)
        session.setAttribute("resetUsername", username);
        session.setAttribute("resetEmail", email);
        return "redirect:/forgot-password/reset";
    }

    // 비밀번호 찾기 - 2단계: 새 비밀번호 입력 폼
    @GetMapping("/forgot-password/reset")
    public String resetPasswordPage(HttpSession session, Model model) {
        if (session.getAttribute("resetUsername") == null) {
            return "redirect:/forgot-password";
        }
        model.addAttribute("resetStep", true);
        return "forgot-password";
    }

    // 비밀번호 찾기 - 2단계: 새 비밀번호 저장
    @PostMapping("/forgot-password/reset")
    public String resetPasswordProcess(@RequestParam(name = "newPassword") String newPassword,
                                       @RequestParam(name = "confirmPassword") String confirmPassword,
                                       HttpSession session,
                                       Model model) {
        String username = (String) session.getAttribute("resetUsername");
        String email = (String) session.getAttribute("resetEmail");

        if (username == null || email == null) {
            return "redirect:/forgot-password";
        }
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("resetStep", true);
            model.addAttribute("resetError", "비밀번호가 일치하지 않습니다.");
            return "forgot-password";
        }
        if (newPassword.length() < 8) {
            model.addAttribute("resetStep", true);
            model.addAttribute("resetError", "비밀번호는 8자 이상이어야 합니다.");
            return "forgot-password";
        }

        userService.resetPassword(username, email, newPassword);
        session.removeAttribute("resetUsername");
        session.removeAttribute("resetEmail");
        return "redirect:/login?passwordReset=true";
    }
}
