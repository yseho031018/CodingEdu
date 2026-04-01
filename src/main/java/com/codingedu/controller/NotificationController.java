package com.codingedu.controller;

import com.codingedu.entity.User;
import com.codingedu.security.CustomUserDetails;
import com.codingedu.service.NotificationService;
import com.codingedu.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) return "redirect:/login";
        User user = userService.findByUsername(userDetails.getUsername());
        model.addAttribute("notifications", notificationService.getNotifications(user));
        notificationService.markAllAsRead(user);
        return "notifications";
    }

    @PostMapping("/read-all")
    public String readAll(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) return "redirect:/login";
        User user = userService.findByUsername(userDetails.getUsername());
        notificationService.markAllAsRead(user);
        return "redirect:/notifications";
    }
}
