package com.ebike.marketplaceModule.controller;

import com.ebike.marketplaceModule.service.MarketplaceConversationService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/marketplace")
public class MarketplaceConversationController {
    private final MarketplaceConversationService service;
    public MarketplaceConversationController(MarketplaceConversationService service) { this.service = service; }

    @PostMapping("/listings/{listingId}/conversation")
    public Map<String, Object> open(Authentication auth, @PathVariable UUID listingId) { return service.open(auth.getName(), listingId); }

    @GetMapping("/conversations")
    public List<Map<String, Object>> conversations(Authentication auth) { return service.conversations(auth.getName()); }

    @GetMapping("/conversations/{conversationId}/messages")
    public List<Map<String, Object>> messages(Authentication auth, @PathVariable UUID conversationId) { return service.messages(auth.getName(), conversationId); }

    @PostMapping("/conversations/{conversationId}/messages")
    public Map<String, Object> send(Authentication auth, @PathVariable UUID conversationId, @RequestBody Map<String, String> body) {
        return service.send(auth.getName(), conversationId, body.get("content"));
    }

    @PostMapping(value = "/conversations/{conversationId}/attachments", consumes = "multipart/form-data")
    public Map<String, Object> attachment(Authentication auth, @PathVariable UUID conversationId, @RequestPart("file") MultipartFile file) {
        return service.sendAttachment(auth.getName(), conversationId, file);
    }

    @PostMapping("/conversations/{conversationId}/location")
    public Map<String, Object> location(Authentication auth, @PathVariable UUID conversationId, @RequestBody Map<String, Double> body) {
        return service.sendLocation(auth.getName(), conversationId, body.get("latitude"), body.get("longitude"));
    }

    @PostMapping("/conversations/{conversationId}/typing")
    public void typing(Authentication auth, @PathVariable UUID conversationId, @RequestBody Map<String, Boolean> body) {
        service.setTyping(auth.getName(), conversationId, Boolean.TRUE.equals(body.get("typing")));
    }

    @GetMapping("/conversations/{conversationId}/typing")
    public Map<String, Boolean> typingStatus(Authentication auth, @PathVariable UUID conversationId) {
        return service.typingStatus(auth.getName(), conversationId);
    }
}
