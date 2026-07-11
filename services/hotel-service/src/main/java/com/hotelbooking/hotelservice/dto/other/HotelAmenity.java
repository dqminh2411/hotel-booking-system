package com.hotelbooking.hotelservice.dto.other;

import java.util.UUID;

import com.hotelbooking.hotelservice.constant.ScopeType;

public record HotelAmenity(
    UUID id,
    String name,
    ScopeType scope
) {

}
