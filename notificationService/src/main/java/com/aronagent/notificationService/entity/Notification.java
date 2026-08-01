package com.aronagent.notificationService.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId; // recipient

    private String message;

    @Builder.Default
    private boolean isRead = false;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // "LIKE", "CONNECTION_REQUEST", "CONNECTION_ACCEPTED" — helps frontend pick icon
    private String type;

    // populated only for CONNECTION_REQUEST type: who sent it (needed for accept/reject)
    private Long relatedUserId;
}