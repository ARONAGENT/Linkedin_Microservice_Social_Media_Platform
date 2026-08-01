package com.aronagent.userService.service;

import com.aronagent.userService.dto.ProfileRequestDto;
import com.aronagent.userService.dto.ProfileResponseDto;
import com.aronagent.userService.dto.UserDto;
import com.aronagent.userService.dto.UserSummaryDto;

import java.util.List;

public interface ProfileService {
    ProfileResponseDto createProfile(ProfileRequestDto requestDto);
    UserDto getMyProfile();
    ProfileResponseDto getProfileByUserId(Long userId);
    ProfileResponseDto updateProfile(ProfileRequestDto requestDto);
    void deleteProfile();
    ProfileResponseDto getCurrentUserProfile();

    // internal / inter-service
    UserSummaryDto getUserSummary(Long userId);
    List<UserSummaryDto> getUserSummaries(List<Long> userIds);
    boolean userExists(Long userId);
}