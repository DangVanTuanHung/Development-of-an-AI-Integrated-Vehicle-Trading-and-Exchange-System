package com.ebike.marketplaceModule.controller;

import com.ebike.marketplaceModule.dto.CreateSellerReviewRequest;
import com.ebike.marketplaceModule.service.MarketplaceSellerReviewService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/marketplace")
public class MarketplaceSellerReviewController {
    private final MarketplaceSellerReviewService service;

    public MarketplaceSellerReviewController(MarketplaceSellerReviewService service) {
        this.service = service;
    }

    @PostMapping("/transactions/{transactionId}/seller-review")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(
        Authentication authentication,
        @PathVariable UUID transactionId,
        @Valid @RequestBody CreateSellerReviewRequest request
    ) {
        return service.createReview(authentication.getName(), transactionId, request);
    }

    @GetMapping("/sellers/{sellerId}/reputation")
    public Map<String, Object> reputation(@PathVariable Long sellerId) {
        return service.reputation(sellerId);
    }
}
