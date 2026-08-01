package com.aronagent.userService.service;

import com.aronagent.userService.auth.AuthContextHolder;
import com.aronagent.userService.client.UploaderServiceClient;
import com.aronagent.userService.dto.*;
import com.aronagent.userService.entity.Profile;
import com.aronagent.userService.entity.Project;
import com.aronagent.userService.entity.SocialLink;
import com.aronagent.userService.entity.User;
import com.aronagent.userService.exception.BadRequestException;
import com.aronagent.userService.repository.ProfileRepository;
import com.aronagent.userService.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;
    private final UploaderServiceClient uploaderServiceClient;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Override
    public ProfileResponseDto createProfile(ProfileRequestDto dto) {
        Long userId = AuthContextHolder.getCurrentUserId();

        if (profileRepository.existsByUserId(userId)) {
            throw new BadRequestException("Profile already exists for this user");
        }

        Profile profile = new Profile();
        profile.setUserId(userId);
        applyBasicFields(dto, profile);
        handleProfileImage(dto, profile);
        handleBannerImage(dto, profile);

        profile = profileRepository.save(profile);
        return mapEntityToResponse(profile);
    }

    @Override
    public ProfileResponseDto updateProfile(ProfileRequestDto dto) {
        Long userId = AuthContextHolder.getCurrentUserId();
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Profile not found"));

        applyBasicFields(dto, profile);
        handleProfileImage(dto, profile); // deletes old + uploads new, if a new file is sent
        handleBannerImage(dto, profile);

        profile = profileRepository.save(profile);
        return mapEntityToResponse(profile);
    }

    @Override
    public void deleteProfile() {
        Long userId = AuthContextHolder.getCurrentUserId();
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Profile not found"));

        List<String> urlsToDelete = new ArrayList<>();
        if (profile.getProfileImageUrl() != null) urlsToDelete.add(profile.getProfileImageUrl());
        if (profile.getBannerImageUrl() != null) urlsToDelete.add(profile.getBannerImageUrl());

        if (!urlsToDelete.isEmpty()) {
            uploaderServiceClient.deleteImages(new DeleteImagesRequestDto(urlsToDelete));
        }

        profileRepository.delete(profile);
    }

    @Override
    public ProfileResponseDto getCurrentUserProfile() {
        Long userId = AuthContextHolder.getCurrentUserId();
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Profile not found with id: " + userId));
        return mapEntityToResponse(profile);
    }

    @Override
    public UserDto getMyProfile() {
        Long userId = AuthContextHolder.getCurrentUserId();
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Profile not found"));

        return UserDto.builder()
                .id(userId)
                .name(profile.getName())
                .email(profile.getEmail())
                .profileUrl(profile.getProfileImageUrl())
                .build();
    }

    @Override
    public ProfileResponseDto getProfileByUserId(Long userId) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Profile not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        ProfileResponseDto dto = mapEntityToResponse(profile);
        dto.setName(user.getName());
        return dto;
    }

    // ---------- internal / inter-service ----------

    @Override
    public UserSummaryDto getUserSummary(Long userId) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Profile not found"));
        return new UserSummaryDto(userId, profile.getName(), profile.getProfileImageUrl());
    }

    @Override
    public List<UserSummaryDto> getUserSummaries(List<Long> userIds) {
        return profileRepository.findByUserIdIn(userIds).stream()
                .map(p -> new UserSummaryDto(p.getUserId(), p.getName(), p.getProfileImageUrl()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean userExists(Long userId) {
        return userRepository.existsById(userId);
    }

    // ---------- helpers ----------

    private void handleProfileImage(ProfileRequestDto dto, Profile profile) {
        if (dto.getProfileImage() != null && !dto.getProfileImage().isEmpty()) {
            String oldUrl = profile.getProfileImageUrl();
            String newUrl = uploaderServiceClient.uploadSingleImage(dto.getProfileImage());
            profile.setProfileImageUrl(newUrl);
            deleteOldImageIfExists(oldUrl); // only delete old AFTER new upload succeeds
        }
    }

    private void handleBannerImage(ProfileRequestDto dto, Profile profile) {
        if (dto.getBannerImage() != null && !dto.getBannerImage().isEmpty()) {
            String oldUrl = profile.getBannerImageUrl();
            String newUrl = uploaderServiceClient.uploadSingleImage(dto.getBannerImage());
            profile.setBannerImageUrl(newUrl);
            deleteOldImageIfExists(oldUrl);
        }
    }

    private void deleteOldImageIfExists(String oldUrl) {
        if (oldUrl != null && !oldUrl.isBlank()) {
            try {
                uploaderServiceClient.deleteImages(new DeleteImagesRequestDto(List.of(oldUrl)));
            } catch (Exception e) {
                log.error("Failed to delete old image: {}", oldUrl, e);
                // don't block update if old image delete fails
            }
        }
    }

    private void applyBasicFields(ProfileRequestDto dto, Profile profile) {
        profile.setDateOfBirth(dto.getDateOfBirth());
        profile.setAbout(dto.getAbout());
        profile.setSkills(dto.getSkills());
        profile.setCertificateUrls(dto.getCertificateUrls());

        try {
            if (dto.getSocialLinksJson() != null && !dto.getSocialLinksJson().isBlank()) {
                List<SocialLinkDto> socialLinkDtos = objectMapper.readValue(
                        dto.getSocialLinksJson(), new TypeReference<List<SocialLinkDto>>() {});
                profile.setSocialLinks(socialLinkDtos.stream()
                        .map(s -> new SocialLink(s.getPlatform(), s.getUrl()))
                        .collect(Collectors.toList()));
            }

            if (dto.getProjectsJson() != null && !dto.getProjectsJson().isBlank()) {
                List<ProjectDto> projectDtos = objectMapper.readValue(
                        dto.getProjectsJson(), new TypeReference<List<ProjectDto>>() {});
                profile.setProjects(projectDtos.stream()
                        .map(p -> new Project(p.getTitle(), p.getDescription(), p.getLink()))
                        .collect(Collectors.toList()));
            }
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Invalid socialLinks or projects JSON: " + e.getMessage());
        }
    }

    private ProfileResponseDto mapEntityToResponse(Profile profile) {
        ProfileResponseDto dto = new ProfileResponseDto();
        dto.setId(profile.getId());
        dto.setUserId(profile.getUserId());
        dto.setName(profile.getName());
        dto.setEmail(profile.getEmail());
        dto.setProfileImageUrl(profile.getProfileImageUrl());
        dto.setBannerImageUrl(profile.getBannerImageUrl());
        dto.setDateOfBirth(profile.getDateOfBirth());
        dto.setAbout(profile.getAbout());
        dto.setSkills(profile.getSkills());
        dto.setCertificateUrls(profile.getCertificateUrls());

        if (profile.getSocialLinks() != null) {
            dto.setSocialLinks(profile.getSocialLinks().stream()
                    .map(s -> new SocialLinkDto(s.getPlatform(), s.getUrl()))
                    .collect(Collectors.toList()));
        }
        if (profile.getProjects() != null) {
            dto.setProjects(profile.getProjects().stream()
                    .map(p -> new ProjectDto(p.getTitle(), p.getDescription(), p.getLink()))
                    .collect(Collectors.toList()));
        }

        dto.setCreatedAt(profile.getCreatedAt());
        dto.setUpdatedAt(profile.getUpdatedAt());
        return dto;
    }
}