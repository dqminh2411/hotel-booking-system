package com.place_booking_service.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.place_booking_service.client.BookingServiceClient;
import com.place_booking_service.client.HotelServiceClient;
import com.place_booking_service.client.UserServiceClient;
import com.place_booking_service.dto.CountBookingsResponse;
import com.place_booking_service.dto.ActiveBookingRoomType;
import com.place_booking_service.dto.Hotel;
import com.place_booking_service.dto.HotelAndRoomTypesResponse;
import com.place_booking_service.dto.PlaceBookingRequest;
import com.place_booking_service.dto.RoomTypeQuantityResponse;
import com.place_booking_service.dto.User;
import com.place_booking_service.exception.HotelNotFoundException;
import com.place_booking_service.exception.InvalidBookingRequestException;
import com.place_booking_service.exception.RoomTypeNotAvailableException;
import com.place_booking_service.exception.RoomTypeNotFoundException;
import com.place_booking_service.exception.UserNotFoundException;
import com.place_booking_service.service.PlaceBookingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping({"/place-booking", ""})
public class PlaceBookingController {

    @Autowired
    private PlaceBookingService placeBookingService;

    @Autowired
    private UserServiceClient userServiceClient;

    @Autowired
    private HotelServiceClient hotelServiceClient;

    @Autowired
    private BookingServiceClient bookingServiceClient;

    @PostMapping
    public ResponseEntity<?> placeBooking(@Valid @RequestBody PlaceBookingRequest placeBookingRequest) {
        // Bước 1: Kiểm tra người dùng đặt phòng có tồn tại trong user-service.
        User user = userServiceClient.getUserById(placeBookingRequest.getUserId());
        if (user == null) {
            throw new UserNotFoundException(placeBookingRequest.getUserId());
        }

        // Bước 2: Lấy thông tin khách sạn và các loại phòng được yêu cầu từ hotel-service.
        HotelAndRoomTypesResponse hotelAndRoomTypes = hotelServiceClient.getHotelAndRequestedRoomTypes(
            placeBookingRequest.getHotelId(),
            placeBookingRequest.getRoomTypeList().stream()
                .map(roomType -> roomType.getRoomTypeId())
                .toList()
        );
        if (hotelAndRoomTypes == null) {
            throw new HotelNotFoundException(placeBookingRequest.getHotelId());
        }

        // Bước 3: Kiểm tra khoảng ngày checkin/checkout hợp lệ.
        LocalDate checkin = LocalDate.parse(placeBookingRequest.getCheckin());
        LocalDate checkout = LocalDate.parse(placeBookingRequest.getCheckout());

        if (!checkin.isBefore(checkout) || checkin.isBefore(LocalDate.now())) {
            throw new InvalidBookingRequestException(
                "checkin must be before checkout and must not be in the past"
            );
        }

        List<RoomTypeQuantityResponse> roomTypes = hotelAndRoomTypes.roomTypes() == null
            ? List.of()
            : hotelAndRoomTypes.roomTypes();

        Map<String, Long> roomTypesTotalQuantity = roomTypes.stream()
            .collect(Collectors.toMap(RoomTypeQuantityResponse::roomTypeId, RoomTypeQuantityResponse::totalQuantity));

        // Bước 4: Lấy số lượng phòng đã được đặt trong khoảng ngày từ booking-service.
        CountBookingsResponse countBookingsResponse = bookingServiceClient.countBookings(
            placeBookingRequest.getHotelId(),
            placeBookingRequest.getRoomTypeList().stream()
                .map(roomType -> roomType.getRoomTypeId())
                .toList(),
            checkin,
            checkout
        );

        List<ActiveBookingRoomType> activeBookingRoomTypes =
            countBookingsResponse == null || countBookingsResponse.activeBookingCount() == null
                ? List.of()
                : countBookingsResponse.activeBookingCount();

        Map<String, Long> activeBookingCounts = activeBookingRoomTypes.stream()
            .collect(Collectors.toMap(
                activeBookingRoomType -> activeBookingRoomType.roomTypeId(),
                activeBookingRoomType -> activeBookingRoomType.bookingCount()
            ));

        // Bước 5: Đối chiếu số lượng phòng yêu cầu với tổng số phòng và số phòng đã đặt.
        placeBookingRequest.getRoomTypeList().forEach(roomType -> {
            Long totalQuantity = roomTypesTotalQuantity.get(roomType.getRoomTypeId());
            if (totalQuantity == null) {
                throw new RoomTypeNotFoundException(roomType.getRoomTypeId());
            }

            long activeBookingCount = activeBookingCounts.getOrDefault(roomType.getRoomTypeId(), 0L);
            if (activeBookingCount + roomType.getBookingQuantity() > totalQuantity) {
                throw new RoomTypeNotAvailableException(roomType.getRoomTypeId());
            }
        });

        // Bước 6: Khởi tạo saga đặt phòng và ghi command vào outbox để xử lý bất đồng bộ.
        String bookingId = placeBookingService.startSaga(placeBookingRequest, user, hotelAndRoomTypes.hotel());

        // Bước 7: Trả bookingId cho client để polling trạng thái booking.
        return ResponseEntity.accepted().body(Map.of(
            "bookingId", bookingId,
            "status", "PENDING",
            "message", "Booking request is being processed. Please check status with bookingId.",
            "pollingUrl", "/bookings/" + bookingId
        ));
    }
}
