package com.hotelbooking.hotelservice.dto.response;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Hotel {

    private String hotelId;
    @NotBlank
    private String name;
    private String address;


}
