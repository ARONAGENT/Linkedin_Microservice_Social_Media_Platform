package com.aronagent.postService.client;

import com.aronagent.postService.dto.UserSummaryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "user-service", path = "/users/profile")
public interface UserServiceClient {

    @GetMapping("/internal/summary/{userId}")
    UserSummaryDto getUserSummary(@PathVariable("userId") Long userId);

    @PostMapping("/internal/summary/batch")
    List<UserSummaryDto> getUserSummaries(@RequestBody List<Long> userIds);

    @GetMapping("/internal/exists/{userId}")
    Boolean userExists(@PathVariable("userId") Long userId);
}