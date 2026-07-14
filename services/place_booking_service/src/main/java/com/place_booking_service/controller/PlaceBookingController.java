package com.place_booking_service.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.place_booking_service.client.BookingServiceClient;
import com.place_booking_service.client.HotelServiceClient;
import com.place_booking_service.client.UserServiceClient;
import com.place_booking_service.dto.CountBookingsResponse;
import com.place_booking_service.dto.DuplicateRequestResult;
import com.place_booking_service.dto.ActiveBookingRoomType;
import com.place_booking_service.dto.ApiResponse;
import com.place_booking_service.dto.HotelAndRoomTypesResponse;
import com.place_booking_service.dto.PlaceBookingRequest;
import com.place_booking_service.dto.RoomTypeQuantityResponse;
import com.place_booking_service.dto.User;
import com.place_booking_service.exception.HotelNotFoundException;
import com.place_booking_service.exception.InvalidBookingRequestException;
import com.place_booking_service.exception.RoomTypeNotAvailableException;
import com.place_booking_service.exception.RoomTypeNotFoundException;
import com.place_booking_service.exception.UserNotFoundException;
import com.place_booking_service.helper.Helpler;
import com.place_booking_service.service.PlaceBookingService;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@RestController
@RequestMapping({ "/place-booking", "" })
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PlaceBookingController {

    PlaceBookingService placeBookingService;
    UserServiceClient userServiceClient;
    HotelServiceClient hotelServiceClient;
    BookingServiceClient bookingServiceClient;
    Helpler helpler;

    @PostMapping
    public ApiResponse<?> placeBooking(@Valid @RequestBody PlaceBookingRequest placeBookingRequest) {

        // kiểm tra request trước đó trong redis đã tồn tại chưa
        String hashRequest = helpler.toStringPlaceBookingRequest(placeBookingRequest);
        DuplicateRequestResult result = helpler.isDuplicateRequest(
                placeBookingRequest.getUserId(),
                hashRequest,
                placeBookingRequest.getForceToken()
        );

        // nếu trùng thì cảnh báo 
        if (result.isDuplicated()) {
            String expectedForceToken = helpler.generateForceToken(placeBookingRequest.getUserId(), hashRequest);
            return ApiResponse.<Map<String, Object>>builder()
                    .code(409)
                    .message("Bạn đang có một đơn đặt phòng tương tự trong vòng 5 phút trước")
                    .data(Map.of(
                            "forceToken", expectedForceToken,
                            "hint", "Để tiếp tục tạo đơn, hãy gửi lại yêu cầu sau nút xác nhận này"))
                    .build();
        }

        UUID bookingId = UUID.randomUUID();

        // Bước 1: Kiểm tra người dùng đặt phòng có tồn tại trong user-service.
        User user = userServiceClient.getUserById(placeBookingRequest.getUserId());
        if (user == null) {
            throw new UserNotFoundException(placeBookingRequest.getUserId().toString());
        }

        // Bước 2: Lấy thông tin khách sạn và các loại phòng được yêu cầu từ hotel-service.
        HotelAndRoomTypesResponse hotelAndRoomTypes = hotelServiceClient.getHotelAndRequestedRoomTypes(
                placeBookingRequest.getHotelId(),
                placeBookingRequest.getRoomTypeList().stream()
                        .map(roomType -> roomType.getRoomTypeId())
                        .toList());

        if (hotelAndRoomTypes == null) {
            throw new HotelNotFoundException(placeBookingRequest.getHotelId().toString());
        }

        // Bước 3: Kiểm tra khoảng ngày checkin/checkout hợp lệ.
        LocalDate checkin = LocalDate.parse(placeBookingRequest.getCheckin());
        LocalDate checkout = LocalDate.parse(placeBookingRequest.getCheckout());

        if (!checkin.isBefore(checkout) || checkin.isBefore(LocalDate.now())) {
            throw new InvalidBookingRequestException(
                    "checkin must be before checkout and must not be in the past");
        }

        List<RoomTypeQuantityResponse> roomTypes = hotelAndRoomTypes.roomTypes() == null
                ? List.of()
                : hotelAndRoomTypes.roomTypes();

        Map<UUID, Long> roomTypesTotalQuantity = roomTypes.stream()
                .collect(Collectors.toMap(RoomTypeQuantityResponse::roomTypeId,
                        RoomTypeQuantityResponse::totalQuantity));

        // Bước 4: Lấy số lượng phòng đã được đặt trong khoảng ngày từ booking-service.
        CountBookingsResponse countBookingsResponse = bookingServiceClient.countBookings(
                placeBookingRequest.getHotelId(),
                placeBookingRequest.getRoomTypeList().stream()
                        .map(roomType -> roomType.getRoomTypeId())
                        .toList(),
                checkin,
                checkout);

        List<ActiveBookingRoomType> activeBookingRoomTypes = countBookingsResponse == null
                || countBookingsResponse.activeBookingCount() == null
                        ? List.of()
                        : countBookingsResponse.activeBookingCount();

        Map<UUID, Long> activeBookingCounts = activeBookingRoomTypes.stream()
                .collect(Collectors.toMap(
                        activeBookingRoomType -> activeBookingRoomType.roomTypeId(),
                        activeBookingRoomType -> activeBookingRoomType.bookingCount()));

        // Bước 5: Đối chiếu số lượng phòng yêu cầu với tổng phòng và số phòng đã đặt.
        placeBookingRequest.getRoomTypeList().forEach(roomType -> {
            Long totalQuantity = roomTypesTotalQuantity.get(roomType.getRoomTypeId());
            if (totalQuantity == null) {
                throw new RoomTypeNotFoundException(roomType.getRoomTypeId());
            }
            roomType.setTotalQuantity(Math.toIntExact(totalQuantity));

            long activeBookingCount = activeBookingCounts.getOrDefault(roomType.getRoomTypeId(), 0L);
            if (activeBookingCount + roomType.getBookingQuantity() > totalQuantity) {
                throw new RoomTypeNotAvailableException(roomType.getRoomTypeId());
            }
        });

        // Bước 6: Khởi tạo saga đặt phòng và ghi command vào outbox để xử lý bất đồng bộ.
        placeBookingService.startSaga(
                placeBookingRequest,
                user,
                hotelAndRoomTypes.hotel(),
                bookingId,
                hashRequest);

        return ApiResponse.<Map<String, Object>>builder()
                .code(200)
                .message("OK")
                .data(Map.of(
                        "bookingId", bookingId,
                        "status", "PENDING",
                        "message", "Booking request is being processed."))
                .build();
    }
}
