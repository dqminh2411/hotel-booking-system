package com.hotelbooking.hotelservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomTypeRequest {

    @NotBlank(message = "Tên loại phòng không được để trống")
    @Size(max = 255, message = "Tên loại phòng tối đa 255 ký tự")
    private String name;

    @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
    private String description;

    @NotNull(message = "maxGuests không được để trống")
    @Min(value = 1, message = "maxGuests phải >= 1")
    private Integer maxGuests;

    @NotNull(message = "bedCounts không được để trống")
    @Min(value = 1, message = "bedCounts phải >= 1")
    private Integer bedCounts;

    @NotNull(message = "basePricePerNight không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "basePricePerNight phải > 0")
    private BigDecimal basePricePerNight;

    @NotNull(message = "quantity không được để trống")
    @Min(value = 1, message = "quantity phải >= 1")
    private Integer quantity;

    @Min(value = 1, message = "area phải >= 1")
    private Integer area;
}
