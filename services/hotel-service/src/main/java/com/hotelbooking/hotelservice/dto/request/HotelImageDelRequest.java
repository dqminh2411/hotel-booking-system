package com.hotelbooking.hotelservice.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record HotelImageDelRequest(
    @NotNull(message = "hotelId không được để trống")
    UUID hotelId,

    @NotEmpty(message = "Danh sách ảnh cần xóa không được rỗng")
    List<@NotNull(message = "imageId không được để trống") UUID> imgIds
) {

}
