package com.aronagent.notificationService.service;

import com.aronagent.notificationService.dto.NotificationDto;
import com.aronagent.notificationService.entity.Notification;

import java.util.List;

public interface NotificationService {
    void addNotification(Notification notification);
    List<NotificationDto> getNotifications(Long userId);
    long getUnreadCount(Long userId);
    void markAsRead(Long notificationId);
    void markAllAsRead(Long userId);
    void respondToRequest(Long notificationId, String action); // "accept" | "reject"
}
