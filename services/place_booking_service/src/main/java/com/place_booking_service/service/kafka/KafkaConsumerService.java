package com.place_booking_service.service.kafka;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelbooking.chassis.outbox.service.OutboxRelay;
import com.place_booking_service.dto.*;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.place_booking_service.entity.SagaState;
import com.place_booking_service.repository.SagaStateRepository;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class KafkaConsumerService {

    SagaStateRepository sagaStateRepository;
    OutboxRelay outboxPublisherService;

    @KafkaListener(topics = "booking-events")
    @Transactional
    public void bookingEventsHandler(String payloadJson) throws JsonProcessingException {
        Map<String, Object> payload = new ObjectMapper().readValue(payloadJson, Map.class);
        String eventType = (String) payload.get("eventType");
        UUID sagaId = UUID.fromString((String) payload.get("sagaId"));
        System.out.println(eventType);
        Optional<SagaState> state= sagaStateRepository.findSagaStateById(sagaId);
        if(state.isEmpty()){
            System.out.println("sagaId is emplty:");
            return;
        }
        ObjectMapper mapper = new ObjectMapper();

        switch (eventType) {
            case "BookingCreated" -> handleBookingCreated(mapper.convertValue(payload, BookingCreated.class));
            case "BookingConfirmed" -> handleBookingConfirmed(mapper.convertValue(payload, BookingConfirmed.class));
            case "BookingFailed" -> handleBookingFailed(mapper.convertValue(payload, BookingFailed.class));
            case "BookingCancelled" -> handleBookingCancelled(mapper.convertValue(payload, BookingCancelled.class));
        }
    }
    @KafkaListener(topics = "payment-events")
    @Transactional
    public void paymentEventsHandler(String payloadJson) throws JsonProcessingException {
        Map<String, Object> payload = new ObjectMapper().readValue(payloadJson, Map.class);
        String eventType = (String) payload.get("eventType");
        UUID sagaId = UUID.fromString((String) payload.get("sagaId"));
        System.out.println(eventType);
        Optional<SagaState> state= sagaStateRepository.findSagaStateById(sagaId);
        if(state.isEmpty()){
            return;
        }
        ObjectMapper mapper = new ObjectMapper();
        switch (eventType) {
            case "PaymentSucceeded"-> handlePaymentSucceeded(mapper.convertValue(payload, PaymentSucceeded.class)) ;
            case "PaymentFailed"-> handlePaymentFailed(mapper.convertValue(payload, PaymentFailed.class));
        }
    }

    @KafkaListener(topics = "promotion-events")
    @Transactional
    public void promotionEventsHandler(String payloadJson) throws JsonProcessingException {
        Map<String, Object> payload = new ObjectMapper().readValue(payloadJson, Map.class);
        String eventType = (String) payload.get("eventType");
        UUID sagaId = UUID.fromString((String) payload.get("sagaId"));
        System.out.println("promotion-event: " + eventType);
        Optional<SagaState> state = sagaStateRepository.findSagaStateById(sagaId);
        if (state.isEmpty()) {
            return;
        }
        ObjectMapper mapper = new ObjectMapper();
        switch (eventType) {
            case "PromotionValidated" -> handlePromotionValidated(mapper.convertValue(payload, PromotionValidated.class));
            case "PromotionRejected" -> handlePromotionRejected(mapper.convertValue(payload, PromotionRejected.class));
            case "PromotionUsageConfirmed" -> handlePromotionUsageConfirmed(mapper.convertValue(payload, PromotionUsageConfirmed.class));
            case "PromotionUsageFailed" -> handlePromotionUsageFailed(mapper.convertValue(payload, PromotionUsageFailed.class));
        }
    }

    private void handlePromotionValidated(PromotionValidated event) throws JsonProcessingException {
        Optional<SagaState> sagaOpt = sagaStateRepository.findByBookingId(event.getBookingId());
        if (sagaOpt.isEmpty()) {
            return;
        }

        SagaState saga = sagaOpt.get();
        saga.setCurrentStep("PROMOTION_VALIDATED");
        saga.setUpdatedAt(LocalDateTime.now());
        sagaStateRepository.save(saga);

        if (saga.getPendingPayload() != null && !saga.getPendingPayload().isBlank()) {
            ObjectMapper mapper = new ObjectMapper();
            CreateBooking createBooking = mapper.readValue(saga.getPendingPayload(), CreateBooking.class);
            createBooking.setFinalAmount(event.getFinalAmount());
            createBooking.setTotalAmount(event.getFinalAmount());
            outboxPublisherService.saveOutboxMessage("booking-commands", createBooking, createBooking.getEventType());
        }
    }

    private void handlePromotionRejected(PromotionRejected event) {
        Optional<SagaState> sagaOpt = sagaStateRepository.findByBookingId(event.getBookingId());
        if (sagaOpt.isEmpty()) {
            return;
        }

        SagaState saga = sagaOpt.get();
        saga.setStatus("FAILED");
        saga.setCurrentStep("PROMOTION_REJECTED");
        saga.setUpdatedAt(LocalDateTime.now());
        sagaStateRepository.save(saga);

        SendBookingFailed sendBookingFailed = new SendBookingFailed();
        sendBookingFailed.setBookingId(event.getBookingId());
        sendBookingFailed.setSagaId(saga.getId());
        sendBookingFailed.setEventType("SendBookingFailed");
        sendBookingFailed.setReason(event.getReason() != null ? event.getReason() : "Mã giảm giá không hợp lệ hoặc đã hết lượt sử dụng.");
        sendBookingFailed.setTo(null); // Không gửi email

        // Đính kèm BookingInfo chứa userId để Notification Service có thể gửi FCM Push Notification
        BookingInfo bookingInfo = new BookingInfo();
        bookingInfo.setBookingId(event.getBookingId());
        if (saga.getPendingPayload() != null && !saga.getPendingPayload().isBlank()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                CreateBooking createBooking = mapper.readValue(saga.getPendingPayload(), CreateBooking.class);
                bookingInfo.setCustomer(createBooking.getUser());
            } catch (Exception ignore) {}
        }
        if (bookingInfo.getCustomer() == null) {
            User user = new User();
            user.setUserId(saga.getUserId());
            bookingInfo.setCustomer(user);
        }
        sendBookingFailed.setBooking(bookingInfo);

        outboxPublisherService.saveOutboxMessage("notification-commands", sendBookingFailed, "SendBookingFailed");
    }

    private void handlePromotionUsageConfirmed(PromotionUsageConfirmed event) {
        Optional<SagaState> sagaOpt = sagaStateRepository.findByBookingId(event.getBookingId());
        if (sagaOpt.isEmpty()) {
            return;
        }

        SagaState saga = sagaOpt.get();
        saga.setCurrentStep("PROMOTION_USAGE_CONFIRMED");
        saga.setUpdatedAt(LocalDateTime.now());
        sagaStateRepository.save(saga);

        ConfirmBooking confirmBooking = new ConfirmBooking();
        confirmBooking.setBookingId(event.getBookingId());
        confirmBooking.setEventType("ConfirmBooking");
        confirmBooking.setSagaId(saga.getId());
        outboxPublisherService.saveOutboxMessage("booking-commands", confirmBooking, "ConfirmBooking");
    }

    private void handlePromotionUsageFailed(PromotionUsageFailed event) {
        Optional<SagaState> sagaOpt = sagaStateRepository.findByBookingId(event.getBookingId());
        if (sagaOpt.isEmpty()) {
            return;
        }

        SagaState saga = sagaOpt.get();
        saga.setStatus("FAILED");
        saga.setCurrentStep("PROMOTION_USAGE_FAILED");
        saga.setUpdatedAt(LocalDateTime.now());
        sagaStateRepository.save(saga);
    }


    private void handleBookingCreated(BookingCreated bookingCreated) {
        System.out.println(bookingCreated);

        Optional<SagaState> sagaOpt = sagaStateRepository.findByBookingId(bookingCreated.getBookingId());
        if (sagaOpt.isEmpty()) {
            return; // Saga not found, skip
        }



        SagaState saga = sagaOpt.get();
        saga.setCurrentStep("BOOKING_CREATED");
        saga.setUpdatedAt(LocalDateTime.now());
        sagaStateRepository.save(saga);

        // Publish ProcessPayment command
        ProcessPayment processPayment = new ProcessPayment();
        processPayment.setEventType("ProcessPayment");
        processPayment.setSagaId(saga.getId());
        processPayment.setBookingId(bookingCreated.getBookingId());
        processPayment.setAmount(bookingCreated.getTotalAmount());
        processPayment.setCurrency(bookingCreated.getCurrency());
        processPayment.setPaymentMethod(bookingCreated.getPaymentMethod());
        processPayment.setPaymentToken(bookingCreated.getPaymentToken());
        processPayment.setIdempotencyKey(saga.getIdempotencyKey());
        processPayment.setUserId(bookingCreated.getUserId());

        outboxPublisherService.saveOutboxMessage("payment-commands",processPayment,"ProcessPayment");



    }

    private void handleBookingConfirmed(BookingConfirmed bookingConfirmed) {

        System.out.println(bookingConfirmed);

        UUID bookingId = bookingConfirmed.getBooking().getBookingId();
        Optional<SagaState> sagaOpt = sagaStateRepository.findByBookingId(bookingId);

        System.out.println(sagaOpt.isPresent());
        if (sagaOpt.isEmpty()) {
            return; // Saga not found, skip
        }


        SagaState saga = sagaOpt.get();
        saga.setStatus("CONFIRMED");
        saga.setCurrentStep("COMPLETED");
        saga.setUpdatedAt(LocalDateTime.now());
        sagaStateRepository.save(saga);

        // Publish SendNotification command for BOOKING_CONFIRMED
        if (bookingConfirmed.getBooking().getCustomer() != null
            && bookingConfirmed.getBooking().getCustomer().getEmail() != null) {
            SendBookingConfirmed sendBookingConfirmed = new SendBookingConfirmed();
            sendBookingConfirmed.setBookingId(bookingId);
            sendBookingConfirmed.setTo(bookingConfirmed.getBooking().getCustomer().getEmail());
            sendBookingConfirmed.setSagaId(saga.getId());
            sendBookingConfirmed.setBooking(bookingConfirmed.getBooking());
            sendBookingConfirmed.setEventType("SendBookingConfirmed");
            outboxPublisherService.saveOutboxMessage("notification-commands",sendBookingConfirmed,"SendBookingConfirmed");

        }
    }

    private void handleBookingFailed(BookingFailed bookingFailed) {

        System.out.println(bookingFailed);
        UUID bookingId = bookingFailed.getBooking().getBookingId();
        Optional<SagaState> sagaOpt = sagaStateRepository.findByBookingId(bookingId);

        if (sagaOpt.isEmpty()) {
            return;
        }

        SagaState saga = sagaOpt.get();
        saga.setStatus("FAILED");
        saga.setCurrentStep("BOOKING_FAILED");
        saga.setUpdatedAt(LocalDateTime.now());
        sagaStateRepository.save(saga);

        if (saga.getCouponCode() != null && !saga.getCouponCode().isBlank()) {
            ReleasePromotionUsage releaseCmd = ReleasePromotionUsage.builder()
                    .eventType("ReleasePromotionUsage")
                    .sagaId(saga.getId())
                    .bookingId(bookingId)
                    .couponCode(saga.getCouponCode())
                    .reason(bookingFailed.getReason() != null ? bookingFailed.getReason() : "Booking failed")
                    .build();
            outboxPublisherService.saveOutboxMessage("promotion-commands", releaseCmd, "ReleasePromotionUsage");
        }

        // Publish SendNotification command for BOOKING_CANCELLED
        if (bookingFailed.getBooking().getCustomer() != null
            && bookingFailed.getBooking().getCustomer().getEmail() != null) {
            SendBookingFailed sendBookingFailed = new SendBookingFailed();
            sendBookingFailed.setBookingId(bookingId);
            sendBookingFailed.setTo(bookingFailed.getBooking().getCustomer().getEmail());
            sendBookingFailed.setSagaId(saga.getId());
            sendBookingFailed.setBooking(bookingFailed.getBooking());
            sendBookingFailed.setEventType("SendBookingFailed");
            sendBookingFailed.setReason(bookingFailed.getReason());
            outboxPublisherService.saveOutboxMessage("notification-commands",sendBookingFailed,"SendBookingFailed");

        }
    }

    private void handleBookingCancelled(BookingCancelled bookingCancelled) {

        System.out.println(bookingCancelled);

        UUID bookingId = bookingCancelled.getBooking().getBookingId();
        Optional<SagaState> sagaOpt = sagaStateRepository.findByBookingId(bookingId);

        if (sagaOpt.isEmpty()) {
            return; // Saga not found, skip
        }

        SagaState saga = sagaOpt.get();
        saga.setStatus("CANCELLED");
        saga.setCurrentStep("BOOKING_CANCELLED");
        saga.setUpdatedAt(LocalDateTime.now());
        sagaStateRepository.save(saga);

        // Publish SendNotification command for BOOKING_CANCELLED
        if (bookingCancelled.getBooking().getCustomer() != null
            && bookingCancelled.getBooking().getCustomer().getEmail() != null) {
            SendBookingFailed sendBookingFailed = new SendBookingFailed();
            sendBookingFailed.setBookingId(bookingId);
            sendBookingFailed.setTo(bookingCancelled.getBooking().getCustomer().getEmail());
            sendBookingFailed.setSagaId(saga.getId());
            sendBookingFailed.setBooking(bookingCancelled.getBooking());
            sendBookingFailed.setEventType("SendBookingFailed");
            sendBookingFailed.setReason(bookingCancelled.getReason());
            outboxPublisherService.saveOutboxMessage("notification-commands",sendBookingFailed,"SendBookingFailed");
        }
    }

    private void handlePaymentSucceeded(PaymentSucceeded paymentSucceeded) {
        System.out.println("payment succeeded "+paymentSucceeded);
        UUID bookingId = paymentSucceeded.getBookingId();
        Optional<SagaState> sagaOpt = sagaStateRepository.findByBookingId(bookingId);
        if (sagaOpt.isEmpty()) {
            return;
        }
        SagaState saga = sagaOpt.get();
        saga.setStatus("PAYMENT_SUCCEEDED");
        saga.setCurrentStep("PAYMENT_SUCCEEDED");
        saga.setUpdatedAt(LocalDateTime.now());
        sagaStateRepository.save(saga);

        if (saga.getCouponCode() != null && !saga.getCouponCode().isBlank()) {
            ConfirmPromotionUsage confirmCmd = ConfirmPromotionUsage.builder()
                    .eventType("ConfirmPromotionUsage")
                    .sagaId(saga.getId())
                    .bookingId(bookingId)
                    .couponCode(saga.getCouponCode())
                    .userId(saga.getUserId())
                    .build();
            outboxPublisherService.saveOutboxMessage("promotion-commands", confirmCmd, "ConfirmPromotionUsage");
        } else {
            ConfirmBooking confirmBooking = new ConfirmBooking();
            confirmBooking.setBookingId(bookingId);
            confirmBooking.setEventType("ConfirmBooking");
            confirmBooking.setSagaId(saga.getId());
            outboxPublisherService.saveOutboxMessage("booking-commands",confirmBooking,"ConfirmBooking");
        }
    }

    private void handlePaymentFailed(PaymentFailed paymentFailed) {
        System.out.println("payment failed "+paymentFailed);
        UUID bookingId = paymentFailed.getBookingId();
        Optional<SagaState> sagaOpt = sagaStateRepository.findByBookingId(bookingId);
        if (sagaOpt.isEmpty()) {
            return;
        }
        SagaState saga = sagaOpt.get();
        saga.setStatus("PAYMENT_FAILED");
        saga.setCurrentStep("PAYMENT_FAILED");
        saga.setUpdatedAt(LocalDateTime.now());
        sagaStateRepository.save(saga);

        if (saga.getCouponCode() != null && !saga.getCouponCode().isBlank()) {
            ReleasePromotionUsage releaseCmd = ReleasePromotionUsage.builder()
                    .eventType("ReleasePromotionUsage")
                    .sagaId(saga.getId())
                    .bookingId(bookingId)
                    .couponCode(saga.getCouponCode())
                    .reason(paymentFailed.getReason() != null ? paymentFailed.getReason() : "Payment failed")
                    .build();
            outboxPublisherService.saveOutboxMessage("promotion-commands", releaseCmd, "ReleasePromotionUsage");
        }

        CancelBooking cancelBooking = new CancelBooking();
        cancelBooking.setBookingId(bookingId);
        cancelBooking.setEventType("CancelBooking");
        cancelBooking.setSagaId(saga.getId());
        cancelBooking.setReason(paymentFailed.getReason());
        outboxPublisherService.saveOutboxMessage("booking-commands",cancelBooking,"CancelBooking");

    }





}

