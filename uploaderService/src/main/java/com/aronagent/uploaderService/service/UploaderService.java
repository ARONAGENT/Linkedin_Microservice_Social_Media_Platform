package com.aronagent.uploaderService.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UploaderService {

    List<String> upload(List<MultipartFile> file);
    void delete(List<String> imageUrls);
    String uploadSingle(MultipartFile file); // new

    // new


}
