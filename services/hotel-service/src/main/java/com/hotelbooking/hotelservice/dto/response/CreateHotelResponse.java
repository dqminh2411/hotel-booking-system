package com.hotelbooking.hotelservice.dto.response;

import com.hotelbooking.hotelservice.constant.HotelStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateHotelResponse {
    private UUID id;
    private UUID tenantId;
    private String name;
    private String description;
    private String address;
    private String provinceCode;
    private String districtCode;
    private String wardCode;
    private HotelStatus status;
    private Instant createdAt;
}
