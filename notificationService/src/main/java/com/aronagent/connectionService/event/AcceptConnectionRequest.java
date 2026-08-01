package com.aronagent.connectionService.event;


import lombok.Data;

@Data
public class AcceptConnectionRequest {
    Long senderId;
    Long receiverId;
    String message;
}
