package com.hotelbooking.hotelservice.dto;


import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class CheapestRoomTypeDTO {
    private UUID roomTypeId;

    private String name;

    private BigDecimal pricePerNight;

    private int availableRooms;

}


