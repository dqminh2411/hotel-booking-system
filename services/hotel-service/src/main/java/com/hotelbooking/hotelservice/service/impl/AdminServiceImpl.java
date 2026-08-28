package com.hotelbooking.hotelservice.service.impl;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
import com.hotelbooking.hotelservice.exception.AppException;
import com.hotelbooking.hotelservice.mapper.HotelMapper;
import com.hotelbooking.hotelservice.repository.HotelImageRepository;
import com.hotelbooking.hotelservice.repository.HotelRepository;
import com.hotelbooking.chassis.outbox.service.OutboxRelay;
import com.hotelbooking.hotelservice.service.AdminService;
import com.hotelbooking.chassis.audit.AuditEventType;
import com.hotelbooking.chassis.audit.AuditLog;
import com.hotelbooking.chassis.audit.Severity;
import com.hotelbooking.chassis.audit.TargetType;

import io.minio.MinioClient;
import io.minio.RemoveObjectsArgs;
import io.minio.Result;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
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
    private final OutboxRelay outboxPublisherService;

    MinioClient minioClient;

    @NonFinal
    @Value("${minio.endpoint:http://minio:9000}")
    String endpoint;

    @NonFinal
    @Value("${minio.bucket.hotel-images:hotel-images}")
    String bucket;

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
        List<HotelImageEntity> hotelImageEntities = hotelImageRepository.findByHotel_IdInAndIsCoverTrueAndIsDeletedFalse(hotelIds);
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
    @AuditLog(
        eventType = AuditEventType.APPROVE_HOTEL,
        message = "Update hotel status",
        severity = Severity.INFO,
        targetType = TargetType.HOTEL,
        targetId = "#hotelId",
        extraData = {"status=#request.hotelStatus()"}
    )
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
       
        outboxPublisherService.saveEvent("hotel-status-actions", emailRequest);
        
        // clearKeyHotelId(hotelId);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                clearKeyHotelId(hotelId);
            }
        });
    }

    @Override
    @Transactional
    public void deleteHotelImages(HotelImageDelRequest request){
        List<HotelImageEntity> images = hotelImageRepository.findAllByIdInAndHotel_IdAndIsDeletedFalse(request.imgIds(), request.hotelId());

        if(images.size() != request.imgIds().size()){
            throw new AppException("IMAGE_NOT_AVAILABLE", "Một số ảnh không tồn tại hoặc không thuộc hotel", HttpStatus.BAD_REQUEST);
        }

        List<DeleteObject> imgNames = images.stream()
                                    .map(img -> new DeleteObject(extractImgName(img.getUrl())))
                                    .toList();

        hotelImageRepository.deleteAll(images);
        
        try {
            Iterable<Result<DeleteError>> results = minioClient.removeObjects(
                        RemoveObjectsArgs.builder()
                                .bucket(bucket)
                                .objects(imgNames)
                                .build()
            );

            for(Result<DeleteError> result : results){
                DeleteError error = result.get();
                log.error("Lỗi khi xóa ảnh {} khỏi MinIO: {}", error.objectName(), error.message());
            }
        } catch (Exception e) {
            log.error("Lỗi không xóa ảnh khổi MinIO được: bucket={}, imgNames={}", bucket, imgNames, e);
            throw new AppException("INTERNAL_SERVER_ERROR", "Lỗi không xóa được ảnh trong MinIO", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // clearKeyHotelId(request.hotelId());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                clearKeyHotelId(request.hotelId());
            }
        });
    }


    // http://minio:9000/hotel-images/imgName
    private String extractImgName(String imgUrl){
        return imgUrl.substring(imgUrl.lastIndexOf('/') + 1);
    }

    private void clearKeyHotelId(UUID hotelId){
        String keyPattern = "hotel-detail-availability" + "::" + hotelId.toString() + "::*";
        Set<String> keysToDelete = stringRedisTemplate.keys(keyPattern);

        if(keysToDelete != null && !keysToDelete.isEmpty()){
            stringRedisTemplate.delete(keysToDelete);
        }
    }
}
