package com.hotelbooking.hotelservice.dto;


import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class HotelSearchItemDTO {

    private UUID hotelId;

    private String name;

    private String coverImageUrl;

    private AddressDTO address;

    private List<PolicyDTO> policies;

    private CheapestRoomTypeDTO cheapestRoomType;

}
