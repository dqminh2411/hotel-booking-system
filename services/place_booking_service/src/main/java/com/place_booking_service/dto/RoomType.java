package com.place_booking_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomType {
    @NotBlank
    private String roomTypeId;
    private String name;
    private int bedCount;
    @Min(1)
    private int bookingQuantity;
    private int totalQuantity;
    private double price;

}
