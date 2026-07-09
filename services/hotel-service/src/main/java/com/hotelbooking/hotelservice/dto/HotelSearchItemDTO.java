package com.hotelbooking.hotelservice.dto;


import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class HotelSearchItemDTO {

    private String hotelId;

    private String name;

    private String coverImageUrl;

    private AddressDTO address;

    private List<PolicyDTO> policies;

    private CheapestRoomTypeDTO cheapestRoomType;

}
