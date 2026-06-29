package com.place_booking_service.client;

import com.place_booking_service.dto.Hotel;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.place_booking_service.dto.HotelAndRoomTypesResponse;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@FeignClient(name = "hotel-service")
public interface HotelServiceClient {

    @GetMapping("/hotels/{id}")
    public Hotel getHotelById(@PathVariable("id") String id);

    @GetMapping("/hotels/{hotelId}/requested-room-types")
    public HotelAndRoomTypesResponse getHotelAndRequestedRoomTypes(
            @PathVariable("hotelId") String hotelId,
            @RequestParam(required = false, name = "roomTypeList") List<String> roomTypeList
    );
}
