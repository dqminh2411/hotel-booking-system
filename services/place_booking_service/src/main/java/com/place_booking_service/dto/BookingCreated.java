package com.place_booking_service.dto;



import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;
import java.math.BigDecimal;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingCreated {

    @NotNull
    private UUID sagaId;
    @NotBlank
    private String eventType;
    @NotNull
    private UUID bookingId;
    @NotNull
    private UUID userId;
    private BigDecimal totalAmount;
    private String currency;
    private String paymentMethod;
    private String paymentToken;


}
