package com.aronagent.notificationService.service;

import com.aronagent.notificationService.client.ConnectionsClient;
import com.aronagent.notificationService.dto.NotificationDto;
import com.aronagent.notificationService.entity.Notification;
import com.aronagent.notificationService.exception.ResourceNotFoundException;
import com.aronagent.notificationService.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final ConnectionsClient connectionsClient; // Feign client -> connection-service

    @Override
    public void addNotification(Notification notification) {
        notificationRepository.save(notification);
    }

    @Override
    public List<NotificationDto> getNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    public void markAllAsRead(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        notifications.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(notifications);
    }

    @Override
    public void respondToRequest(Long notificationId, String action) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (notification.getRelatedUserId() == null) {
            throw new IllegalStateException("This notification has no associated connection request");
        }

        if ("accept".equalsIgnoreCase(action)) {
            connectionsClient.acceptConnectionRequest(notification.getRelatedUserId());
        } else if ("reject".equalsIgnoreCase(action)) {
            connectionsClient.rejectConnectionRequest(notification.getRelatedUserId());
        } else {
            throw new IllegalArgumentException("Invalid action: " + action);
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    private NotificationDto toDto(Notification n) {
        NotificationDto dto = new NotificationDto();
        BeanUtils.copyProperties(n, dto);
        return dto;
    }
}
