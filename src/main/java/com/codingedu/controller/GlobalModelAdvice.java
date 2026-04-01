package com.codingedu.controller;

import com.codingedu.entity.User;
import com.codingedu.security.CustomUserDetails;
import com.codingedu.service.NotificationService;
import com.codingedu.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAdvice {

    private final NotificationService notificationService;
    private final UserService userService;

    public GlobalModelAdvice(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @ModelAttribute("unreadNotificationCount")
    public long unreadNotificationCount(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) return 0;
        User user = userService.findByUsername(userDetails.getUsername());
        return notificationService.getUnreadCount(user);
    }
}
