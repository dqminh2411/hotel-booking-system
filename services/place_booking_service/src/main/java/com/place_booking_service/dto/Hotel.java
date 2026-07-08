package com.place_booking_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Hotel {

    private UUID hotelId;
    @NotBlank
    private String name;
    private String address;


}
