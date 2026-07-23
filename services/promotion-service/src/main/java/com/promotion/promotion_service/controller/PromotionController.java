package com.promotion.promotion_service.controller;

import com.promotion.promotion_service.constant.promotions.PromotionStatus;
import com.promotion.promotion_service.dto.request.ChangePromotionStatusRequest;
import com.promotion.promotion_service.dto.request.CreatePromotionRequest;
import com.promotion.promotion_service.dto.request.UpdatePromotionRequest;
import com.promotion.promotion_service.dto.response.PromotionPageResponse;
import com.promotion.promotion_service.dto.response.PromotionResponse;
import com.promotion.promotion_service.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
@Validated
public class PromotionController {

    private final PromotionService promotionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PromotionResponse createPromotion(
            @Valid @RequestBody CreatePromotionRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        return promotionService.create(request);
    }

    @PutMapping("/{id}")
    public PromotionResponse updatePromotion(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePromotionRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        return promotionService.update(id, request);
    }

    @GetMapping("/{id}")
    public PromotionResponse getPromotion(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        return promotionService.getById(id);
    }

    @GetMapping
    public PromotionPageResponse getPromotions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) PromotionStatus status,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {

        return promotionService.getAll(keyword, status, pageable);
    }

    @PatchMapping("/{id}/status")
    public PromotionResponse changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ChangePromotionStatusRequest request
      ) {
        return promotionService.changeStatus(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePromotion(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        promotionService.delete(id);
    }
}