package com.place_booking_service.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelbooking.chassis.audit.AuditEventType;
import com.hotelbooking.chassis.audit.AuditLog;
import com.hotelbooking.chassis.audit.Severity;
import com.hotelbooking.chassis.audit.TargetType;
import com.hotelbooking.chassis.outbox.service.OutboxRelay;
import com.place_booking_service.dto.CreateBooking;
import com.place_booking_service.dto.HotelSummaryResponse;
import com.place_booking_service.dto.PlaceBookingRequest;
import com.place_booking_service.dto.User;
import com.place_booking_service.dto.ValidatePromotion;
import com.place_booking_service.entity.SagaState;
import com.place_booking_service.exception.PlaceBookingException;
import com.place_booking_service.exception.SagaStateConflictException;
import com.place_booking_service.repository.SagaStateRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PlaceBookingService {

    SagaStateRepository sagaStateRepository;
    OutboxRelay outboxPublisherService;
    ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    @AuditLog(
        eventType = AuditEventType.CREATE_BOOKING,
        message = "Start booking saga",
        severity = Severity.INFO,
        targetType = TargetType.BOOKING,
        targetId = "#bookingId",
        extraData = {
            "hotelId=#hotel.hotelId",
            "userId=#placeBookingRequest.userId",
            "checkin=#placeBookingRequest.checkin",
            "checkout=#placeBookingRequest.checkout"
        }
    )
    public UUID startSaga(PlaceBookingRequest placeBookingRequest, User user, HotelSummaryResponse hotel,
            UUID bookingId, String hashRequest) {
        
        if(sagaStateRepository.existsSagaStateByIdempotencyKey(placeBookingRequest.getIdempotencyKey())){
            SagaState sagaState = sagaStateRepository.findSagaStateByIdempotencyKey(placeBookingRequest.getIdempotencyKey());
            if (sagaState == null || sagaState.getBookingId() == null) {
                throw new SagaStateConflictException(placeBookingRequest.getIdempotencyKey());
            }
            if(!sagaState.getHashRequest().equals(hashRequest)){
                throw new PlaceBookingException(
                    "SAGA_STATE_CONFLICT",
                    "Có lỗi trong quá trình thực hiện yêu cầu. Vui lòng thử lại sau",
                    HttpStatus.CONFLICT
                );
            }
            return sagaState.getBookingId();
        }
        
        CreateBooking createBooking = new CreateBooking(placeBookingRequest);
        createBooking.setUser(user);
        createBooking.setHotel(hotel);
        createBooking.setBookingId(bookingId);
        UUID sagaId = UUID.randomUUID();
        createBooking.setSagaId(sagaId);
        createBooking.setEventType("CreateBooking");

        SagaState sagaState = new SagaState();
        sagaState.setId(sagaId);
        sagaState.setIdempotencyKey(placeBookingRequest.getIdempotencyKey());
        sagaState.setBookingId(bookingId);
        sagaState.setStatus("IN_PROGRESS");
        sagaState.setCreatedAt(LocalDateTime.now());
        sagaState.setUpdatedAt(LocalDateTime.now());
        sagaState.setUserId(placeBookingRequest.getUserId());
        sagaState.setHashRequest(hashRequest);

        boolean hasCoupon = placeBookingRequest.getCouponCode() != null && !placeBookingRequest.getCouponCode().isBlank();
        if (hasCoupon) {
            sagaState.setCouponCode(placeBookingRequest.getCouponCode().trim());
            sagaState.setCurrentStep("VALIDATING_PROMOTION");
            try {
                sagaState.setPendingPayload(objectMapper.writeValueAsString(createBooking));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize createBooking payload", e);
            }
            sagaStateRepository.save(sagaState);

            ValidatePromotion validateCmd = ValidatePromotion.builder()
                    .eventType("ValidatePromotion")
                    .sagaId(sagaId)
                    .bookingId(bookingId)
                    .userId(placeBookingRequest.getUserId())
                    .couponCode(placeBookingRequest.getCouponCode().trim())
                    .hotelId(placeBookingRequest.getHotelId())
                    .roomTypeIds(placeBookingRequest.getRoomTypeList() != null
                            ? placeBookingRequest.getRoomTypeList().stream().map(r -> r.getRoomTypeId()).toList()
                            : java.util.List.of())
                    .checkin(placeBookingRequest.getCheckin())
                    .checkout(placeBookingRequest.getCheckout())
                    .totalAmount(placeBookingRequest.getTotalAmount())
                    .build();

            outboxPublisherService.saveOutboxMessage("promotion-commands", validateCmd, "ValidatePromotion");
        } else {
            sagaState.setCurrentStep("STARTED");
            sagaStateRepository.save(sagaState);
            outboxPublisherService.saveOutboxMessage("booking-commands", createBooking, createBooking.getEventType());
        }

        return bookingId;
    }

}
