package com.place_booking_service.dto;



import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBooking {
    @NotNull
    private UUID sagaId;
    @NotBlank
    private String eventType;
    @NotNull
    private User user;
    @NotNull
    private HotelSummaryResponse hotel;
    @NotNull
    private UUID bookingId;
    @NotNull
    private List<RoomType> roomTypeList;
    @NotBlank
    private String checkin;
    @NotBlank
    private String checkout;
    @Min(1)
    private int numAdults;
    @NotNull
    private BigDecimal totalAmount;
    private BigDecimal originalAmount;
    private BigDecimal finalAmount;
    @NotBlank
    private String currency;
    @NotBlank
    private String paymentMethod;
    @NotBlank
    private String paymentToken;

    public CreateBooking(PlaceBookingRequest placeBookingRequest) {
        this.checkin = placeBookingRequest.getCheckin();
        this.checkout = placeBookingRequest.getCheckout();
        this.currency = placeBookingRequest.getCurrency();
        this.numAdults = placeBookingRequest.getNumAdults();
        this.totalAmount = placeBookingRequest.getTotalAmount();
        this.originalAmount = placeBookingRequest.getTotalAmount();
        this.finalAmount = placeBookingRequest.getTotalAmount();
        this.paymentMethod = placeBookingRequest.getPaymentMethod();
        this.paymentToken = placeBookingRequest.getPaymentToken();
        this.roomTypeList = placeBookingRequest.getRoomTypeList();
    }





}
