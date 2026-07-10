package com.hotelbooking.bookingservice.entity;

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "booking_info")
public class BookingInfoEntity {
    @Id
    @Column(name = "booking_id")
    private UUID bookingId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "booking_detail", nullable = false, columnDefinition = "jsonb")
    private String bookingDetail;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
}
