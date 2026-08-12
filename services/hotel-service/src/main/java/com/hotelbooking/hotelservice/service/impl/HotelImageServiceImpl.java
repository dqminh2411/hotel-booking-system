package com.hotelbooking.hotelservice.service.impl;

import com.hotelbooking.hotelservice.dto.response.HotelImageResponse;
import com.hotelbooking.hotelservice.entity.HotelEntity;
import com.hotelbooking.hotelservice.entity.HotelImageEntity;
import com.hotelbooking.hotelservice.exception.AppException;
import com.hotelbooking.hotelservice.repository.HotelImageRepository;
import com.hotelbooking.hotelservice.repository.HotelRepository;
import com.hotelbooking.hotelservice.security.SecurityUtils;
import com.hotelbooking.hotelservice.service.HotelImageService;
import com.hotelbooking.hotelservice.service.MinioStorageService;
import com.hotelbooking.hotelservice.util.ImageValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class HotelImageServiceImpl implements HotelImageService {

    private final HotelRepository hotelRepository;
    private final HotelImageRepository hotelImageRepository;
    private final MinioStorageService minioStorageService;
    private final SecurityUtils securityUtils;

    @Value("${minio.bucket.hotel-images}")
    private String bucketName;

    @Override
    @PreAuthorize("hasRole('HOTEL_OWNER')")
    public List<HotelImageResponse> uploadImages(UUID hotelId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new AppException(
                    "IMAGES_REQUIRED",
                    "Phải gửi ít nhất 1 file ảnh",
                    HttpStatus.BAD_REQUEST);
        }

        HotelEntity hotel = hotelRepository.findById(hotelId)
                .filter(h -> !Boolean.TRUE.equals(h.getIsDeleted()))
                .orElseThrow(() -> new AppException(
                        "HOTEL_NOT_FOUND",
                        "Không tìm thấy hotel id: " + hotelId,
                        HttpStatus.NOT_FOUND));

        // Chỉ đúng chủ khách sạn (không phải bất kỳ ai có role HOTEL_OWNER) mới được thêm ảnh
        UUID currentTenantId = securityUtils.getCurrentTenantId();
        if (!hotel.getTenantId().equals(currentTenantId)) {
            throw new AppException(
                    "NOT_HOTEL_OWNER",
                    "Bạn không phải chủ sở hữu khách sạn này",
                    HttpStatus.FORBIDDEN);
        }

        // Validate TOÀN BỘ batch trước, chưa đụng gì tới MinIO.
        // Nếu 1 file sai, fail ngay, không có object nào bị upload lên MinIO cả.
        files.forEach(ImageValidationUtils::validate);

        boolean hasCoverAlready = hotelImageRepository
                .findByHotel_IdAndIsCoverTrueAndIsDeletedFalse(hotelId)
                .isPresent();

        List<String> uploadedObjectNames = new ArrayList<>();
        List<HotelImageEntity> preparedEntities = new ArrayList<>();

        try {
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                String extension = ImageValidationUtils.extractExtension(file.getOriginalFilename());
                String objectName = "%s/%s.%s".formatted(hotelId, UUID.randomUUID(), extension);

                String url;
                try {
                    url = minioStorageService.uploadFile(
                            bucketName,
                            objectName,
                            file.getInputStream(),
                            file.getSize(),
                            file.getContentType());
                } catch (IOException e) {
                    throw new AppException(
                            "IMAGE_READ_FAILED",
                            "Không đọc được file '" + file.getOriginalFilename() + "'",
                            HttpStatus.BAD_REQUEST);
                }

                uploadedObjectNames.add(objectName);

                HotelImageEntity entity = new HotelImageEntity();
                entity.setHotel(hotel);
                entity.setUrl(url);
                entity.setObjectName(objectName);
                // Chỉ ảnh ĐẦU TIÊN của batch này được làm cover, và CHỈ KHI hotel
                // chưa có cover nào từ trước (tính cả các lần upload trước đó).
                entity.setIsCover(!hasCoverAlready && i == 0);
                entity.setIsDeleted(false);
                entity.setCreatedAt(Instant.now());
                preparedEntities.add(entity);
            }

            List<HotelImageEntity> saved = hotelImageRepository.saveAll(preparedEntities);

            log.info("Upload {} ảnh thành công cho hotelId={}", saved.size(), hotelId);

            return saved.stream()
                    .map(img -> HotelImageResponse.builder()
                            .imageId(img.getId())
                            .url(img.getUrl())
                            .isCover(img.getIsCover())
                            .build())
                    .toList();

        } catch (RuntimeException e) {
            // Fail-fast: bất kỳ lỗi nào (validate MinIO, DB...) đều rollback
            // toàn bộ object đã lỡ upload lên MinIO trong batch này.
            rollbackUploadedObjects(uploadedObjectNames);
            throw e;
        }
    }

    private void rollbackUploadedObjects(List<String> objectNames) {
        for (String objectName : objectNames) {
            try {
                minioStorageService.deleteFile(bucketName, objectName);
            } catch (Exception cleanupEx) {
                // Best-effort: log lại để dọn tay sau, không được ném tiếp
                // (sẽ che mất exception gốc là lý do thật sự gây fail).
                log.error("Rollback thất bại, còn sót object mồ côi trên MinIO: {}", objectName, cleanupEx);
            }
        }
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'HOTEL_OWNER')")
    public void deleteImages(UUID hotelId, UUID imageId) {
        HotelEntity hotel = hotelRepository.findById(hotelId)
                .filter(h -> !Boolean.TRUE.equals(h.getIsDeleted()))
                .orElseThrow(() -> new AppException(
                        "HOTEL_NOT_FOUND",
                        "Không tìm thấy hotel id: " + hotelId,
                        HttpStatus.NOT_FOUND));

        // PLATFORM_ADMIN được xoá ảnh của bất kỳ hotel nào.
        // HOTEL_OWNER chỉ được xoá ảnh của ĐÚNG hotel mình sở hữu.
        if (!securityUtils.hasRole("PLATFORM_ADMIN")) {
            UUID currentTenantId = securityUtils.getCurrentTenantId();
            if (!hotel.getTenantId().equals(currentTenantId)) {
                throw new AppException(
                        "NOT_HOTEL_OWNER",
                        "Bạn không phải chủ sở hữu khách sạn này",
                        HttpStatus.FORBIDDEN);
            }
        }

        HotelImageEntity image = hotelImageRepository.findById(imageId)
                .filter(img -> !Boolean.TRUE.equals(img.getIsDeleted()))
                .filter(img -> img.getHotel().getId().equals(hotelId))
                .orElseThrow(() -> new AppException(
                        "IMAGE_NOT_FOUND",
                        "Không tìm thấy ảnh id: " + imageId + " thuộc hotel: " + hotelId,
                        HttpStatus.NOT_FOUND));

        boolean wasCover = Boolean.TRUE.equals(image.getIsCover());
        String objectNameToDelete = image.getObjectName();

        // 1) Soft delete trước - đây là thay đổi mang tính "chân lý nghiệp vụ":
        // ngay khi dòng này được lưu, ảnh coi như không còn tồn tại với người dùng,
        // bất kể bước xoá vật lý trên MinIO ở dưới có thành công hay không.
        image.setIsDeleted(true);
        image.setIsCover(false);
        hotelImageRepository.save(image);

        // 2) Nếu ảnh vừa xoá là cover, tự động chọn ảnh còn lại CŨ NHẤT làm cover mới.
        if (wasCover) {
            hotelImageRepository
                    .findFirstByHotel_IdAndIsDeletedFalseOrderByCreatedAtAsc(hotelId)
                    .ifPresent(nextCover -> {
                        nextCover.setIsCover(true);
                        hotelImageRepository.save(nextCover);
                    });
            // Nếu hotel không còn ảnh nào khác -> không có cover, hoàn toàn hợp lệ.
        }

        // 3) Xoá vật lý trên MinIO SAU CÙNG. Nếu bước này lỗi, exception sẽ làm
        // rollback toàn bộ transaction (kể cả soft-delete + đổi cover ở trên) -
        // coi như "chưa xoá gì cả", để không xảy ra tình huống nửa vời (DB nói
        // đã xoá nhưng file vật lý vẫn còn, hoặc ngược lại).
        try {
            minioStorageService.deleteFile(bucketName, objectNameToDelete);
        } catch (Exception e) {
            log.error("Xoá object trên MinIO thất bại: bucket={}, object={}", bucketName, objectNameToDelete, e);
            throw new AppException(
                    "IMAGE_DELETE_FAILED",
                    "Xoá ảnh trên storage thất bại, vui lòng thử lại",
                    HttpStatus.BAD_GATEWAY);
        }

        log.info("Đã xoá ảnh id={} (hotelId={}, wasCover={})", imageId, hotelId, wasCover);
    }
}
