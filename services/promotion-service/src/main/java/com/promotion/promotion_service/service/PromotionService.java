package com.promotion.promotion_service.service;

import com.promotion.promotion_service.constant.promotions.PromotionStatus;
import com.promotion.promotion_service.dto.request.CreatePromotionRequest;
import com.promotion.promotion_service.dto.request.UpdatePromotionRequest;
import com.promotion.promotion_service.dto.response.PromotionPageResponse;
import com.promotion.promotion_service.dto.response.PromotionResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PromotionService {

    PromotionResponse create(CreatePromotionRequest request);

    PromotionResponse update(UUID id, UpdatePromotionRequest request);

    PromotionResponse getById(UUID id);

    PromotionPageResponse getAll(String keyword,
                                 PromotionStatus status,
                                 Pageable pageable);


    void delete(UUID id);

    PromotionResponse changeStatus(UUID id, PromotionStatus status);

}