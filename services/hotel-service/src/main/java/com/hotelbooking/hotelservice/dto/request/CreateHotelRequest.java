package com.hotelbooking.hotelservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateHotelRequest {

    @NotBlank(message = "Tên khách sạn không được để trống")
    @Size(max = 255, message = "Tên khách sạn tối đa 255 ký tự")
    private String name;

    @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
    private String description;

    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(max = 500, message = "Địa chỉ tối đa 500 ký tự")
    private String address;

    @NotBlank(message = "provinceCode không được để trống")
    @Pattern(regexp = "^\\d{2}$", message = "provinceCode phải gồm đúng 2 chữ số")
    private String provinceCode;

    @NotBlank(message = "districtCode không được để trống")
    @Pattern(regexp = "^\\d{3}$", message = "districtCode phải gồm đúng 3 chữ số")
    private String districtCode;

    @NotBlank(message = "wardCode không được để trống")
    @Pattern(regexp = "^\\d{5}$", message = "wardCode phải gồm đúng 5 chữ số")
    private String wardCode;

    @Valid
    private List<AmenityRequest> amenities;

    @Valid
    private List<PolicyRequest> policies;

    @Valid
    @NotBlank(message = "Phải có it nhất 1 loại phòng (roomTypes)")
    private  List<RoomTypeRequest> roomTypes;
}
