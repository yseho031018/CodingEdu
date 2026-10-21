package com.codingedu.controller;

import com.codingedu.entity.Notification;
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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
        List<Notification> notifications = notificationService.getNotifications(user);
        // 화면에서 '새 알림'으로 구분하도록, 모두 읽음 처리하기 전에 읽지 않은 알림을 기억
        Set<Long> unreadIds = notifications.stream()
                .filter(n -> !n.isRead())
                .map(Notification::getId)
                .collect(Collectors.toSet());
        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadIds", unreadIds);
        notificationService.markAllAsRead(user);
        // 상단 종 아이콘 배지는 요청 처음에 계산되므로, 방금 모두 읽었음을 반영
        model.addAttribute("unreadNotificationCount", 0L);
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
