package com.hotelbooking.hotelservice.dto.response;

import com.hotelbooking.hotelservice.dto.other.District;
import com.hotelbooking.hotelservice.dto.other.Province;
import com.hotelbooking.hotelservice.dto.other.Ward;

public record Address(
    String fullAddress,
    Province province,
    District district,
    Ward ward
) {

}
