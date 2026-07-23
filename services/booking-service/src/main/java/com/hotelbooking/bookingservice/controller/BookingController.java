package com.hotelbooking.bookingservice.controller;

import com.hotelbooking.bookingservice.dto.ApiResponse;
import com.hotelbooking.bookingservice.dto.BookingCheckinInfo;
import com.hotelbooking.bookingservice.dto.BookingResponse;
import com.hotelbooking.bookingservice.dto.CheckinRequest;
import com.hotelbooking.bookingservice.dto.CountBookingsResponse;
import com.hotelbooking.bookingservice.dto.UpdateBookingStatusRequest;
import com.hotelbooking.bookingservice.enums.BookingStatus;
import com.hotelbooking.bookingservice.service.BookingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping({"/bookings", ""})
public class BookingController {
    private final BookingService bookingService;

    @GetMapping("/count")
    public CountBookingsResponse countBookings(
        @RequestParam(required = false) UUID hotelId,
        @RequestParam(required = false) List<UUID> roomTypeList,
        @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkin,
        @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkout
    ) {
        return bookingService.countBookings(hotelId, roomTypeList, checkin, checkout);
    }

    @GetMapping("/{bookingId}")
    public BookingResponse getBookingById(@PathVariable UUID bookingId) {
        return bookingService.getBookingById(bookingId);
    }

    @PatchMapping("/{bookingId}/status")
    public BookingResponse updateBookingStatus(@PathVariable UUID bookingId, @Valid @RequestBody UpdateBookingStatusRequest request) {
        return bookingService.updateBookingStatus(bookingId, request.status());
    }

    @PreAuthorize("hasRole('HOTEL_STAFF')")
    @PatchMapping("/checkin")
    public ApiResponse<Void> checkinBooking(@RequestBody CheckinRequest checkinRequest){
        bookingService.checkin(checkinRequest);
        return ApiResponse.<Void>builder()
                            .code(200)
                            .message("Checkin cho khách hàng thành công")
                            .build();
    }

    @PreAuthorize("hasRole('HOTEL_STAFF')")
    @PatchMapping("/checkout/{bookingId}")
    public ApiResponse<Void> checkout(@PathVariable(name = "bookingId") UUID bookingId){
        bookingService.checkout(bookingId);
        return ApiResponse.<Void>builder()
                            .code(200)
                            .message("Checkout cho khách hàng thành công")
                            .build();
    }

    @PreAuthorize("hasRole('HOTEL_STAFF')")
    @GetMapping("/today/{hotelId}")
    public ApiResponse<Page<BookingCheckinInfo>> getBookingCheckinToday(
        @PathVariable(name = "hotelId") UUID hotelId,
        @RequestParam(required = false, name = "page", defaultValue = "0") @Min(0) int page,
        @RequestParam(required = false, name = "size", defaultValue = "10") @Min(10) @Max(30) int size
    ){
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        return ApiResponse.<Page<BookingCheckinInfo>>builder()
                        .code(200)
                        .message("Lấy danh sách booking hôm nay của khách sạn thành công")
                        .data(bookingService.getBookingToday(hotelId, LocalDate.now(), BookingStatus.CONFIRMED, pageable))
                        .build();
    }

    @PreAuthorize("hasRole('HOTEL_STAFF')")
    @GetMapping("/{hotelId}/checkin")
    public ApiResponse<Page<BookingCheckinInfo>> getBookingIsCheckin(
        @PathVariable(name = "hotelId") UUID hotelId,
        @RequestParam(required = false, name = "page", defaultValue = "0") @Min(0) int page,
        @RequestParam(required = false, name = "size", defaultValue = "10") @Min(10) @Max(30) int size
    ){
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "checkoutDate"));
        return ApiResponse.<Page<BookingCheckinInfo>>builder()
                        .code(200)
                        .message("Lấy danh sách booking hôm nay của khách sạn thành công")
                        .data(bookingService.getBookingToday(hotelId, null, BookingStatus.CHECKEDIN, pageable))
                        .build();
    }
}
