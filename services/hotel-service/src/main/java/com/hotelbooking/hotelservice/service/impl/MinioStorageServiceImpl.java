package com.hotelbooking.hotelservice.service.impl;

import com.hotelbooking.hotelservice.service.MinioStorageService;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioStorageServiceImpl implements MinioStorageService {

    private final MinioClient minioClient;

    @Value("${minio.endpoint}")
    private String endpoint;

    @Override
    public String uploadFile(String bucketName, String objectName, InputStream stream, long size, String contentType) {

        try {
            ensureBucket(bucketName);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(stream, size, -1)
                            .contentType(contentType)
                            .build()
            );

            return endpoint + "/" + bucketName + "/" + objectName;
        } catch (Exception e) {
            log.error("Upload file to MinIO failed: bucket={}, object={}", bucketName, objectName, e);
            throw new RuntimeException("Upload file thất bại: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(String bucketName, String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch(Exception e) {
            log.error("Delete file from MinIO failed: bucket={}, object={}", bucketName, objectName, e);
            throw new RuntimeException("Xóa file thất bại: " + e.getMessage(), e);
        }
    }

    private void ensureBucket(String bucketName) throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucketName).build());

        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }

        // Luôn set lại policy, kể cả khi bucket đã tồn tại từ trước
        // (ví dụ ai đó tạo tay qua console mà quên set public-read)
        String policy = """
        {
          "Version": "2012-10-17",
          "Statement": [
            {
              "Effect": "Allow",
              "Principal": {"AWS": ["*"]},
              "Action": ["s3:GetBucketLocation"],
              "Resource": ["arn:aws:s3:::%s"]
            },
            {
              "Effect": "Allow",
              "Principal": {"AWS": ["*"]},
              "Action": ["s3:ListBucket"],
              "Resource": ["arn:aws:s3:::%s"]
            },
            {
              "Effect": "Allow",
              "Principal": {"AWS": ["*"]},
              "Action": ["s3:GetObject"],
              "Resource": ["arn:aws:s3:::%s/*"]
            }
          ]
        }
        """.formatted(bucketName, bucketName, bucketName);

        minioClient.setBucketPolicy(
                SetBucketPolicyArgs.builder().bucket(bucketName).config(policy).build());
    }
}
