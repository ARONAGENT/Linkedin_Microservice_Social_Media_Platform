package com.aronagent.postService.client;

import com.aronagent.postService.dto.PersonDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "connections-service", path = "/connections")
public interface ConnectionsServiceClient {

    @GetMapping("/core/first-degree")
    List<PersonDto> getFirstDegreeConnections();
}
