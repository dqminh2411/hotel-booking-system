package com.hotelbooking.hotelservice.service.impl;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelbooking.hotelservice.client.UserServiceFeignClient;
import com.hotelbooking.hotelservice.constant.HotelStatus;
import com.hotelbooking.hotelservice.dto.request.EmailHotelStatusRequest;
import com.hotelbooking.hotelservice.dto.request.HotelImageDelRequest;
import com.hotelbooking.hotelservice.dto.request.EmailHotelStatusRequest.Hotel;
import com.hotelbooking.hotelservice.dto.request.EmailHotelStatusRequest.User;
import com.hotelbooking.hotelservice.dto.request.HotelUpdateStatusRequest;
import com.hotelbooking.hotelservice.dto.response.HotelPendingResponse;
import com.hotelbooking.hotelservice.dto.response.UserResponse;
import com.hotelbooking.hotelservice.entity.HotelEntity;
import com.hotelbooking.hotelservice.entity.HotelImageEntity;
import com.hotelbooking.hotelservice.entity.OutboxEventEntity;
import com.hotelbooking.hotelservice.exception.AppException;
import com.hotelbooking.hotelservice.mapper.HotelMapper;
import com.hotelbooking.hotelservice.repository.HotelImageRepository;
import com.hotelbooking.hotelservice.repository.HotelRepository;
import com.hotelbooking.hotelservice.repository.OutboxEventRepository;
import com.hotelbooking.hotelservice.service.AdminService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AdminServiceImpl implements AdminService{

    HotelRepository hotelRepository;
    HotelMapper hotelMapper;
    HotelImageRepository hotelImageRepository;
    StringRedisTemplate stringRedisTemplate;
    UserServiceFeignClient userServiceFeignClient;
    ObjectMapper objectMapper;
    OutboxEventRepository outboxEventRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<HotelPendingResponse> getListHotelPending(Pageable pageable){
        Page<HotelEntity> hotels = hotelRepository.findByStatus(HotelStatus.PENDING, pageable);
        if (hotels.isEmpty()) {
            return hotels.map(hotel -> null);
        }

        List<UUID> hotelIds = hotels.getContent().stream()
                                    .map(HotelEntity::getId)
                                    .toList();
        List<HotelImageEntity> hotelImageEntities = hotelImageRepository.findByHotel_IdInAndIsCoverTrue(hotelIds);
        Map<UUID, String> mapImgIsCover = hotelImageEntities.stream()
                                                .collect(Collectors.toMap(
                                                    img -> img.getHotel().getId(),
                                                    HotelImageEntity::getUrl,
                                                    (url1, url2) -> url1));

        return hotels.map(
            hotel -> new HotelPendingResponse(
                hotel.getId(),
                hotel.getName(),
                hotel.getTenantId(),
                hotelMapper.toAddress(hotel),
                mapImgIsCover.get(hotel.getId()),
                hotel.getStatus().toString(),
                hotel.getCreatedAt()
            )
        );
    }

    @Override
    @Transactional
    public void updateHotelStatus(UUID hotelId, HotelUpdateStatusRequest request){
        HotelEntity hotelEntity = hotelRepository.findByIdAndIsDeletedFalse(hotelId)
                        .orElseThrow(() -> new AppException("HOTEL_NOT_FOUND", "Không tìm thấy khách sạn có id=" + hotelId.toString(), HttpStatus.NOT_FOUND));
        
        if(hotelEntity.getStatus().equals(request.hotelStatus())){
            throw new AppException("INVALID_STATUS_TRANSITION", "Khách sạn hiện đã ở trạng thái này", HttpStatus.CONFLICT);
        }

        if (!List.of(HotelStatus.APPROVED, HotelStatus.SUSPENDED).contains(request.hotelStatus())) {
            throw new AppException("HOTEL_STATUS_CONFLICT", "Trạng thái để cập nhật không hợp lệ", HttpStatus.CONFLICT);
        }

        if (request.hotelStatus() == HotelStatus.SUSPENDED && !StringUtils.hasText(request.reason())) {
            throw new AppException("VALIDATION_ERROR", "Bắt buộc phải có lý do khi từ chối hoặc đình chỉ khách sạn", HttpStatus.BAD_REQUEST);
    }

        hotelEntity.setStatus(request.hotelStatus());
        hotelRepository.save(hotelEntity);

        // kafka sang noti - outbox event
        UserResponse tenant = userServiceFeignClient.getUserById(hotelEntity.getTenantId());
        String eventType = HotelStatus.APPROVED == request.hotelStatus() ? "HOTEL_APPROVED" : "HOTEL_SUSPENDED";
        EmailHotelStatusRequest emailRequest = new EmailHotelStatusRequest(
                    new User(tenant.userId(), tenant.fullName(), tenant.email()),
                    new Hotel(hotelEntity.getId(), hotelEntity.getName(), hotelEntity.getAddress()),
                    eventType,
                    request.reason()        
        );
        saveOutboxEvent(emailRequest, "hotel-status-actions");
        // invalid keys của hotel trong redis
        String keyPattern = "hotel-detail-availability" + "::" + hotelId.toString() + "::*";
        Set<String> keysToDelete = stringRedisTemplate.keys(keyPattern);

        if(keysToDelete != null && !keysToDelete.isEmpty()){
            stringRedisTemplate.delete(keysToDelete);
        }
    }

    @Override
    @Transactional
    public void deleteHotelImages(HotelImageDelRequest request){
        
    }

    private void saveOutboxEvent(Object event, String topic) {
        try {
            OutboxEventEntity outbox = new OutboxEventEntity();
            outbox.setId(UUID.randomUUID());
            outbox.setTopic(topic);
            outbox.setPayload(objectMapper.writeValueAsString(event));
            outbox.setPublished(Boolean.FALSE);
            outbox.setCreatedAt(Instant.now());
            outboxEventRepository.save(outbox);
        } catch (Exception ex) {
            throw new AppException("INTERNAL_SERVER_ERROR", "Failed to write outbox event", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
