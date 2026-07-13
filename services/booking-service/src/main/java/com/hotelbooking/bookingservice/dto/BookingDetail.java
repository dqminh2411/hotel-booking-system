package com.hotelbooking.bookingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class BookingDetail {
    private UUID bookingId;
    private Customer customer;
    private String checkin;
    private String checkout;
    private int numAdults;
    private BigDecimal totalAmount;
    private Hotel hotel;
    private List<RoomType> roomTypeList;
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Customer {
        private UUID userId;
        private String name;
        private String email;
    }
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Hotel{
        private UUID hotelId;
        private String name;
        private String address;
    }
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RoomType{
        private UUID roomTypeId;
        private String name;
        private int bedCount;
        private int bookingQuantity;
        private int totalQuantity;
        private BigDecimal price;
    }
}
