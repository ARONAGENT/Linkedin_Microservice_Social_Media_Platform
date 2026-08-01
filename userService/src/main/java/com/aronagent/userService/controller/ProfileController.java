package com.aronagent.userService.controller;

import com.aronagent.userService.dto.ProfileRequestDto;
import com.aronagent.userService.dto.ProfileResponseDto;
import com.aronagent.userService.dto.UserDto;
import com.aronagent.userService.dto.UserSummaryDto;
import com.aronagent.userService.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProfileResponseDto> createProfile(@ModelAttribute ProfileRequestDto requestDto) {
        return ResponseEntity.ok(profileService.createProfile(requestDto));
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProfileResponseDto> updateProfile(@ModelAttribute ProfileRequestDto requestDto) {
        return ResponseEntity.ok(profileService.updateProfile(requestDto));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser() {
        return ResponseEntity.ok(profileService.getMyProfile());
    }

    @GetMapping("/me/detail")
    public ResponseEntity<ProfileResponseDto> getCurrentUserProfile() {
        return ResponseEntity.ok(profileService.getCurrentUserProfile());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ProfileResponseDto> getProfileByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(profileService.getProfileByUserId(userId));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteProfile() {
        profileService.deleteProfile();
        return ResponseEntity.noContent().build();
    }

    // ---------- internal endpoints, called by other services via Feign ----------
    // NOTE: put these behind an internal-only network path / API gateway rule,
    // don't expose them on the public-facing route the same way as /me etc.

    @GetMapping("/internal/summary/{userId}")
    public ResponseEntity<UserSummaryDto> getUserSummary(@PathVariable Long userId) {
        return ResponseEntity.ok(profileService.getUserSummary(userId));
    }

    @PostMapping("/internal/summary/batch")
    public ResponseEntity<List<UserSummaryDto>> getUserSummaries(@RequestBody List<Long> userIds) {
        return ResponseEntity.ok(profileService.getUserSummaries(userIds));
    }

    @GetMapping("/internal/exists/{userId}")
    public ResponseEntity<Boolean> userExists(@PathVariable Long userId) {
        return ResponseEntity.ok(profileService.userExists(userId));
    }
}