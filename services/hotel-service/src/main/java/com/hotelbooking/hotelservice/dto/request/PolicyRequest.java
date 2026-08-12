package com.hotelbooking.hotelservice.dto.request;

import com.hotelbooking.hotelservice.constant.PolicyType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyRequest {

    @NotNull(message = "type không được để trống")
    private PolicyType type;

    @Size(max = 1000, message = "description tối đa 1000 ký tự")
    private String description;
}
