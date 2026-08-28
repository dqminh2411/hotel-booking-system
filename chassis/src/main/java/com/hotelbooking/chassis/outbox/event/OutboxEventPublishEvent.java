package com.hotelbooking.chassis.outbox.event;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEventPublishEvent {
    private UUID eventId;
}
