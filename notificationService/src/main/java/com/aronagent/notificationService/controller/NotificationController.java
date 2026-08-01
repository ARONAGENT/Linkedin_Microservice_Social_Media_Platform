package com.aronagent.notificationService.controller;

import com.aronagent.notificationService.auth.AuthContextHolder;
import com.aronagent.notificationService.dto.NotificationDto;
import com.aronagent.notificationService.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/core")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationDto>> list() {
        Long userId= AuthContextHolder.getCurrentUserId();
        return ResponseEntity.ok(notificationService.getNotifications(userId));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> unreadCount() {
        Long userId= AuthContextHolder.getCurrentUserId();

        return ResponseEntity.ok(notificationService.getUnreadCount(userId));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllRead() {
        Long userId= AuthContextHolder.getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/respond")
    public ResponseEntity<Void> respond(@PathVariable Long id, @RequestBody Map<String, String> body) {
        notificationService.respondToRequest(id, body.get("action"));
        return ResponseEntity.noContent().build();
    }
}