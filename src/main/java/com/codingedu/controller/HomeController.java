package com.codingedu.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index() {
        return "index"; // templates/index.html
    }

    @GetMapping("/learn")
    public String learn() {
        return "learn"; // templates/learn.html
    }

    @GetMapping("/quiz")
    public String quiz() {
        return "quiz"; // templates/quiz.html
    }

    @GetMapping("/challenge")
    public String challenge() {
        return "challenge"; // templates/challenge.html
    }

    @GetMapping("/community")
    public String community() {
        return "community"; // templates/community.html
    }
}
