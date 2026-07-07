package com.hotelbooking.hotelservice.dto.response;

import com.hotelbooking.hotelservice.dto.HotelSearchItemDTO;
import lombok.*;

import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class HotelSearchResponse {

    private List<HotelSearchItemDTO> data;
    private int total;
    private int page;
    private int size;

}
