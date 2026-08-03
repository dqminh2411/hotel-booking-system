package com.hotelbooking.hotelservice.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Spring application event nội bộ, publish ngay trong transaction tạo hotel.
 * Listener (HotelKafkaEventPublisher) sẽ bắt event này SAU KHI transaction
 * commit thành công rồi mới thật sự gửi message lên Kafka.
 */
public record HotelCreatedEvent(
        UUID hotelId,
        UUID tenantId,
        String hotelName,
        Instant createdAt
) {
}
