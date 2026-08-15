package com.hotelbooking.chassis.outbox.entity;

public enum OutboxEventStatus {
    PENDING, PROCESSING, PUBLISHED, DEAD_LETTER
}
