package com.hotelbooking.hotelservice.service;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.hotelbooking.hotelservice.dto.request.HotelImageDelRequest;
import com.hotelbooking.hotelservice.dto.request.HotelUpdateStatusRequest;
import com.hotelbooking.hotelservice.dto.response.HotelPendingResponse;

public interface AdminService {
    Page<HotelPendingResponse> getListHotelPending(Pageable pageable);

    void updateHotelStatus(UUID hotelId, HotelUpdateStatusRequest request);

    void deleteHotelImages(HotelImageDelRequest request);
}
