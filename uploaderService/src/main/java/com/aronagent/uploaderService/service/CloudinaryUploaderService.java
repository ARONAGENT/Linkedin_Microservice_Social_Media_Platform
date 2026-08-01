package com.aronagent.uploaderService.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class CloudinaryUploaderService implements UploaderService{

    private final Cloudinary cloudinary;

    @Override
    public List<String> upload(List<MultipartFile> files) {
        List<String> imageUrls = new ArrayList<>();

        try {
            for (MultipartFile file : files) {

                Map<?, ?> uploadResult = cloudinary.uploader()
                        .upload(file.getBytes(), Map.of());
                imageUrls.add(uploadResult.get("secure_url").toString());
            }
            return imageUrls;

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload images", e);
        }

    }


    @Override
    public String uploadSingle(MultipartFile file) {
        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), Map.of());
            return uploadResult.get("secure_url").toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) return;

        for (String url : imageUrls) {
            try {
                String publicId = extractPublicId(url);
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                log.info("Deleted image from cloudinary: {}", publicId);
            } catch (Exception e) {
                // don't let one failed delete break the whole loop
                log.error("Failed to delete image: {}", url, e);
            }
        }
    }


    // extracts public_id from a cloudinary secure_url
    // e.g. https://res.cloudinary.com/<cloud>/image/upload/v123456/folder/name.jpg
    // -> folder/name
    private String extractPublicId(String imageUrl) {
        String afterUpload = imageUrl.split("/upload/")[1];       // v123456/folder/name.jpg
        String withoutVersion = afterUpload.replaceFirst("^v\\d+/", ""); // folder/name.jpg
        int lastDot = withoutVersion.lastIndexOf(".");
        return lastDot != -1 ? withoutVersion.substring(0, lastDot) : withoutVersion;
    }
}
