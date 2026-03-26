package com.codingedu.controller;

import com.codingedu.entity.User;
import com.codingedu.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

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
    public String registerProcess(@Valid User user, BindingResult bindingResult, Model model) {
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
        return "redirect:/login?registered=true";
    }
}
