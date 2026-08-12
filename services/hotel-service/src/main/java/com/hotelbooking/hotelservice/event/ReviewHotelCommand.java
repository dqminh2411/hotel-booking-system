package com.hotelbooking.hotelservice.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Hợp đồng message của topic "review-hotel-command".
 * Mọi thay đổi field ở đây PHẢI đồng bộ với file docs/kafka-events.md
 * và thông báo cho team consumer (admin-service).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReviewHotelCommand {

    /** UUID riêng cho từng message, giúp consumer chống xử lý trùng (idempotency). */
    private UUID eventId;

    private UUID hotelId;

    private UUID tenantId;

    private String hotelName;

    private Instant requestedAt;
}
