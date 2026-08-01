package com.aronagent.postService.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class PostCreateRequestDto {

    private List<MultipartFile> files;
    private String content;
}
