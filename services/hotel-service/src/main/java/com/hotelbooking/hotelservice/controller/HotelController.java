package com.hotelbooking.hotelservice.controller;

import com.hotelbooking.hotelservice.dto.HotelSearchItemDTO;
import com.hotelbooking.hotelservice.dto.request.CreateHotelRequest;
import com.hotelbooking.hotelservice.dto.request.HotelSearchRequest;
import com.hotelbooking.hotelservice.dto.response.ApiResponse;
import com.hotelbooking.hotelservice.dto.response.HotelDetailsResponse;
import com.hotelbooking.hotelservice.dto.request.RoomCheckinRequest;
import com.hotelbooking.hotelservice.service.HotelService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.hotelbooking.hotelservice.dto.response.*;

@RestController
@RequestMapping("/api/hotels")
@Validated
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HotelController {

    HotelService hotelService;

    @GetMapping("/{hotelId}")
    public ApiResponse<HotelDetailsResponse> getHotelById(
            @PathVariable UUID hotelId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkinDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkoutDate,
            @RequestParam(required = false) @Min(value = 1, message = "guestNum phải lớn hơn 0") Integer guestNum,

            @RequestParam(required = false) @Min(value = 1, message = "roomNum phải lớn hơn 0") Integer roomNum) {
        return ApiResponse.<HotelDetailsResponse>builder()
                .code(200)
                .message("Lấy thành công thông tin khách sạn: " + hotelId.toString())
                .data(hotelService.getHotelDetail(hotelId, checkinDate, checkoutDate, guestNum, roomNum))
                .build();
    }

    @GetMapping("/{hotelId}/room-types")
    public ApiResponse<List<RoomTypeResponse>> getListRoomTypeByHotelId(
            @PathVariable UUID hotelId) {
        return ApiResponse.<List<RoomTypeResponse>>builder()
                .code(200)
                .message("Lấy thành công danh sách các loại phòng của khách sạn: " + hotelId.toString())
                .data(hotelService.getListRoomTypeByHotelId(hotelId))
                .build();
    }

    @GetMapping
    public PagedResponse<HotelSearchItemDTO> searchHotels(
            @Valid @ModelAttribute HotelSearchRequest request) {

        return hotelService.search(request);
    }

    @GetMapping("/{hotelId}/requested-room-types")
    public HotelAndRoomTypesResponse getHotelAndRequestedRoomTypes(
            @PathVariable UUID hotelId,
            @RequestParam(required = false, name = "roomTypeList") List<UUID> roomTypeList) {
        return hotelService.getRequestedRoomTypesByHotel(hotelId, roomTypeList);
    }

    @PostMapping("/rooms")
    public ApiResponse<Void> updateRoomStatus(@Valid @RequestBody RoomCheckinRequest request) {
        hotelService.updateRoomStatus(request);
        return ApiResponse.<Void>builder()
                .code(200)
                .message("Cập nhật trạng thái các phòng thành công")
                .build();
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CreateHotelResponse>> createHotel(
            @Valid @RequestBody CreateHotelRequest request) {
        CreateHotelResponse response = hotelService.createHotel(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<CreateHotelResponse>builder()
                        .code(HttpStatus.CREATED.value())
                        .message("Tạo khách sạn thành công, đang chờ admin duyệt")
                        .data(response)
                        .build());
    }
}
