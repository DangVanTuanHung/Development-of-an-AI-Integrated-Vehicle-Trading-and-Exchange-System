package com.ebike.marketplaceModule.controller;

import com.ebike.marketplaceModule.dto.CreateAppointmentRequest;
import com.ebike.marketplaceModule.dto.UpdateAppointmentRequest;
import com.ebike.marketplaceModule.service.MarketplaceAppointmentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/marketplace")
public class MarketplaceAppointmentController {
    private final MarketplaceAppointmentService service;

    public MarketplaceAppointmentController(MarketplaceAppointmentService service) {
        this.service = service;
    }

    @PostMapping("/listings/{listingId}/appointments")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(Authentication authentication, @PathVariable UUID listingId, @Valid @RequestBody CreateAppointmentRequest request) {
        return service.create(authentication.getName(), listingId, request);
    }

    @GetMapping("/appointments/mine")
    public List<Map<String, Object>> mine(Authentication authentication) {
        return service.mine(authentication.getName());
    }

    @PatchMapping("/appointments/{appointmentId}")
    public Map<String, Object> update(Authentication authentication, @PathVariable UUID appointmentId, @Valid @RequestBody UpdateAppointmentRequest request) {
        return service.update(authentication.getName(), appointmentId, request);
    }
}
