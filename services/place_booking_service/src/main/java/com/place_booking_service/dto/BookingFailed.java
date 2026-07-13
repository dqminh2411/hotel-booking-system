package com.place_booking_service.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingFailed {
    @NotNull
    private UUID sagaId;
    @NotBlank private String eventType;
    private BookingInfo booking;
    private String reason;




}
