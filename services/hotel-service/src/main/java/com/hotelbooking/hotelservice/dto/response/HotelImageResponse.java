package com.hotelbooking.hotelservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotelImageResponse {
    private UUID imageId;
    private String url;
    private Boolean isCover;
}
