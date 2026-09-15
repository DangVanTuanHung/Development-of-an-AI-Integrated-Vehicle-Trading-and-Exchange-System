package com.ebike.orderModule.controller;

import com.ebike.orderModule.dto.request.VnPayCreatePaymentRequest;
import com.ebike.orderModule.dto.response.VnPayCreatePaymentResponse;
import com.ebike.orderModule.dto.response.VnPayReturnResponse;
import com.ebike.orderModule.service.VnPayService;
import com.ebike.shared.constants.PermissionConstants;
import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments/vnpay")
public class VnPayController {

    private final VnPayService vnPayService;

    public VnPayController(VnPayService vnPayService) {
        this.vnPayService = vnPayService;
    }

    @PostMapping("/create")
    public VnPayCreatePaymentResponse createPaymentUrl(
        @RequestBody VnPayCreatePaymentRequest request,
        HttpServletRequest servletRequest,
        Authentication authentication
    ) {
        return vnPayService.createPaymentUrl(
            request,
            resolveClientIp(servletRequest),
            authentication == null ? null : authentication.getName(),
            isBackOffice(authentication)
        );
    }

    @RequestMapping(value = "/ipn", method = {RequestMethod.GET, RequestMethod.POST})
    public Map<String, String> handleIpnCallback(@RequestParam Map<String, String> params) {
        return vnPayService.handleIpnCallback(params);
    }

    @GetMapping("/return")
    public VnPayReturnResponse handleReturnUrl(@RequestParam Map<String, String> params) {
        return vnPayService.handleReturnUrl(params);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return normalizeClientIp(forwardedFor.split(",")[0].trim());
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return normalizeClientIp(realIp.trim());
        }
        return normalizeClientIp(request.getRemoteAddr());
    }

    private String normalizeClientIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return "127.0.0.1";
        }
        try {
            InetAddress address = InetAddress.getByName(clientIp);
            if (address.isLoopbackAddress()) {
                return "127.0.0.1";
            }
            return address.getHostAddress();
        } catch (UnknownHostException exception) {
            return "127.0.0.1";
        }
    }

    private boolean isBackOffice(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
            .anyMatch(authority -> PermissionConstants.OrderManagement.PAYMENT_VERIFY.equals(authority.getAuthority()));
    }
}
