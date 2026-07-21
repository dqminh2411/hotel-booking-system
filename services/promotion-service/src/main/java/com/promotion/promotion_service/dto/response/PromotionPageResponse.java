package com.promotion.promotion_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionPageResponse {

    private List<PromotionResponse> content;

    private Integer page;

    private Integer size;

    private Long totalElements;

    private Integer totalPages;

}