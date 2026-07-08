package com.place_booking_service.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendBookingFailed {
    private String eventType;
    private UUID sagaId;
    private UUID bookingId;
    private String to;
    private BookingInfo booking;
    private String reason;
}
