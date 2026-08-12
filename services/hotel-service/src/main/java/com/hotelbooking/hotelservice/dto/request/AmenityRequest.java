package com.hotelbooking.hotelservice.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * - id != null  -> dùng amenity có sẵn trong hệ thống (id lấy từ danh sách
 *   GET /api/amenities mà frontend load để hiển thị lên form, KHÔNG hiển thị
 *   id lên UI, chỉ hiển thị tên).
 * - id == null  -> chủ khách sạn thêm amenity mới, bắt buộc có "name".
 *   Server sẽ kiểm tra trùng tên (không phân biệt hoa/thường) trước khi tạo mới.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmenityRequest {

    private UUID id;

    @Size(max = 255, message = "Tên tiện ích tối đa 255 ký tự")
    private String name;
}
