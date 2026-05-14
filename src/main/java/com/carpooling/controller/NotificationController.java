package com.carpooling.controller;

import com.carpooling.model.Notification;
import com.carpooling.service.CustomUserDetails;
import com.carpooling.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
    @Autowired
    private NotificationService notificationService;

    @GetMapping("/my")
    public Map<String, Object> myNotifications(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        List<Notification> notifications = notificationService.getMyNotifications(userId);
        long unread = notificationService.getUnreadCount(userId);
        Map<String, Object> response = new HashMap<>();
        response.put("notifications", notifications);
        response.put("unreadCount", unread);
        return response;
    }

    @PutMapping("/{notificationId}/read")
    public Notification markRead(@AuthenticationPrincipal CustomUserDetails userDetails,
                                 @PathVariable Long notificationId) {
        return notificationService.markAsRead(userDetails.getUser().getId(), notificationId);
    }
}
