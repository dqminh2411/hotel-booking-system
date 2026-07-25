package com.place_booking_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookingInfo {
    private UUID bookingId;
    private User customer;
    private String checkin;
    private String checkout;
    private int numAdults;
    private BigDecimal totalAmount;
    private BigDecimal originalAmount;
    private BigDecimal finalAmount;
    private Hotel hotel;
    private List<RoomType> roomTypeList;
}
