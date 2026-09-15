package com.ebike.marketplaceModule.controller;
import com.ebike.marketplaceModule.service.MarketplaceOperationsService;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/manager/marketplace")
@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
public class MarketplaceOperationsController {
    private final MarketplaceOperationsService service;
    public MarketplaceOperationsController(MarketplaceOperationsService service) { this.service = service; }
    @GetMapping("/overview") public Map<String,Object> overview() { return service.overview(); }
    @GetMapping("/listings") public List<Map<String,Object>> listings() { return service.listings(); }
    @PostMapping("/listings/{id}/moderate") public Map<String,Object> moderate(Authentication auth, @PathVariable UUID id, @RequestBody Map<String,String> body) { return service.moderate(auth.getName(), id, body); }
    @GetMapping("/reports") public List<Map<String,Object>> reports() { return service.reports(); }
    @PostMapping("/reports/{id}/resolve") public Map<String,Object> resolve(Authentication auth, @PathVariable Long id, @RequestBody Map<String,String> body) { return service.resolve(auth.getName(), id, body); }
    @PostMapping("/transactions/{id}/resume") public Map<String,Object> resume(Authentication auth, @PathVariable UUID id, @RequestBody Map<String,String> body) { return service.resume(auth.getName(), id, body.get("note")); }
    @GetMapping("/transactions") public List<Map<String,Object>> transactions() { return service.transactions(); }
    @PostMapping("/transactions/{id}/dispute") public Map<String,Object> dispute(Authentication auth, @PathVariable UUID id, @RequestBody Map<String,String> body) { return service.dispute(auth.getName(), id, body.get("note")); }
}
