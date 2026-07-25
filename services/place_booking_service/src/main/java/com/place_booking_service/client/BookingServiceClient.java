package com.place_booking_service.client;
import java.time.LocalDate;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.constraints.NotNull;
import com.place_booking_service.dto.CountBookingsResponse;

import java.util.List;
import java.util.UUID;
@FeignClient(name = "booking-service")
public interface BookingServiceClient {
    @GetMapping("/bookings/count")
    public CountBookingsResponse countBookings(
        @RequestParam(required = false) UUID hotelId,
        @RequestParam(required = false) List<UUID> roomTypeList,
        @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkin,
        @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkout
    );
}
