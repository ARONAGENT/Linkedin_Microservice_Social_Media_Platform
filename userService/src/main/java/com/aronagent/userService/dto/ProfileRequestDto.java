package com.aronagent.userService.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Data
public class ProfileRequestDto {

    private MultipartFile profileImage; // optional
    private MultipartFile bannerImage;  // optional

    private LocalDate dateOfBirth;
    private String about;

    private List<String> skills;            // plain repeated form fields — works fine
    private List<String> certificateUrls;    // google docs links — plain text, works fine

    private String socialLinksJson; // frontend sends JSON.stringify([{platform, url}, ...])
    private String projectsJson;    // frontend sends JSON.stringify([{title, description, link}, ...])
}