package com.ebike.marketplaceModule.controller;

import com.ebike.marketplaceModule.dto.ListingResponse;
import com.ebike.marketplaceModule.service.ListingManagementService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/marketplace/listings/{id}")
public class ListingManagementController {
    private final ListingManagementService service;
    public ListingManagementController(ListingManagementService service) { this.service=service; }
    @PostMapping("/manage/{action}")
    public ListingResponse manage(Authentication auth,@PathVariable UUID id,@PathVariable String action) { return service.manage(auth.getName(),id,action); }
    @GetMapping("/statistics")
    public Map<String,Object> statistics(Authentication auth,@PathVariable UUID id) { return service.statistics(auth.getName(),id); }
    public record Engagement(String type) {}
    @PostMapping("/engagement") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void engagement(Authentication auth,@PathVariable UUID id,@RequestBody Engagement body,HttpServletRequest request) {
        service.engagement(id,auth==null?null:auth.getName(),body.type(),request.getRemoteAddr(),request.getHeader("User-Agent"));
    }
}
