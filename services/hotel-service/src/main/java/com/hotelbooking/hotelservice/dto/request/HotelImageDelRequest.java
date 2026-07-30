package com.hotelbooking.hotelservice.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record HotelImageDelRequest(
    @NotNull(message = "hotelId không được để trống")
    UUID hotelId,

    @NotEmpty(message = "Danh sách ảnh cần xóa không được rỗng")
    List<@NotBlank(message = "URL ảnh không được là chuỗi rỗng hoặc khoảng trắng") String> imgUrls
) {

}
