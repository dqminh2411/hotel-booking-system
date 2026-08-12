package com.hotelbooking.hotelservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomTypeCreatedResponse {
    private UUID id;
    private String name;
    private String description;
    private Integer maxGuests;
    private Integer bedCounts;
    private BigDecimal basePricePerNight;
    private Integer quantity;
    private Integer area;
}
