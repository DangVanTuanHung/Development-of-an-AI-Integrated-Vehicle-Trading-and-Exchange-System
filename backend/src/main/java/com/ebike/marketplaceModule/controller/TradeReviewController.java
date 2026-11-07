package com.ebike.marketplaceModule.controller;
import com.ebike.marketplaceModule.service.MarketplaceTransactionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/manager/marketplace/trades") @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class TradeReviewController {
    private final MarketplaceTransactionService trades;
    public TradeReviewController(MarketplaceTransactionService trades){this.trades=trades;}
    public record Resolution(@NotBlank String decision,@NotBlank @Size(max=2000) String note) {}
    @GetMapping public List<Map<String,Object>> all(Authentication auth){return trades.all(auth.getName());}
    @PostMapping("/{id}/resolve") public Map<String,Object> resolve(Authentication auth,@PathVariable UUID id,@Valid @RequestBody Resolution body){return trades.resolveDispute(auth.getName(),id,body.decision(),body.note());}
}
