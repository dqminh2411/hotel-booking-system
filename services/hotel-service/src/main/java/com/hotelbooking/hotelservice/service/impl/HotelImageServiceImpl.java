package com.hotelbooking.hotelservice.service.impl;

import com.hotelbooking.hotelservice.dto.response.HotelImageResponse;
import com.hotelbooking.hotelservice.service.HotelImageService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public class HotelImageServiceImpl implements HotelImageService {

    @Override
    public List<HotelImageResponse> uploadImages(UUID hotelId, List<MultipartFile> files) {
        return List.of();
    }
}

