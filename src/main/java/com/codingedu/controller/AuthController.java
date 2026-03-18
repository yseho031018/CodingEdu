package com.codingedu.controller;

import com.codingedu.entity.User;
import com.codingedu.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

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
    public String registerProcess(User user, Model model) {
        if (userService.isUsernameTaken(user.getUsername())) {
            model.addAttribute("error", "이미 존재하는 아이디입니다.");
            return "register";
        }

        if (userService.isNicknameTaken(user.getNickname())) {
            model.addAttribute("error", "이미 사용하는 닉네임입니다.");
            return "register";
        }

        userService.register(user);
        return "redirect:/login?registered=true";
    }
}
