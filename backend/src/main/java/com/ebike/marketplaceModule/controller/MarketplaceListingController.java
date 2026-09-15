package com.ebike.marketplaceModule.controller;

import com.ebike.marketplaceModule.dto.CreateListingRequest;
import com.ebike.marketplaceModule.dto.ListingResponse;
import com.ebike.marketplaceModule.service.MarketplaceListingService;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/marketplace")
public class MarketplaceListingController {
    private final MarketplaceListingService service;
    public MarketplaceListingController(MarketplaceListingService service) { this.service = service; }

    @GetMapping("/categories") public List<Map<String, Object>> categories() { return service.categories(); }
    @GetMapping("/listings") public List<ListingResponse> listings() { return service.publicListings(); }
    @GetMapping("/listings/{publicId}") public ListingResponse detail(@PathVariable UUID publicId, Authentication auth) { return service.visibleDetail(publicId, auth); }
    @GetMapping("/seller/listings") public List<ListingResponse> own(Authentication auth) { return service.ownListings(auth.getName()); }
    @PostMapping("/listings") @ResponseStatus(HttpStatus.CREATED)
    public ListingResponse create(Authentication auth, @Valid @RequestBody CreateListingRequest request) { return service.create(auth.getName(), request); }

    @PostMapping(value = "/listings/{publicId}/images", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> uploadImage(Authentication auth, @PathVariable UUID publicId,
                                            @RequestPart("file") MultipartFile file) {
        return service.uploadImage(auth.getName(), publicId, file);
    }

    @PostMapping("/listings/{publicId}/publish")
    public ListingResponse publish(Authentication auth, @PathVariable UUID publicId) {
        return service.publish(auth.getName(), publicId);
    }

    @PostMapping("/listings/{publicId}/edit")
    public ListingResponse edit(Authentication auth, @PathVariable UUID publicId, @Valid @RequestBody CreateListingRequest request) { return service.edit(auth.getName(),publicId,request); }
    @PostMapping("/listings/{publicId}/remove-image")
    public ListingResponse removeImage(Authentication auth, @PathVariable UUID publicId, @RequestBody Map<String,String> body) { return service.removeImage(auth.getName(),publicId,body.get("url")); }
    @PostMapping("/listings/{publicId}/withdraw")
    public ListingResponse withdraw(Authentication auth, @PathVariable UUID publicId) { return service.withdraw(auth.getName(),publicId); }

    @PostMapping("/listings/{publicId}/favorite")
    public Map<String, Object> favorite(Authentication auth, @PathVariable UUID publicId) {
        return service.toggleFavorite(auth.getName(), publicId);
    }

    @GetMapping("/listings/{publicId}/favorite")
    public Map<String, Object> favoriteStatus(Authentication auth, @PathVariable UUID publicId) {
        return service.favoriteStatus(auth.getName(), publicId);
    }

    @GetMapping("/favorites")
    public List<ListingResponse> favorites(Authentication auth) {
        return service.favoriteListings(auth.getName());
    }

    @PostMapping("/listings/{publicId}/report")
    public Map<String, Object> report(Authentication auth, @PathVariable UUID publicId, @RequestBody Map<String, String> body) {
        return service.report(auth.getName(), publicId, body);
    }
}
