package com.aronagent.connectionService.service;


import com.aronagent.connectionService.auth.AuthContextHolder;
import com.aronagent.connectionService.entities.Person;
import com.aronagent.connectionService.event.AcceptConnectionRequest;
import com.aronagent.connectionService.event.SendConnectionRequest;
import com.aronagent.connectionService.exception.BadRequestException;
import com.aronagent.connectionService.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConnectionsService {

    private final PersonRepository personRepository;
    private final KafkaTemplate<Long,SendConnectionRequest> sendConnectionRequestKafkaTemplate;
    private final KafkaTemplate<Long, AcceptConnectionRequest> acceptConnectionRequestKafkaTemplate;

    public List<Person> getFirstDegreeConnectionsOfUser(Long userId) {
        log.info("Getting first degree connections of user with ID: {}", userId);

        return personRepository.getFirstDegreeConnections(userId);
    }

    public List<Person> getSecondDegreeConnectionsOfUser(Long userId) {

        log.info("Getting second degree connections of user with ID: {}", userId);

        return personRepository.getSecondDegreeConnections(userId);
    }



    public List<Person> getThirdDegreeConnectionsOfUser(Long userId) {

        log.info("Getting third degree connections of user with ID: {}", userId);

        return personRepository.getThirdDegreeConnections(userId);
    }


    public void sendConnectionRequest(Long receiverId) {
        Long senderId = AuthContextHolder.getCurrentUserId();
        log.info("sending connection request with senderId: {}, receiverId: {}", senderId, receiverId);

        if (senderId.equals(receiverId)) {
            throw new BadRequestException("Both sender and receiver are the same");
        }

        boolean alreadySentRequest = personRepository.connectionRequestExists(senderId, receiverId);
        if (alreadySentRequest) {
            throw new BadRequestException("Connection request already exists, cannot send again");
        }

        boolean alreadyConnected = personRepository.alreadyConnected(senderId, receiverId);
        if (alreadyConnected) {
            throw new BadRequestException("Already connected users, cannot add connection request");
        }

        String message=String.format("Connection request send %s to %s",senderId,receiverId);

        SendConnectionRequest sendConnectionRequest= SendConnectionRequest.builder()
                .senderId(senderId).receiverId(receiverId).message(message).build();

        personRepository.addConnectionRequest(senderId, receiverId);
        sendConnectionRequestKafkaTemplate.send("send_connection_request",sendConnectionRequest);

        log.info("Successfully sent the connection request");
    }

    public void acceptConnectionRequest(Long senderId) {
        Long receiverId = AuthContextHolder.getCurrentUserId();
        log.info("Accepting a connection request with senderId: {}, receiverId: {}", senderId, receiverId);

        if (senderId.equals(receiverId)) {
            throw new BadRequestException("Both sender and receiver are the same");
        }

        boolean alreadyConnected = personRepository.alreadyConnected(senderId, receiverId);
        if (alreadyConnected) {
            throw new BadRequestException("Already connected users, cannot accept connection request again");
        }

        boolean alreadySentRequest = personRepository.connectionRequestExists(senderId, receiverId);
        if (!alreadySentRequest) {
            throw new BadRequestException("No Connection request exists, cannot accept without request");
        }

        personRepository.acceptConnectionRequest(senderId, receiverId);

        String message = String.format(
                "User %d accepted your connection request.",
                receiverId
        );

        AcceptConnectionRequest acceptConnectionRequest =
                AcceptConnectionRequest.builder()
                        .senderId(senderId)
                        .receiverId(receiverId)
                        .message(message)
                        .build();

        acceptConnectionRequestKafkaTemplate.send("accept_connection_request",acceptConnectionRequest);

        log.info("Successfully accepted the connection request with senderId: {}, receiverId: {}", senderId,
                receiverId);

    }

    public void rejectConnectionRequest(Long senderId) {
        Long receiverId = AuthContextHolder.getCurrentUserId();
        log.info("Rejecting a connection request with senderId: {}, receiverId: {}", senderId, receiverId);

        if (senderId.equals(receiverId)) {
            throw new BadRequestException("Both sender and receiver are the same");
        }

        boolean alreadySentRequest = personRepository.connectionRequestExists(senderId, receiverId);
        if (!alreadySentRequest) {
            throw new BadRequestException("No Connection request exists, cannot reject it");
        }

        personRepository.rejectConnectionRequest(senderId, receiverId);

        log.info("Successfully rejected the connection request with senderId: {}, receiverId: {}", senderId,
                receiverId);
    }

    public List<Person> getAllUsersExceptMe(Long userId) {
        return personRepository.findAllPersonsExceptMe(userId);
    }
}
