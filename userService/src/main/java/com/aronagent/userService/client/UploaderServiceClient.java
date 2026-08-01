package com.aronagent.userService.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;


@FeignClient(name = "uploader-service", path = "/uploads/file")
public interface UploaderServiceClient {
    @PostMapping(value = "/single", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    String uploadSingleImage(@RequestPart("file") MultipartFile file);

    @DeleteMapping
    void deleteImages(@RequestBody com.aronagent.userService.dto.DeleteImagesRequestDto request);
}
