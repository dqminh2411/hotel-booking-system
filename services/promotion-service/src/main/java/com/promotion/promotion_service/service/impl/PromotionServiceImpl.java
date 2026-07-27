package com.promotion.promotion_service.service.impl;

import com.promotion.promotion_service.constant.coupons.CouponStatus;
import com.promotion.promotion_service.constant.promotions.PromotionDiscountType;
import com.promotion.promotion_service.constant.promotions.PromotionStatus;
import com.promotion.promotion_service.constant.scope.ScopeType;
import com.promotion.promotion_service.dto.event.CouponActiveNotificationEvent;
import com.promotion.promotion_service.dto.event.PromotionActiveNotificationEvent;
import com.promotion.promotion_service.dto.request.*;
import com.promotion.promotion_service.dto.response.*;
import com.promotion.promotion_service.entity.CouponEntity;
import com.promotion.promotion_service.entity.PromotionConditionEntity;
import com.promotion.promotion_service.entity.PromotionEntity;
import com.promotion.promotion_service.entity.PromotionScopeEntity;
import com.promotion.promotion_service.exception.ResourceNotFoundException;
import com.promotion.promotion_service.exception.BusinessException;
import com.promotion.promotion_service.constant.promotion_usage.PromotionUsageStatus;
import com.promotion.promotion_service.dto.kafka.*;
import com.promotion.promotion_service.entity.PromotionUsageEntity;
import com.promotion.promotion_service.repository.CouponRepository;
import com.promotion.promotion_service.repository.PromotionConditionRepository;
import com.promotion.promotion_service.repository.PromotionRepository;
import com.promotion.promotion_service.repository.PromotionScopeRepository;
import com.promotion.promotion_service.repository.PromotionUsageRepository;
import com.promotion.promotion_service.service.OutboxPublisherService;
import com.promotion.promotion_service.service.PromotionService;
import jakarta.ws.rs.BadRequestException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class PromotionServiceImpl implements PromotionService {

    private final CouponRepository couponRepository;
    private final PromotionScopeRepository promotionScopeRepository;
    private final PromotionConditionRepository promotionConditionRepository;
    private final PromotionRepository promotionRepository;
    private final PromotionUsageRepository promotionUsageRepository;
    private final OutboxPublisherService outboxPublisherService;


    @Override
    @Transactional
    public PromotionResponse create(CreatePromotionRequest request) {
        // 1. Validate business
        validateCreateRequest(request);

        // 2. Tạo Promotion
        PromotionEntity promotion = getPromotionEntity(request);

        promotion = promotionRepository.save(promotion);

        // 3. Lưu Scope
        saveScopes(promotion, request.getScopes());

        // 4. Lưu Condition
        saveConditions(promotion, request.getConditions());

        // 5. Lưu Coupon
        saveCoupons(promotion, request.getCoupons());

        // 6. Gửi outbox event nếu status == ACTIVE
        publishPromotionActiveEvent(promotion, true);

        // 7. Trả về chi tiết Promotion
        return getById(promotion.getId());
    }

    private static PromotionEntity getPromotionEntity(CreatePromotionRequest request) {
        PromotionEntity promotion = new PromotionEntity();

        promotion.setTenantId(request.getTenantId());

        promotion.setName(request.getName());
        promotion.setDescription(request.getDescription());

        promotion.setType(request.getType());
        promotion.setDiscountType(request.getDiscountType());

        promotion.setDiscountValue(request.getDiscountValue());
        promotion.setMaxDiscountAmount(request.getMaxDiscountAmount());

        promotion.setMinBookingAmount(request.getMinBookingAmount());
        promotion.setMinNights(request.getMinNights());

        promotion.setStartAt(request.getStartAt());
        promotion.setEndAt(request.getEndAt());

        promotion.setStatus(request.getStatus());

        promotion.setTotalUsageLimit(request.getTotalUsageLimit());
        promotion.setPerUserUsageLimit(request.getPerUserUsageLimit());

        promotion.setCurrentUsageCount(0);

        promotion.setStackable(Boolean.TRUE.equals(request.getStackable()));

        promotion.setDeleted(false);

        return promotion;
    }

    @Override
    @Transactional
    public PromotionResponse update(UUID id, UpdatePromotionRequest request) {

        // 1. Tìm promotion
        PromotionEntity promotion = promotionRepository
                .findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found"));

        // 2. Validate business
        validateUpdateRequest(promotion, request);

        // 3. Update Promotion
        updatePromotionEntity(promotion, request);

        promotion = promotionRepository.save(promotion);

        // 4. Replace Scope
        replaceScopes(promotion, request.getScopes());

        // 5. Replace Condition
        replaceConditions(promotion, request.getConditions());

        // 6. Sync Coupon
        syncCoupons(promotion, request.getCoupons());

        // 7. Gửi outbox event nếu status == ACTIVE
        publishPromotionActiveEvent(promotion, false);

        // 8. Trả về chi tiết
        return getById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionResponse getById(UUID id) {

        PromotionEntity promotion = promotionRepository
                .findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found"));

        PromotionResponse response = toResponse(promotion);

        /*// Promotion
        response.setId(promotion.getId());
        response.setName(promotion.getName());
        response.setDescription(promotion.getDescription());
        response.setType(promotion.getType());

        response.setDiscountType(promotion.getDiscountType());
        response.setDiscountValue(promotion.getDiscountValue());
        response.setMaxDiscountAmount(promotion.getMaxDiscountAmount());

        response.setMinBookingAmount(promotion.getMinBookingAmount());
        response.setMinNights(promotion.getMinNights());

        response.setStartAt(promotion.getStartAt());
        response.setEndAt(promotion.getEndAt());

        response.setStatus(promotion.getStatus());*/


        // Scope
        List<PromotionScopeResponse> scopes =
                promotionScopeRepository.findAllByPromotionId(promotion.getId())
                        .stream()
                        .map(this::toScopeResponse)
                        .toList();

        response.setScopes(scopes);

        // Condition
        List<PromotionConditionResponse> conditions =
                promotionConditionRepository.findAllByPromotionId(promotion.getId())
                        .stream()
                        .map(this::toConditionResponse)
                        .toList();

        response.setConditions(conditions);

        // Coupon
        List<CouponResponse> coupons =
                couponRepository.findAllByPromotionIdAndIsDeletedFalse(promotion.getId())
                        .stream()
                        .map(this::toCouponResponse)
                        .toList();

        response.setCoupons(coupons);

        return response;
    }

    private static PromotionResponse toResponse(PromotionEntity promotion) {

        PromotionResponse response = new PromotionResponse();

        response.setTenantId(promotion.getTenantId());

        response.setId(promotion.getId());
        response.setName(promotion.getName());
        response.setDescription(promotion.getDescription());
        response.setType(promotion.getType());

        response.setDiscountType(promotion.getDiscountType());
        response.setDiscountValue(promotion.getDiscountValue());
        response.setMaxDiscountAmount(promotion.getMaxDiscountAmount());

        response.setMinBookingAmount(promotion.getMinBookingAmount());
        response.setMinNights(promotion.getMinNights());

        response.setStartAt(promotion.getStartAt());
        response.setEndAt(promotion.getEndAt());

        response.setStatus(promotion.getStatus());

        response.setTotalUsageLimit(promotion.getTotalUsageLimit());
        response.setPerUserUsageLimit(promotion.getPerUserUsageLimit());

        response.setCurrentUsageCount(promotion.getCurrentUsageCount());

        response.setStackable(promotion.isStackable());

        response.setCreatedAt(promotion.getCreatedAt());
        response.setUpdatedAt(promotion.getUpdatedAt());

        response.setCreatedBy(promotion.getCreatedBy());
        response.setUpdatedBy(promotion.getUpdatedBy());

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionPageResponse getAll(String keyword,
                                         PromotionStatus status,
                                         Pageable pageable) {

        // XỬ LÝ CHUỖI RỖNG
        keyword = StringUtils.hasText(keyword)
                ? keyword.trim()
                : null;

        Page<PromotionEntity> page;

        if (keyword == null) {
            if (status == null) {
                page = promotionRepository.findAllByIsDeletedFalse(pageable);
            } else {
                page = promotionRepository.findAllByStatusAndIsDeletedFalse(status, pageable);
            }
        } else {
            page = promotionRepository.search(keyword, status, pageable);
        }
        List<PromotionResponse> contents = page.getContent()
                .stream()
                .map(this::toPromotionResponse)
                .toList();

        PromotionPageResponse response = new PromotionPageResponse();

        response.setContent(contents);
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());

        return response;
    }


    @Override
    @Transactional
    public void delete(UUID id) {

        PromotionEntity promotion = promotionRepository
                .findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found"));

        promotionRepository.delete(promotion);
    }

    @Override
    @Transactional
    public PromotionResponse changeStatus(UUID id, ChangePromotionStatusRequest request) {

        PromotionEntity promotion = promotionRepository
                .findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found"));

        validateStatusTransition(promotion.getStatus(), request.getStatus());

        promotion.setStatus(request.getStatus());

        promotion = promotionRepository.save(promotion);

        publishPromotionActiveEvent(promotion, false);

        return getById(id);
    }
    /*
       Validate
    * */

    private void validateCreateRequest(CreatePromotionRequest request) {

        validateDate(request.getStartAt(), request.getEndAt());

        validateDiscount(request.getDiscountType(), request.getDiscountValue());

        validateScopes(request.getScopes());

        validateCreateCoupons(request.getCoupons());

        validateConditions(request.getConditions());

    }

    private void validateUpdateRequest(PromotionEntity promotion,
                                       UpdatePromotionRequest request) {

        validateDate(request.getStartAt(), request.getEndAt());

        validateDiscount(request.getDiscountType(), request.getDiscountValue());

        validateScopes(request.getScopes());

        validateUpdateCoupons(promotion, request.getCoupons());

        validateConditions(request.getConditions());

        validateStatusTransition(
                promotion.getStatus(),
                request.getStatus());

    }

    private void validateDate(
            OffsetDateTime startAt,
            OffsetDateTime endAt) {

        if (startAt == null || endAt == null) {
            return; // @NotNull xử lý trước
        }

        if (!startAt.isBefore(endAt)) {
            throw new BadRequestException("Promotion startAt must be before endAt.");
        }
    }

    private void validateDiscount(
            PromotionDiscountType type,
            BigDecimal value
    ) {

        if (type == null || value == null) {
            return; // @NotNull xử lý
        }

        if (type == PromotionDiscountType.PERCENTAGE
                && value.compareTo(BigDecimal.valueOf(100)) > 0) {

            throw new BadRequestException(
                    "Percentage discount must not exceed 100.");
        }
    }



    private void validateScopes(List<PromotionScopeInput> scopes) {

        if (scopes == null) {
            return;
        }

        for (PromotionScopeInput scope : scopes) {

            if (scope.getScopeType() != ScopeType.SYSTEM) {
                throw new BusinessException(
                        "Only SYSTEM scope is supported.");
            }

            if (scope.getScopeRefId() != null) {
                throw new BusinessException(
                        "scopeRefId must be null for SYSTEM scope.");
            }
        }
    }

    private void validateCreateCoupons(List<CouponInput> coupons) {

        if (coupons == null || coupons.isEmpty()) {
            return;
        }

        Set<String> codes = new HashSet<>();

        for (CouponInput coupon : coupons) {

            if (!codes.add(coupon.getCode())) {

                throw new BusinessException(
                        "Duplicate coupon code: " + coupon.getCode());
            }

            if (couponRepository.existsByCodeAndIsDeletedFalse(coupon.getCode())) {

                throw new BusinessException(
                        "Coupon code already exists: " + coupon.getCode());
            }
        }
    }

    private void validateUpdateCoupons(PromotionEntity promotion,
                                       List<CouponInput> coupons) {

        if (coupons == null || coupons.isEmpty()) {
            return;
        }

        // Các coupon hiện có của promotion
        Set<String> existingCodes = couponRepository
                .findAllByPromotionIdAndIsDeletedFalse(promotion.getId())
                .stream()
                .map(CouponEntity::getCode)
                .collect(Collectors.toSet());

        // Kiểm tra trùng trong request
        Set<String> requestCodes = new HashSet<>();

        for (CouponInput coupon : coupons) {

            String code = coupon.getCode();

            if (!requestCodes.add(code)) {
                throw new BadRequestException(
                        "Duplicate coupon code: " + code);
            }

            // Nếu là coupon mới phải đảm bảo code chưa tồn tại toàn hệ thống
            if (!existingCodes.contains(code)
                    && couponRepository.existsByCodeAndIsDeletedFalse(code)) {

                throw new BadRequestException(
                        "Coupon code already exists: " + code);
            }
        }
    }

    private void validateConditions(
            List<PromotionConditionInput> conditions) {

        if (conditions == null) {
            return;
        }

        // reserved for future business validation
    }

    private void validateStatusTransition(
            PromotionStatus current,
            PromotionStatus target) {

        if (current == PromotionStatus.EXPIRED
                && target == PromotionStatus.ACTIVE) {

            throw new BadRequestException(
                    "Expired promotion cannot be activated.");
        }
    }

    /*
        Save
    */

    private void saveScopes(PromotionEntity promotion,
                            List<PromotionScopeInput> scopes) {

        if (scopes == null || scopes.isEmpty()) {
            return;
        }

        List<PromotionScopeEntity> entities = new ArrayList<>();

        for (PromotionScopeInput input : scopes) {

            PromotionScopeEntity entity = new PromotionScopeEntity();

            entity.setPromotion(promotion);
            entity.setScopeType(input.getScopeType());
            entity.setScopeRefId(input.getScopeRefId());

            entities.add(entity);
        }

        promotionScopeRepository.saveAll(entities);
    }

    private void saveConditions(PromotionEntity promotion,
                                List<PromotionConditionInput> conditions) {

        if (conditions == null || conditions.isEmpty()) {
            return;
        }

        List<PromotionConditionEntity> entities = new ArrayList<>();

        for (PromotionConditionInput input : conditions) {

            PromotionConditionEntity entity = new PromotionConditionEntity();

            entity.setPromotion(promotion);
            entity.setConditionType(input.getConditionType());
            entity.setOperator(input.getOperator());
            entity.setConditionValue(input.getConditionValue());

            entities.add(entity);
        }

        promotionConditionRepository.saveAll(entities);
    }

    private void saveCoupons(PromotionEntity promotion,
                             List<CouponInput> coupons) {

        if (coupons == null || coupons.isEmpty()) {
            return;
        }

        List<CouponEntity> entities = new ArrayList<>();

        for (CouponInput input : coupons) {

            CouponEntity entity = new CouponEntity();

            entity.setPromotion(promotion);
            entity.setCode(input.getCode());
            entity.setStatus(input.getStatus());
            entity.setUsageLimit(input.getUsageLimit());

            entities.add(entity);
        }

        entities = couponRepository.saveAll(entities);

        for (CouponEntity entity : entities) {
            publishCouponActiveEvent(promotion, entity, true);
        }
    }

    private void updatePromotionEntity(PromotionEntity promotion,
                                       UpdatePromotionRequest request) {

        promotion.setName(request.getName());
        promotion.setDescription(request.getDescription());

        promotion.setType(request.getType());
        promotion.setDiscountType(request.getDiscountType());

        promotion.setDiscountValue(request.getDiscountValue());
        promotion.setMaxDiscountAmount(request.getMaxDiscountAmount());

        promotion.setMinBookingAmount(request.getMinBookingAmount());
        promotion.setMinNights(request.getMinNights());

        promotion.setStartAt(request.getStartAt());
        promotion.setEndAt(request.getEndAt());
    }

    /*
        REPLACE
    */

    private void replaceScopes(PromotionEntity promotion,
                               List<PromotionScopeInput> scopes) {

        // Xóa toàn bộ scope hiện tại
        promotionScopeRepository.deleteAllByPromotionId(promotion.getId());

        // Lưu lại danh sách mới
        saveScopes(promotion, scopes);
    }

    private void replaceConditions(PromotionEntity promotion,
                                   List<PromotionConditionInput> conditions) {

        // Xóa toàn bộ condition hiện tại
        promotionConditionRepository.deleteAllByPromotionId(promotion.getId());

        // Lưu danh sách condition mới
        saveConditions(promotion, conditions);
    }

    private void syncCoupons(PromotionEntity promotion,
                             List<CouponInput> coupons) {

        // Coupon hiện có của promotion
        List<CouponEntity> existingCoupons =
                couponRepository.findAllByPromotionIdAndIsDeletedFalse(promotion.getId());

        Map<String, CouponEntity> existingCouponMap = existingCoupons.stream()
                .collect(Collectors.toMap(
                        CouponEntity::getCode,
                        Function.identity()
                ));

        if (coupons != null) {

            for (CouponInput input : coupons) {

                CouponEntity existing = existingCouponMap.remove(input.getCode());

                if (existing != null) {

                    // Update coupon hiện có
                    CouponStatus oldStatus = existing.getStatus();
                    existing.setStatus(input.getStatus());
                    existing.setUsageLimit(input.getUsageLimit());

                    existing = couponRepository.save(existing);

                    if (input.getStatus() == CouponStatus.ACTIVE) {
                        boolean isNewActive = oldStatus != CouponStatus.ACTIVE;
                        publishCouponActiveEvent(promotion, existing, isNewActive);
                    }

                } else {

                    // Insert coupon mới
                    CouponEntity coupon = new CouponEntity();

                    coupon.setPromotion(promotion);
                    coupon.setCode(input.getCode());
                    coupon.setStatus(input.getStatus());
                    coupon.setUsageLimit(input.getUsageLimit());

                    coupon = couponRepository.save(coupon);

                    publishCouponActiveEvent(promotion, coupon, true);
                }
            }
        }

        // Soft delete coupon không còn trong request
        existingCouponMap.values()
                .forEach(couponRepository::delete);
    }

    private void publishPromotionActiveEvent(PromotionEntity promotion, boolean isNew) {
        if (promotion.getStatus() == PromotionStatus.ACTIVE) {
            String eventType = isNew ? "PromotionActiveCreated" : "PromotionActiveUpdated";
            PromotionActiveNotificationEvent event = PromotionActiveNotificationEvent.builder()
                    .eventId(UUID.randomUUID())
                    .eventType(eventType)
                    .promotionId(promotion.getId())
                    .name(promotion.getName())
                    .description(promotion.getDescription())
                    .type(promotion.getType())
                    .discountType(promotion.getDiscountType())
                    .discountValue(promotion.getDiscountValue())
                    .maxDiscountAmount(promotion.getMaxDiscountAmount())
                    .startAt(promotion.getStartAt())
                    .endAt(promotion.getEndAt())
                    .scopeType(ScopeType.SYSTEM.name())
                    .occurredAt(OffsetDateTime.now())
                    .build();
            outboxPublisherService.saveOutboxMessage("promotion-active-notification", event, eventType);
        }
    }

    private void publishCouponActiveEvent(PromotionEntity promotion, CouponEntity coupon, boolean isNew) {
        if (coupon.getStatus() == CouponStatus.ACTIVE) {
            String eventType = isNew ? "CouponActiveCreated" : "CouponActiveUpdated";
            CouponActiveNotificationEvent event = CouponActiveNotificationEvent.builder()
                    .eventId(UUID.randomUUID())
                    .eventType(eventType)
                    .couponId(coupon.getId())
                    .promotionId(promotion.getId())
                    .code(coupon.getCode())
                    .promotionName(promotion.getName())
                    .promotionDescription(promotion.getDescription())
                    .discountType(promotion.getDiscountType())
                    .discountValue(promotion.getDiscountValue())
                    .usageLimit(coupon.getUsageLimit())
                    .occurredAt(OffsetDateTime.now())
                    .build();
            outboxPublisherService.saveOutboxMessage("coupon-active-notification", event, eventType);
        }
    }

    /*
        READ SUPPORT METHOD
    * */

    private PromotionScopeResponse toScopeResponse(PromotionScopeEntity entity) {

        PromotionScopeResponse response = new PromotionScopeResponse();

        response.setScopeType(entity.getScopeType());
        response.setScopeRefId(entity.getScopeRefId());

        return response;
    }

    private PromotionConditionResponse toConditionResponse(PromotionConditionEntity entity) {

        PromotionConditionResponse response = new PromotionConditionResponse();

        response.setConditionType(entity.getConditionType());
        response.setOperator(entity.getOperator());
        response.setConditionValue(entity.getConditionValue());

        return response;
    }

    private CouponResponse toCouponResponse(CouponEntity entity) {

        CouponResponse response = new CouponResponse();

        response.setCode(entity.getCode());
        response.setStatus(entity.getStatus());
        response.setUsageLimit(entity.getUsageLimit());
        response.setCurrentUsageCount(entity.getCurrentUsageCount());

        return response;
    }

    private PromotionResponse toPromotionResponse(PromotionEntity entity) {

        PromotionResponse response = new PromotionResponse();

        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());

        response.setType(entity.getType());

        response.setDiscountType(entity.getDiscountType());
        response.setDiscountValue(entity.getDiscountValue());

        response.setMaxDiscountAmount(entity.getMaxDiscountAmount());

        response.setMinBookingAmount(entity.getMinBookingAmount());

        response.setMinNights(entity.getMinNights());

        response.setStartAt(entity.getStartAt());
        response.setEndAt(entity.getEndAt());

        response.setStatus(entity.getStatus());

        return response;
    }

    private BigDecimal calculateDiscount(PromotionEntity promotion, BigDecimal totalAmount) {
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal discount = BigDecimal.ZERO;
        if (promotion.getDiscountType() == PromotionDiscountType.PERCENTAGE) {
            discount = totalAmount.multiply(promotion.getDiscountValue()).divide(BigDecimal.valueOf(100));
            if (promotion.getMaxDiscountAmount() != null && discount.compareTo(promotion.getMaxDiscountAmount()) > 0) {
                discount = promotion.getMaxDiscountAmount();
            }
        } else if (promotion.getDiscountType() == PromotionDiscountType.FIXED_AMOUNT) {
            discount = promotion.getDiscountValue();
            if (discount.compareTo(totalAmount) > 0) {
                discount = totalAmount;
            }
        }
        return discount;
    }

    @Override
    @Transactional(readOnly = true)
    public ValidatePromotionPreResponse validatePre(ValidatePromotionPreRequest request) {
        CouponEntity coupon = couponRepository.findByCodeAndIsDeletedFalse(request.getCouponCode())
                .orElseThrow(() -> new BusinessException("Coupon không tồn tại"));

        if (!CouponStatus.ACTIVE.equals(coupon.getStatus())) {
            throw new BusinessException("Coupon không còn hoạt động");
        }

        PromotionEntity promotion = coupon.getPromotion();
        if (promotion.isDeleted() || promotion.getStatus() != PromotionStatus.ACTIVE) {
            throw new BusinessException("Chương trình khuyến mãi không còn hoạt động");
        }

        OffsetDateTime now = OffsetDateTime.now();
        if (now.isBefore(promotion.getStartAt()) || now.isAfter(promotion.getEndAt())) {
            throw new BusinessException("Chương trình khuyến mãi đã hết hạn hoặc chưa bắt đầu");
        }

        if (promotion.getMinBookingAmount() != null && request.getTotalAmount().compareTo(promotion.getMinBookingAmount()) < 0) {
            throw new BusinessException("Tổng tiền không đủ điều kiện áp dụng mã giảm giá");
        }

        validateScope(promotion, request.getHotelId(), request.getRoomTypeIds());

        if (promotion.getMinNights() != null && request.getCheckin() != null && request.getCheckout() != null) {
            try {
                LocalDate checkinDate = LocalDate.parse(request.getCheckin());
                LocalDate checkoutDate = LocalDate.parse(request.getCheckout());
                long nights = ChronoUnit.DAYS.between(checkinDate, checkoutDate);
                if (nights < promotion.getMinNights()) {
                    throw new BusinessException("Số đêm lưu trú tối thiểu là " + promotion.getMinNights() + " đêm");
                }
            } catch (Exception e) {
                if (e instanceof BusinessException) throw e;
            }
        }

        List<PromotionUsageStatus> activeStatuses = List.of(PromotionUsageStatus.RESERVED, PromotionUsageStatus.CONFIRMED);
        if (coupon.getUsageLimit() != null) {
            long couponUsages = promotionUsageRepository.countByCouponIdAndStatusIn(coupon.getId(), activeStatuses);
            if (couponUsages >= coupon.getUsageLimit()) {
                throw new BusinessException("Coupon đã hết lượt sử dụng");
            }
        }

        if (promotion.getTotalUsageLimit() != null) {
            long promoUsages = promotionUsageRepository.countByPromotionIdAndStatusIn(promotion.getId(), activeStatuses);
            if (promoUsages >= promotion.getTotalUsageLimit()) {
                throw new BusinessException("Mã khuyến mãi đã hết lượt sử dụng");
            }
        }

        if (promotion.getPerUserUsageLimit() != null && request.getUserId() != null) {
            long userUsages = promotionUsageRepository.countByUserIdAndPromotionIdAndStatusIn(request.getUserId(), promotion.getId(), activeStatuses);
            if (userUsages >= promotion.getPerUserUsageLimit()) {
                throw new BusinessException("Bạn đã dùng hết lượt cho mã giảm giá này");
            }
        }

        BigDecimal discountAmount = calculateDiscount(promotion, request.getTotalAmount());
        BigDecimal finalAmount = request.getTotalAmount().subtract(discountAmount).max(BigDecimal.ZERO);

        String desc = promotion.getDiscountType() == PromotionDiscountType.PERCENTAGE
                ? "Giảm " + promotion.getDiscountValue() + "%"
                : "Giảm " + promotion.getDiscountValue() + " VNĐ";

        return ValidatePromotionPreResponse.builder()
                .promotionId(promotion.getId())
                .couponId(coupon.getId())
                .couponCode(coupon.getCode())
                .promotionName(promotion.getName())
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .discountDescription(desc)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CouponDetailResponse getCouponDetail(String code, BigDecimal totalAmount, UUID hotelId) {
        CouponEntity coupon = couponRepository.findByCodeAndIsDeletedFalse(code)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon không tồn tại"));

        PromotionEntity promotion = coupon.getPromotion();

        Integer remainingUsages = null;
        if (coupon.getUsageLimit() != null) {
            remainingUsages = Math.max(0, coupon.getUsageLimit() - coupon.getCurrentUsageCount());
        }

        BigDecimal discountAmount = null;
        BigDecimal finalAmount = null;
        if (totalAmount != null && totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            discountAmount = calculateDiscount(promotion, totalAmount);
            finalAmount = totalAmount.subtract(discountAmount).max(BigDecimal.ZERO);
        }

        return CouponDetailResponse.builder()
                .couponId(coupon.getId())
                .code(coupon.getCode())
                .status(coupon.getStatus())
                .usageLimit(coupon.getUsageLimit())
                .currentUsageCount(coupon.getCurrentUsageCount())
                .remainingUsages(remainingUsages)
                .promotionId(promotion.getId())
                .promotionName(promotion.getName())
                .promotionDescription(promotion.getDescription())
                .discountType(promotion.getDiscountType())
                .discountValue(promotion.getDiscountValue())
                .maxDiscountAmount(promotion.getMaxDiscountAmount())
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .build();
    }

    @Override
    @Transactional
    public void handleValidatePromotion(ValidatePromotionCommand command) {
        String idempotencyKey = command.getSagaId() + ":" + command.getBookingId();
        // nếu có yêu cầu dùng promotion bị trùng
        if (promotionUsageRepository.existsByIdempotencyKey(idempotencyKey)) {
            Optional<PromotionUsageEntity> existing = promotionUsageRepository.findByBookingId(command.getBookingId());

            if (existing.isPresent() && existing.get().getStatus() != PromotionUsageStatus.CANCELLED) {
                PromotionUsageEntity u = existing.get();
                BigDecimal finalAmt = command.getTotalAmount().subtract(u.getDiscountAmount()).max(BigDecimal.ZERO);
                PromotionValidatedEvent event = PromotionValidatedEvent.builder()
                        .sagaId(command.getSagaId())
                        .bookingId(command.getBookingId())
                        .promotionId(u.getPromotion().getId())
                        .couponId(u.getCoupon() != null ? u.getCoupon().getId() : null)
                        .discountAmount(u.getDiscountAmount())
                        .finalAmount(finalAmt)
                        .build();
                outboxPublisherService.saveOutboxMessage("promotion-events", event, "PromotionValidated");
                return;
            }
        }

        try {
            CouponEntity coupon = couponRepository.findByCodeForUpdate(command.getCouponCode())
                    .orElseThrow(() -> new BusinessException("Coupon không tồn tại: " + command.getCouponCode()));

            if (!CouponStatus.ACTIVE.equals(coupon.getStatus())) {
                throw new BusinessException("Coupon không còn hoạt động");
            }

            PromotionEntity promotion = promotionRepository.findByIdForUpdate(coupon.getPromotion().getId())
                    .orElseThrow(() -> new BusinessException("Promotion không tồn tại"));

            if (promotion.isDeleted() || promotion.getStatus() != PromotionStatus.ACTIVE) {
                throw new BusinessException("Chương trình khuyến mãi không còn hoạt động");
            }

            OffsetDateTime now = OffsetDateTime.now();
            if (now.isBefore(promotion.getStartAt()) || now.isAfter(promotion.getEndAt())) {
                throw new BusinessException("Chương trình khuyến mãi đã hết hạn");
            }

            if (promotion.getMinBookingAmount() != null && command.getTotalAmount().compareTo(promotion.getMinBookingAmount()) < 0) {
                throw new BusinessException("Tổng tiền không đủ điều kiện tối thiểu");
            }

            validateScope(promotion, command.getHotelId(), command.getRoomTypeIds());

            List<PromotionUsageStatus> activeStatuses = List.of(PromotionUsageStatus.RESERVED, PromotionUsageStatus.CONFIRMED);
            if (coupon.getUsageLimit() != null) {
                long count = promotionUsageRepository.countByCouponIdAndStatusIn(coupon.getId(), activeStatuses);
                if (count >= coupon.getUsageLimit()) {
                    throw new BusinessException("Coupon đã hết lượt sử dụng");
                }
            }

            if (promotion.getTotalUsageLimit() != null) {
                long count = promotionUsageRepository.countByPromotionIdAndStatusIn(promotion.getId(), activeStatuses);
                if (count >= promotion.getTotalUsageLimit()) {
                    throw new BusinessException("Chương trình khuyến mãi đã hết lượt sử dụng");
                }
            }

            if (promotion.getPerUserUsageLimit() != null && command.getUserId() != null) {
                long count = promotionUsageRepository.countByUserIdAndPromotionIdAndStatusIn(command.getUserId(), promotion.getId(), activeStatuses);
                if (count >= promotion.getPerUserUsageLimit()) {
                    throw new BusinessException("Bạn đã dùng hết lượt cho mã giảm giá này");
                }
            }

            BigDecimal discountAmount = calculateDiscount(promotion, command.getTotalAmount());
            BigDecimal finalAmount = command.getTotalAmount().subtract(discountAmount).max(BigDecimal.ZERO);

            PromotionUsageEntity usage = new PromotionUsageEntity();
            usage.setPromotion(promotion);
            usage.setCoupon(coupon);
            usage.setBookingId(command.getBookingId());
            usage.setUserId(command.getUserId());
            usage.setHotelId(command.getHotelId() != null ? command.getHotelId() : UUID.randomUUID());
            usage.setDiscountAmount(discountAmount);
            usage.setBookingAmount(command.getTotalAmount());
            usage.setUsedAt(OffsetDateTime.now());
            usage.setStatus(PromotionUsageStatus.RESERVED);
            usage.setIdempotencyKey(idempotencyKey);
            promotionUsageRepository.save(usage);

            PromotionValidatedEvent event = PromotionValidatedEvent.builder()
                    .sagaId(command.getSagaId())
                    .bookingId(command.getBookingId())
                    .promotionId(promotion.getId())
                    .couponId(coupon.getId())
                    .discountAmount(discountAmount)
                    .finalAmount(finalAmount)
                    .build();
            outboxPublisherService.saveOutboxMessage("promotion-events", event, "PromotionValidated");

        } catch (Exception e) {
            PromotionRejectedEvent rejectedEvent = PromotionRejectedEvent.builder()
                    .sagaId(command.getSagaId())
                    .bookingId(command.getBookingId())
                    .reason(e.getMessage())
                    .build();
            outboxPublisherService.saveOutboxMessage("promotion-events", rejectedEvent, "PromotionRejected");
        }
    }

    @Override
    @Transactional
    public void handleConfirmPromotionUsage(ConfirmPromotionUsageCommand command) {
        Optional<PromotionUsageEntity> usageOpt = promotionUsageRepository.findByBookingIdAndStatus(command.getBookingId(), PromotionUsageStatus.RESERVED);
        if (usageOpt.isPresent()) {
            PromotionUsageEntity usage = usageOpt.get();

            CouponEntity coupon = couponRepository.findByCodeForUpdate(usage.getCoupon().getCode()).orElse(null);
            PromotionEntity promotion = promotionRepository.findByIdForUpdate(usage.getPromotion().getId()).orElse(null);

            usage.setStatus(PromotionUsageStatus.CONFIRMED);
            promotionUsageRepository.save(usage);

            if (promotion != null) {
                promotion.setCurrentUsageCount((promotion.getCurrentUsageCount() == null ? 0 : promotion.getCurrentUsageCount()) + 1);
                promotionRepository.save(promotion);
            }
            if (coupon != null) {
                coupon.setCurrentUsageCount((coupon.getCurrentUsageCount() == null ? 0 : coupon.getCurrentUsageCount()) + 1);
                couponRepository.save(coupon);
            }

            PromotionUsageConfirmedEvent event = PromotionUsageConfirmedEvent.builder()
                    .sagaId(command.getSagaId())
                    .bookingId(command.getBookingId())
                    .build();
            outboxPublisherService.saveOutboxMessage("promotion-events", event, "PromotionUsageConfirmed");
        } else {
            Optional<PromotionUsageEntity> existing = promotionUsageRepository.findByBookingId(command.getBookingId());
            if (existing.isPresent() && existing.get().getStatus() == PromotionUsageStatus.CONFIRMED) {
                PromotionUsageConfirmedEvent event = PromotionUsageConfirmedEvent.builder()
                        .sagaId(command.getSagaId())
                        .bookingId(command.getBookingId())
                        .build();
                outboxPublisherService.saveOutboxMessage("promotion-events", event, "PromotionUsageConfirmed");
            } else {
                PromotionUsageFailedEvent event = PromotionUsageFailedEvent.builder()
                        .sagaId(command.getSagaId())
                        .bookingId(command.getBookingId())
                        .reason("No reserved promotion usage found for bookingId: " + command.getBookingId())
                        .build();
                outboxPublisherService.saveOutboxMessage("promotion-events", event, "PromotionUsageFailed");
            }
        }
    }

    @Override
    @Transactional
    public void handleReleasePromotionUsage(ReleasePromotionUsageCommand command) {
        Optional<PromotionUsageEntity> usageOpt = promotionUsageRepository.findByBookingIdAndStatus(command.getBookingId(), PromotionUsageStatus.RESERVED);
        if (usageOpt.isPresent()) {
            PromotionUsageEntity usage = usageOpt.get();
            usage.setStatus(PromotionUsageStatus.CANCELLED);
            promotionUsageRepository.save(usage);
        }
    }

    private void validateScope(PromotionEntity promotion, UUID hotelId, List<UUID> roomTypeIds) {
        List<PromotionScopeEntity> scopes = promotionScopeRepository.findAllByPromotionId(promotion.getId());
        if (scopes == null || scopes.isEmpty()) {
            return;
        }

        boolean hasSystemScope = scopes.stream().anyMatch(s -> s.getScopeType() == ScopeType.SYSTEM);
        if (hasSystemScope) {
            return;
        }

        List<PromotionScopeEntity> hotelScopes = scopes.stream()
                .filter(s -> s.getScopeType() == ScopeType.HOTEL)
                .toList();
        if (!hotelScopes.isEmpty() && hotelId != null) {
            boolean matchHotel = hotelScopes.stream()
                    .anyMatch(s -> hotelId.equals(s.getScopeRefId()));
            if (!matchHotel) {
                throw new BusinessException("Mã giảm giá không áp dụng cho khách sạn này");
            }
        }

        List<PromotionScopeEntity> roomTypeScopes = scopes.stream()
                .filter(s -> s.getScopeType() == ScopeType.ROOM_TYPE)
                .toList();
        if (!roomTypeScopes.isEmpty() && roomTypeIds != null && !roomTypeIds.isEmpty()) {
            Set<UUID> allowedRoomTypeIds = roomTypeScopes.stream()
                    .map(PromotionScopeEntity::getScopeRefId)
                    .collect(Collectors.toSet());
            boolean matchRoomType = roomTypeIds.stream().anyMatch(allowedRoomTypeIds::contains);
            if (!matchRoomType) {
                throw new BusinessException("Mã giảm giá không áp dụng cho loại phòng đã chọn");
            }
        }
    }
}
