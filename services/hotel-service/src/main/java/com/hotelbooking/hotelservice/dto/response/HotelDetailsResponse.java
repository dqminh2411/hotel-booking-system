package com.hotelbooking.hotelservice.dto.response;

import java.util.List;
import java.util.UUID;

import com.hotelbooking.hotelservice.dto.other.HotelAmenity;
import com.hotelbooking.hotelservice.dto.other.HotelPolicy;
import com.hotelbooking.hotelservice.dto.other.Image;

public record HotelDetailsResponse(
        UUID hotelId,
        String name,
        String description,
        Address address,
        List<Image> imageUrls,
        List<HotelPolicy> policies,
        List<HotelAmenity> amenities,
        List<RoomTypeResponse> availableRoomTypes,
        String status
) {
}

