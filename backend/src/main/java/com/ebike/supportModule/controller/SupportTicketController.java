package com.ebike.supportModule.controller;
import com.ebike.supportModule.dto.*;
import com.ebike.supportModule.service.SupportTicketService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController
public class SupportTicketController {
    private final SupportTicketService service; public SupportTicketController(SupportTicketService service){this.service=service;}
    @PostMapping("/support/tickets") @ResponseStatus(HttpStatus.CREATED) public SupportTicketResponse create(@RequestBody SupportTicketCreateRequest r, Authentication a){return service.create(r,a);}
    @GetMapping("/support/tickets/mine") public List<SupportTicketResponse> mine(Authentication a){return service.mine(a);}
    @GetMapping({"/admin/support-tickets","/manager/support-tickets"}) public List<SupportTicketResponse> all(@RequestParam(required=false) String status,@RequestParam(required=false) String search){return service.all(status,search);}
    @PatchMapping({"/admin/support-tickets/{id}","/manager/support-tickets/{id}"}) public SupportTicketResponse update(@PathVariable Long id,@RequestBody SupportTicketUpdateRequest r,Authentication a){return service.update(id,r,a);}
}
