package com.hotelbooking.bookingservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.hotelbooking.bookingservice.dto.ApiResponse;
import com.hotelbooking.bookingservice.dto.RoomCheckinRequest;

@FeignClient(name = "hotel-service", url = "http://hotel-service:5000", path = "/api/hotels")
public interface HotelServiceFiegnClient {

    @PostMapping("/rooms")
    ApiResponse<Void> updateRoomStatus(@RequestBody RoomCheckinRequest roomCheckinRequest);
}
