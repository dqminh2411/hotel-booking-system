package com.place_booking_service.client;

import com.place_booking_service.dto.ValidatePromotionPreRequest;
import com.place_booking_service.dto.ValidatePromotionPreResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "promotion-service")
public interface PromotionServiceClient {

    @PostMapping("/api/promotions/validate-pre")
    ValidatePromotionPreResponse validatePromotion(@RequestBody ValidatePromotionPreRequest request);
}
