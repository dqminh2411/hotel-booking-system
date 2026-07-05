package com.hotelbooking.hotelservice.dto.other;

import java.util.UUID;

public record Image(
    UUID id,
    String url,
    boolean isCover
) {

}
