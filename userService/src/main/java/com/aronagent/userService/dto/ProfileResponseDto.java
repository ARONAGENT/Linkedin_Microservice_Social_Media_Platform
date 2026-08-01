package com.aronagent.userService.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProfileResponseDto {
    private Long id;
    private Long userId;
    private String name;
    private String email;
    private String profileImageUrl;
    private String bannerImageUrl;    // like LinkedIn cover/banner
    private LocalDate dateOfBirth;
    private String about;
    private List<SocialLinkDto> socialLinks;
    private List<String> skills;
    private List<ProjectDto> projects;
    private List<String> certificateUrls;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}