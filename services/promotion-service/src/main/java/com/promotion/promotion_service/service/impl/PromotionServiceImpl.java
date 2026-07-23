package com.promotion.promotion_service.service.impl;

import com.promotion.promotion_service.constant.promotions.PromotionDiscountType;
import com.promotion.promotion_service.constant.promotions.PromotionStatus;
import com.promotion.promotion_service.constant.scope.ScopeType;
import com.promotion.promotion_service.dto.request.*;
import com.promotion.promotion_service.dto.response.*;
import com.promotion.promotion_service.entity.CouponEntity;
import com.promotion.promotion_service.entity.PromotionConditionEntity;
import com.promotion.promotion_service.entity.PromotionEntity;
import com.promotion.promotion_service.entity.PromotionScopeEntity;
import com.promotion.promotion_service.exception.BusinessException;
import com.promotion.promotion_service.repository.CouponRepository;
import com.promotion.promotion_service.repository.PromotionConditionRepository;
import com.promotion.promotion_service.repository.PromotionRepository;
import com.promotion.promotion_service.repository.PromotionScopeRepository;
import com.promotion.promotion_service.service.PromotionService;
import com.promotion.promotion_service.exception.ResourceNotFoundException;
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
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class PromotionServiceImpl implements PromotionService {

    private CouponRepository couponRepository;
    private PromotionScopeRepository promotionScopeRepository;
    private PromotionConditionRepository promotionConditionRepository;
    private PromotionRepository promotionRepository;

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

        // 6. Trả về chi tiết Promotion
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

        // 7. Trả về chi tiết
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

    @Transactional
    public PromotionResponse changeStatus(UUID id, ChangePromotionStatusRequest request) {

        PromotionEntity promotion = promotionRepository
                .findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found"));

        validateStatusTransition(promotion.getStatus(), request.getStatus());

        promotion.setStatus(request.getStatus());

        promotionRepository.save(promotion);

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

        couponRepository.saveAll(entities);
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
                    existing.setStatus(input.getStatus());
                    existing.setUsageLimit(input.getUsageLimit());

                    couponRepository.save(existing);

                } else {

                    // Insert coupon mới
                    CouponEntity coupon = new CouponEntity();

                    coupon.setPromotion(promotion);
                    coupon.setCode(input.getCode());
                    coupon.setStatus(input.getStatus());
                    coupon.setUsageLimit(input.getUsageLimit());

                    couponRepository.save(coupon);
                }
            }
        }

        // Soft delete coupon không còn trong request
        existingCouponMap.values()
                .forEach(couponRepository::delete);
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
}
