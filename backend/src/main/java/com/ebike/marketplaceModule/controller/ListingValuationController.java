package com.ebike.marketplaceModule.controller;
import com.ebike.marketplaceModule.service.ListingValuationService;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/marketplace/listings")
public class ListingValuationController {
    private final ListingValuationService service;
    public ListingValuationController(ListingValuationService service) { this.service = service; }
    @GetMapping("/{publicId}/valuation")
    public ListingValuationService.Valuation estimate(@PathVariable UUID publicId) { return service.estimate(publicId); }
}
