package com.hotelbooking.hotelservice.dto;

import com.hotelbooking.hotelservice.enums.PolicyType;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class PolicyDTO {

    private UUID id;

    private PolicyType type;

    private String description;
}
