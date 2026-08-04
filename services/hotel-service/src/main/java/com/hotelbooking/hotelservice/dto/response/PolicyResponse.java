package com.hotelbooking.hotelservice.dto.response;

import com.hotelbooking.hotelservice.constant.PolicyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyResponse {
    private UUID id;
    private PolicyType type;
    private String description;
}
