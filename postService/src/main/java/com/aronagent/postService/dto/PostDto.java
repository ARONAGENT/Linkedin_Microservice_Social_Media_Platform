package com.aronagent.postService.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostDto {
    private Long userId;
    private String name;
    private String profileUrl;
    private Long id;
    private String content;
    private List<String> imageUrls;
    private LocalDateTime createdAt;
}