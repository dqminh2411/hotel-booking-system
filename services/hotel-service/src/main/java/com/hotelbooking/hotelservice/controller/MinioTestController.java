package com.hotelbooking.hotelservice.controller;

import com.hotelbooking.hotelservice.service.MinioStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
@RequestMapping("api/test/minio")
@RequiredArgsConstructor
public class MinioTestController {

    private final MinioStorageService minioStorageService;

    @PostMapping("/upload")
    public String upload(@RequestParam("file")MultipartFile file) throws IOException {
        return minioStorageService.uploadFile(
                "hotel-images",
                file.getOriginalFilename(),
                file.getInputStream(),
                file.getSize(),
                file.getContentType()
        );
    }

    @DeleteMapping("/delete")
    public void delete(@RequestParam String objectName) {
        minioStorageService.deleteFile("hotel-images", objectName);
    }
}
