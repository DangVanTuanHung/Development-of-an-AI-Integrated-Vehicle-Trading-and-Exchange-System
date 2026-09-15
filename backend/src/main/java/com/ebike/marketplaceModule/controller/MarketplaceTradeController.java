package com.ebike.marketplaceModule.controller;

import com.ebike.marketplaceModule.dto.CreateOfferRequest;
import com.ebike.marketplaceModule.dto.CreatePaymentRequest;
import com.ebike.marketplaceModule.service.MarketplaceTradeService;
import jakarta.validation.Valid;
import java.util.List;
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
public class MarketplaceTradeController {
    private final MarketplaceTradeService service;
    public MarketplaceTradeController(MarketplaceTradeService service) { this.service = service; }

    @PostMapping("/listings/{listingId}/offers") @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> offer(Authentication auth, @PathVariable UUID listingId, @Valid @RequestBody CreateOfferRequest request) { return service.createOffer(auth.getName(), listingId, request); }
    @GetMapping("/offers/mine") public List<Map<String, Object>> offers(Authentication auth) { return service.myOffers(auth.getName()); }
    @PostMapping("/offers/{offerId}/accept") public Map<String, Object> accept(Authentication auth, @PathVariable UUID offerId) { return service.acceptOffer(auth.getName(), offerId); }
    @GetMapping("/transactions/mine") public List<Map<String, Object>> transactions(Authentication auth) { return service.myTransactions(auth.getName()); }
    @PostMapping("/transactions/{transactionId}/payments") @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> payment(Authentication auth, @PathVariable UUID transactionId, @Valid @RequestBody CreatePaymentRequest request) { return service.createPayment(auth.getName(), transactionId, request); }
    @PostMapping("/payments/{paymentId}/confirm")
    public Map<String, Object> confirmPayment(Authentication auth, @PathVariable UUID paymentId) { return service.confirmPayment(auth.getName(), paymentId); }
    @PostMapping("/transactions/{transactionId}/handover/confirm")
    public Map<String, Object> confirmHandover(Authentication auth, @PathVariable UUID transactionId) { return service.confirmHandover(auth.getName(), transactionId); }
}
