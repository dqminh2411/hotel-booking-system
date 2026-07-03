package com.hotelbooking.hotelservice.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.hotelbooking.hotelservice.dto.other.HotelAmenity;
import com.hotelbooking.hotelservice.dto.other.Image;

public record RoomTypeDetailResponse(
    UUID roomTypeId,
    UUID hotelId,
    String name,
    String description,
    BigDecimal basePricePerNight,
    Integer maxGuests,
    Integer bedCounts,
    Integer area,
    Integer totalRooms,
    List<HotelAmenity> amenities,
    List<Image> images
) {

}
