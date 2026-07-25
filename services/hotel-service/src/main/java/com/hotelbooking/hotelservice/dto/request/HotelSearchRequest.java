package com.hotelbooking.hotelservice.dto.request;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class HotelSearchRequest {

    @Pattern(regexp = "^\\d{2}$|^\\d{3}$|^\\d{5}$")
    private String locationCode;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkinDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkoutDate;

    @Min(1)
    private int guestNum;

    @Min(1)
    private int roomNum;

    @PositiveOrZero
    private BigDecimal minPrice;

    @PositiveOrZero
    private BigDecimal maxPrice;

    private List<UUID> amenities;

    private String sortBy;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 10;

}
