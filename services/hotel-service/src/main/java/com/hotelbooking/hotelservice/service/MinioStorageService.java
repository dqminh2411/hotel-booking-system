package com.hotelbooking.hotelservice.service;

import java.io.InputStream;

public interface MinioStorageService {

    String uploadFile(String bucketName,
                      String objectName,
                      InputStream stream,
                      long size,
                      String contentType);

    void deleteFile(String bucketName, String objectName);

}
