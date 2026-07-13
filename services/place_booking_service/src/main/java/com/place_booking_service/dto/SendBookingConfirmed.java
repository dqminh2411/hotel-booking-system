package com.place_booking_service.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendBookingConfirmed {
    private String eventType;
    private UUID sagaId;
    private UUID bookingId;
    private String to;
    private BookingInfo booking;

}
