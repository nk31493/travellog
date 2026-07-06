package com.side.travellog.domain.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<Map<String, Object>> getNotifications(@AuthenticationPrincipal UserDetails userDetails) {
        return notificationService.getNotifications(userDetails.getUsername())
                .stream().map(n -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", n.getId());
                    map.put("message", n.getMessage());
                    map.put("link", n.getLink());
                    map.put("isRead", n.isRead());
                    map.put("createdAt", n.getCreatedAt().toString());
                    return map;
                }).toList();
    }

    @GetMapping("/unread-count")
    public Map<String, Integer> getUnreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        return Map.of("count", notificationService.getUnreadCount(userDetails.getUsername()));
    }

    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(@AuthenticationPrincipal UserDetails userDetails) {
        notificationService.markAllAsRead(userDetails.getUsername());
        return ResponseEntity.ok().build();
    }
}