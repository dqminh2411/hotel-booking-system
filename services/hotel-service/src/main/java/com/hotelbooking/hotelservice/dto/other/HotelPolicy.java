package com.hotelbooking.hotelservice.dto.other;

import java.util.UUID;

import com.hotelbooking.hotelservice.constant.PolicyType;

public record HotelPolicy(
    UUID id,
    PolicyType type,
    String description
) {

}
