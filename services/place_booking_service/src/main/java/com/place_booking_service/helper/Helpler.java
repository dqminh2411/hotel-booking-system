package com.place_booking_service.helper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
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

    public DuplicateRequestResult isDuplicateRequest(UUID userId, String hashRequest, String forceToken) {
        // check token có đúng sau khi gửi lại từ user hay không (kiểu xác nhận đặt lại ấy)
        String expectedToken = generateForceToken(userId, hashRequest);
        if (expectedToken.equals(forceToken)) {
            return new DuplicateRequestResult(false, null);
        }

        String key = "dup:booking-request:" + userId.toString() + ":" + hashRequest;
        try {
            Boolean isFirst = stringRedisTemplate.opsForValue().setIfAbsent(key, "locked", Duration.ofSeconds(10));
            if (Boolean.FALSE.equals(isFirst)) {
                return new DuplicateRequestResult(true, null);
            }
        } catch (Exception ex) {
            log.warn("Redis lỗi: ", ex);
        }

        
        LocalDateTime sinceTime = LocalDateTime.now().minus(Duration.ofMinutes(5));
        List<String> statuses = List.of("FAILED", "CANCELLED");

        return sagaStateRepository.findFirstByUserIdAndHashRequestAndCreatedAtAfterAndStatusNotInOrderByCreatedAtDesc(
                userId, hashRequest, sinceTime, statuses)
            .map(sg -> new DuplicateRequestResult(true, sg.getBookingId()))
            .orElse(new DuplicateRequestResult(false, null));
    }

    public String generateForceToken(UUID userId, String hashRequest) {
        String raw = userId.toString() + ":" + hashRequest + ":confirmed";
        return hashSHA256(raw);
    }

    public String toStringPlaceBookingRequest(PlaceBookingRequest request){
        String roomTypes = request.getRoomTypeList().stream()
                        .sorted(Comparator.comparing(rt -> rt.getRoomTypeId().toString()))
                        .map(rt -> rt.getRoomTypeId()+":"+rt.getBookingQuantity())
                        .collect(Collectors.joining(","));

        String bodyRequest = String.join("|",
            request.getUserId().toString(),
            request.getHotelId().toString(),
            request.getCheckin(),
            request.getCheckout(),
            String.valueOf(request.getNumAdults()),
            roomTypes
        );
        return hashSHA256(bodyRequest);
    }

    private String hashSHA256(String input) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = sha.digest(input.getBytes(StandardCharsets.UTF_8));
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
