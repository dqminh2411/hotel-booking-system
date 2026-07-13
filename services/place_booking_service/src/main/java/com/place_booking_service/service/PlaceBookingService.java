package com.place_booking_service.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.place_booking_service.dto.CreateBooking;
import com.place_booking_service.dto.HotelSummaryResponse;
import com.place_booking_service.dto.PlaceBookingRequest;
import com.place_booking_service.dto.User;
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
    OutboxPublisherService outboxPublisherService;

    @Transactional
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
                    org.springframework.http.HttpStatus.CONFLICT
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
        sagaState.setCurrentStep("STARTED");
        sagaState.setCreatedAt(LocalDateTime.now());
        sagaState.setUpdatedAt(LocalDateTime.now());
        sagaState.setUserId(placeBookingRequest.getUserId());
        sagaState.setHashRequest(hashRequest);
        sagaStateRepository.save(sagaState);

        outboxPublisherService.saveOutboxMessage("booking-commands", createBooking, createBooking.getEventType());

        return bookingId;
    }

}
