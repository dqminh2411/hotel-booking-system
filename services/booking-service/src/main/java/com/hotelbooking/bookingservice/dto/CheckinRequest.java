package com.hotelbooking.bookingservice.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;


public record CheckinRequest(
    
    @NotNull(message = "BookingId không được trống")
    UUID bookingId,

    @NotEmpty(message = "Danh sách phòng cần checkin không được để trống")
    Map<UUID, List<UUID>> listRoomId
) {

}
