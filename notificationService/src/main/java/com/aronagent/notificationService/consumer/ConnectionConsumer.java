package com.aronagent.notificationService.consumer;


import com.aronagent.connectionService.event.AcceptConnectionRequest;
import com.aronagent.connectionService.event.SendConnectionRequest;
import com.aronagent.notificationService.entity.Notification;
import com.aronagent.notificationService.service.NotificationServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConnectionConsumer {

    private final NotificationServiceImpl notificationService;

    @KafkaListener(topics = "send_connection_request")
    public void sendConnectionRequest(SendConnectionRequest sendConnectionRequest) {
        log.info("sendConnectionRequest: {}", sendConnectionRequest.getMessage());

        String message = String.format("User %d sent you a connection request.", sendConnectionRequest.getSenderId());
        Notification notification = Notification.builder()
                .message(message)
                .userId(sendConnectionRequest.getReceiverId())
                .type("CONNECTION_REQUEST")
                .relatedUserId(sendConnectionRequest.getSenderId())
                .build();
        notificationService.addNotification(notification);
    }

    @KafkaListener(topics = "accept_connection_request")
    public void acceptConnectionRequest(AcceptConnectionRequest event) {
        log.info("Received Event : {}", event);
        Notification notification = Notification.builder()
                .userId(event.getSenderId())
                .message(event.getMessage())
                .relatedUserId(event.getReceiverId())
                .type("CONNECTION_ACCEPTED")
                .build();
        notificationService.addNotification(notification);
    }

}
