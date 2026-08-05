package com.hotelbooking.hotelservice.service;

import com.hotelbooking.hotelservice.dto.response.HotelImageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface HotelImageService {

    List<HotelImageResponse> uploadImages(UUID hotelId, List<MultipartFile> files);
}
