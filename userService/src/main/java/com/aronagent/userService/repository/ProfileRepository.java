package com.aronagent.userService.repository;

import com.aronagent.userService.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
    Optional<Profile> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
    List<Profile> findByUserIdIn(List<Long> userIds); // add this line
}