package com.aronagent.connectionService.controller;

import com.aronagent.connectionService.auth.AuthContextHolder;
import com.aronagent.connectionService.entities.Person;
import com.aronagent.connectionService.service.ConnectionsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/core")
@RequiredArgsConstructor
public class ConnectionsController {

    private final ConnectionsService connectionsService;

    @GetMapping("/first-degree")
    public ResponseEntity<List<Person>> getFirstDegreeConnections() {
        Long userId=AuthContextHolder.getCurrentUserId();
        List<Person> personList = connectionsService.getFirstDegreeConnectionsOfUser(userId);
        return ResponseEntity.ok(personList);
    }


    @GetMapping("/second-degree")
    public ResponseEntity<List<Person>> getSecondDegreeConnections() {
        Long userId=AuthContextHolder.getCurrentUserId();
        return ResponseEntity.ok(connectionsService.getSecondDegreeConnectionsOfUser(userId));
    }

    @GetMapping("/third-degree")
    public ResponseEntity<List<Person>> getThirdDegreeConnections() {
        Long userId=AuthContextHolder.getCurrentUserId();
        return ResponseEntity.ok(connectionsService.getThirdDegreeConnectionsOfUser(userId));
    }

    @GetMapping("/all-connections")
    public ResponseEntity<List<Person>> getAllConnections() {
        Long userId=AuthContextHolder.getCurrentUserId();
        return ResponseEntity.ok(connectionsService.getAllUsersExceptMe(userId));
    }

    @PostMapping("/request/{userId}")
    public ResponseEntity<Void> sendConnectionRequest(@PathVariable Long userId) {
        connectionsService.sendConnectionRequest(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/accept/{userId}")
    public ResponseEntity<Void> acceptConnectionRequest(@PathVariable Long userId) {
        connectionsService.acceptConnectionRequest(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reject/{userId}")
    public ResponseEntity<Void> rejectConnectionRequest(@PathVariable Long userId) {
        connectionsService.rejectConnectionRequest(userId);
        return ResponseEntity.noContent().build();
    }
}
