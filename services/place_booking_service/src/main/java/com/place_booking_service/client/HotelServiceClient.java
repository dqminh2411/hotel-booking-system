package com.place_booking_service.client;

import com.place_booking_service.dto.Hotel;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.place_booking_service.dto.HotelAndRoomTypesResponse;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import java.util.UUID;

@FeignClient(name = "hotel-service")
public interface HotelServiceClient {

    @GetMapping("/api/hotels/{id}")
    public Hotel getHotelById(@PathVariable("id") UUID id);

    @GetMapping("/api/hotels/{hotelId}/requested-room-types")
    public HotelAndRoomTypesResponse getHotelAndRequestedRoomTypes(
            @PathVariable("hotelId") UUID hotelId,
            @RequestParam(required = false, name = "roomTypeList") List<UUID> roomTypeList
    );
}
