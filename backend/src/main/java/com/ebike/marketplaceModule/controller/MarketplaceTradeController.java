package com.ebike.marketplaceModule.controller;

import com.ebike.marketplaceModule.dto.*;
import com.ebike.marketplaceModule.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/marketplace")
public class MarketplaceTradeController {
    private final MarketplaceOfferService offers;
    private final MarketplaceTransactionService trades;
    public MarketplaceTradeController(MarketplaceOfferService offers,MarketplaceTransactionService trades){this.offers=offers;this.trades=trades;}
    public record Agreement(boolean policyAccepted) {}
    public record Note(@NotBlank @Size(max=2000) String note) {}
    public record Reference(@NotBlank @Size(max=160) String reference) {}
    public record Plan(@NotNull OffsetDateTime scheduledAt,@NotBlank @Size(max=500) String location,@Size(max=2000) String note,@NotNull @Min(0) Integer revision) {}
    public record Revision(@NotNull @Min(0) Integer revision) {}
    @PostMapping("/listings/{id}/offers") @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> create(Authentication auth,@PathVariable UUID id,@Valid @RequestBody CreateOfferRequest body){return offers.create(auth.getName(),id,body);}
    @GetMapping("/offers/mine") public List<Map<String,Object>> offers(Authentication auth){return offers.mine(auth.getName());}
    @PostMapping("/offers/{id}/accept") public Map<String,Object> accept(Authentication auth,@PathVariable UUID id,@RequestBody Agreement body){return offers.respond(auth.getName(),id,"accept",null,body.policyAccepted());}
    @PostMapping("/offers/{id}/counter") public Map<String,Object> counter(Authentication auth,@PathVariable UUID id,@Valid @RequestBody CreateOfferRequest body){return offers.respond(auth.getName(),id,"counter",body,false);}
    @PostMapping("/offers/{id}/reject") public Map<String,Object> reject(Authentication auth,@PathVariable UUID id){return offers.respond(auth.getName(),id,"reject",null,false);}
    @PostMapping("/offers/{id}/withdraw") public Map<String,Object> withdraw(Authentication auth,@PathVariable UUID id){return offers.respond(auth.getName(),id,"withdraw",null,false);}
    @GetMapping("/transactions/mine") public List<Map<String,Object>> transactions(Authentication auth){return trades.mine(auth.getName());}
    @GetMapping("/transactions/{id}") public Map<String,Object> detail(Authentication auth,@PathVariable UUID id){return trades.detail(auth.getName(),id);}
    @PostMapping("/transactions/{id}/payments") public Map<String,Object> payment(Authentication auth,@PathVariable UUID id,@Valid @RequestBody CreatePaymentRequest body){return trades.createPayment(auth.getName(),id,body.stage(),body.provider());}
    @PostMapping("/payments/{id}/sent") public Map<String,Object> sent(Authentication auth,@PathVariable UUID id,@Valid @RequestBody Reference body){return trades.payment(auth.getName(),id,"sent",body.reference());}
    @PostMapping("/payments/{id}/confirm") public Map<String,Object> received(Authentication auth,@PathVariable UUID id){return trades.payment(auth.getName(),id,"confirm",null);}
    @PostMapping("/payments/{id}/cancel") public Map<String,Object> cancelPayment(Authentication auth,@PathVariable UUID id){return trades.payment(auth.getName(),id,"cancel",null);}
    @PostMapping("/transactions/{id}/cancellation/request") public Map<String,Object> cancel(Authentication auth,@PathVariable UUID id,@Valid @RequestBody Note body){return trades.cancellation(auth.getName(),id,"request",body.note());}
    @PostMapping("/transactions/{id}/cancellation/{action:approve|reject|withdraw}") public Map<String,Object> cancelDecision(Authentication auth,@PathVariable UUID id,@PathVariable String action){return trades.cancellation(auth.getName(),id,action,null);}
    @PostMapping("/transactions/{id}/refunds/{refund}/sent") public Map<String,Object> refundSent(Authentication auth,@PathVariable UUID id,@PathVariable UUID refund,@Valid @RequestBody Reference body){return trades.refund(auth.getName(),id,refund,"sent",body.reference());}
    @PostMapping("/transactions/{id}/refunds/{refund}/received") public Map<String,Object> refundReceived(Authentication auth,@PathVariable UUID id,@PathVariable UUID refund){return trades.refund(auth.getName(),id,refund,"received",null);}
    @PostMapping("/transactions/{id}/disputes") public Map<String,Object> dispute(Authentication auth,@PathVariable UUID id,@Valid @RequestBody Note body){return trades.dispute(auth.getName(),id,body.note(),false);}
    @PostMapping("/transactions/{id}/handover/plan") public Map<String,Object> plan(Authentication auth,@PathVariable UUID id,@Valid @RequestBody Plan body){return trades.plan(auth.getName(),id,body.scheduledAt(),body.location(),body.note(),body.revision());}
    @PostMapping("/transactions/{id}/handover/{action:agree|confirm}") public Map<String,Object> handover(Authentication auth,@PathVariable UUID id,@PathVariable String action,@Valid @RequestBody Revision body){return trades.handover(auth.getName(),id,action,body.revision());}
}
