package com.aronagent.uploaderService.controlller;

import com.aronagent.uploaderService.dto.DeleteImagesRequestDto;
import com.aronagent.uploaderService.service.UploaderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/file")
public class UploaderController {

    private final UploaderService uploaderService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<String>> uploadMultipleImages(@RequestPart("files") List<MultipartFile> files){
        return ResponseEntity.ok(uploaderService.upload(files));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteImages(@RequestBody DeleteImagesRequestDto request) {
        uploaderService.delete(request.getImageUrls());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/single", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadSingleImage(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(uploaderService.uploadSingle(file));
    }
}
