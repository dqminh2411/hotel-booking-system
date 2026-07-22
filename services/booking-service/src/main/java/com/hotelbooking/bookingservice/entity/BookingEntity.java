package com.hotelbooking.bookingservice.entity;

import com.hotelbooking.bookingservice.enums.BookingStatus;
import com.hotelbooking.bookingservice.enums.PaymentMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

@Getter
@Setter
@Entity
@Table(name = "bookings")
public class BookingEntity {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "hotel_id", nullable = false)
    private UUID hotelId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "checkin_date", nullable = false)
    private LocalDate checkinDate;

    @Column(name = "checkout_date", nullable = false)
    private LocalDate checkoutDate;

    @Column(name = "num_adults", nullable = false)
    private Integer numAdults;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", nullable = false, columnDefinition = "booking_status_enum")
    private BookingStatus status;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "payment_method", columnDefinition = "payment_method_enum")
    private PaymentMethod paymentMethod;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
}
