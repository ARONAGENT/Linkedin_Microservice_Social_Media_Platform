package com.aronagent.notificationService.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "CONNECTIONS-SERVICE")
public interface ConnectionsClient {

    @PostMapping("/core/accept/{userId}")
    void acceptConnectionRequest(@PathVariable("userId") Long userId);

    @PostMapping("/core/reject/{userId}")
    void rejectConnectionRequest(@PathVariable("userId") Long userId);
}