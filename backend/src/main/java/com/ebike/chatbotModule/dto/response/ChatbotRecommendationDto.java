package com.ebike.chatbotModule.dto.response;

import java.math.BigDecimal;

public record ChatbotRecommendationDto(
    Long id,
    String name,
    String slug,
    BigDecimal price,
    BigDecimal discountPrice,
    Integer stockQuantity,
    String imageUrl,
    String categoryName,
    String reason
) {
}
