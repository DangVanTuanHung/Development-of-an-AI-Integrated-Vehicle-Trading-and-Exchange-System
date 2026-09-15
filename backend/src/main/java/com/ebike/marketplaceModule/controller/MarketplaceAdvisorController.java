package com.ebike.marketplaceModule.controller;

import com.ebike.chatbotModule.dto.request.ChatbotAskRequest;
import com.ebike.chatbotModule.dto.response.ChatbotResponse;
import com.ebike.marketplaceModule.service.MarketplaceAdvisorService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import java.util.List;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/marketplace/advisor")
public class MarketplaceAdvisorController {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(MarketplaceAdvisorController.class);
    private final MarketplaceAdvisorService service;
    public MarketplaceAdvisorController(MarketplaceAdvisorService service) { this.service = service; }
    @PostMapping("/ask") public ChatbotResponse ask(@RequestBody ChatbotAskRequest request) {
        try { return service.ask(request); }
        catch (Exception exception) {
            LOGGER.error("Marketplace advisor request failed", exception);
            return new ChatbotResponse("Hệ thống hiện chưa lấy được dữ liệu phương tiện. Bạn có thể thử lại sau.", "error", List.of());
        }
    }
    @PostMapping("/draft") public Map<String, Object> draft(@RequestBody Map<String, Object> request) { return service.draft(request); }
    @PostMapping(value="/analyze-media", consumes="multipart/form-data")
    public Map<String, Object> analyzeMedia(@RequestPart("files") List<MultipartFile> files) { return service.analyzeMedia(files); }
}
