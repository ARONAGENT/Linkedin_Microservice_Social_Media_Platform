package com.aronagent.postService.client;

import com.aronagent.postService.config.FeignMultipartConfig;
import com.aronagent.postService.dto.DeleteImagesRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@FeignClient(name = "uploader-service", path = "/uploads/file",configuration = FeignMultipartConfig.class)
public interface UploaderServiceClient {

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    List<String> uploadMultipleImages(@RequestPart("files") List<MultipartFile> files);

    @DeleteMapping
    void deleteImages(@RequestBody DeleteImagesRequestDto request);
}