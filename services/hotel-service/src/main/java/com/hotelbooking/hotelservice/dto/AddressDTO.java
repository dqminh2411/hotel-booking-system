package com.hotelbooking.hotelservice.dto;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class AddressDTO {

    private String fullAddress;

    private LocationRefDTO province;
    private LocationRefDTO distinct;
    private LocationRefDTO ward;
}
