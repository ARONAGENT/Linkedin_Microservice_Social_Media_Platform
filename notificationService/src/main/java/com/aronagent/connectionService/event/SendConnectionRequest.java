package com.aronagent.connectionService.event;

import lombok.Data;

@Data
public class SendConnectionRequest {

    Long senderId;
    Long receiverId;
    String message;
}
