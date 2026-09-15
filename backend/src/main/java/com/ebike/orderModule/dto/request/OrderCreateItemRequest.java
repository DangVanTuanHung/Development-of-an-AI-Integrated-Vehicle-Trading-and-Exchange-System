package com.ebike.orderModule.dto.request;

public record OrderCreateItemRequest(Long productId, Integer quantity) {
}
