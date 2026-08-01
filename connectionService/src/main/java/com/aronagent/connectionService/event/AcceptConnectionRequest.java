package com.aronagent.connectionService.event;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AcceptConnectionRequest {
    Long senderId;
    Long receiverId;
    String message;
}
