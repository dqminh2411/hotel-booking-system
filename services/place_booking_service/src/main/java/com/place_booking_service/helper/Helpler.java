package com.place_booking_service.helper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.place_booking_service.dto.DuplicateRequestResult;
import com.place_booking_service.dto.PlaceBookingRequest;
import com.place_booking_service.repository.SagaStateRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Component
@Slf4j
public class Helpler {

    StringRedisTemplate stringRedisTemplate;
    SagaStateRepository sagaStateRepository;

    public DuplicateRequestResult isFirstRequest(UUID userId, String hashRequest, UUID candidateBookingId){
        String key = "dedup:booking-request:" + userId.toString() + ":" + hashRequest;

        try {
            Boolean isFirst = stringRedisTemplate.opsForValue()
                    .setIfAbsent(key, candidateBookingId.toString(), Duration.ofMinutes(5));

            if (Boolean.TRUE.equals(isFirst)) {
                return new DuplicateRequestResult(false, candidateBookingId);
            }
            String value = stringRedisTemplate.opsForValue().get(key);
            if (value == null) {
                return new DuplicateRequestResult(false, candidateBookingId);
            }
            return new DuplicateRequestResult(true, UUID.fromString(value));

        } catch (Exception ex) {
            log.warn("Redis lỗi, kiểm tra DB để check trùng lặp (userId={})", userId, ex);
            return checkDuplicateFromDatabase(userId, hashRequest, candidateBookingId);
        }
    }

    private DuplicateRequestResult checkDuplicateFromDatabase(UUID userId, String hashRequest, UUID candidateBookingId){
        LocalDateTime sinceTime = LocalDateTime.now().minus(Duration.ofMinutes(5));
        return sagaStateRepository.findFirstByUserIdAndHashRequestAndCreatedAtAfterOrderByCreatedAtDesc(userId, hashRequest, sinceTime)
                                .map(sg -> new DuplicateRequestResult(true, sg.getBookingId()))
                                .orElseGet(() -> new DuplicateRequestResult(false, candidateBookingId));
    }

    public String toStringPlaceBookingRequest(PlaceBookingRequest placeBookingRequest){
        String roomTypes = placeBookingRequest.getRoomTypeList().stream()
                        .sorted(Comparator.comparing(rt -> rt.getRoomTypeId().toString()))
                        .map(rt -> rt.getRoomTypeId()+":"+rt.getBookingQuantity())
                        .collect(Collectors.joining(","));

        String bodyRequest = String.join("|",
            placeBookingRequest.getUserId().toString(),
            placeBookingRequest.getHotelId().toString(),
            placeBookingRequest.getCheckin(),
            placeBookingRequest.getCheckout(),
            String.valueOf(placeBookingRequest.getNumAdults()),
            roomTypes
        );

        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = sha.digest(bodyRequest.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 không khả dụng: ", ex);
        }
    }
}
