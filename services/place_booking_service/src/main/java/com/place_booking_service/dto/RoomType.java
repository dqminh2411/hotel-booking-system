package com.place_booking_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomType {
    @NotNull
    private UUID roomTypeId;
    private String name;
    private int bedCount;
    @Min(1)
    private int bookingQuantity;
    private int totalQuantity;
    private BigDecimal price;

}
