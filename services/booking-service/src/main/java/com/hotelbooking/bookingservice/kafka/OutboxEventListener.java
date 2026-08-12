package com.hotelbooking.bookingservice.kafka;

import java.util.List;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.hotelbooking.bookingservice.dto.OutboxEventPublishEvent;
import com.hotelbooking.bookingservice.entity.OutboxEventEntity;
import com.hotelbooking.bookingservice.service.BookingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventListener {

    private final BookingService bookingService;
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOutboxSaved(OutboxEventPublishEvent event){
        List<OutboxEventEntity> events = bookingService.getAndProcessOutboxEvents();
        bookingService.sendOutboxEventToKafka(events);
    }
}
