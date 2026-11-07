package com.ebike.authModule.controller;

import com.ebike.authModule.service.AccountVerificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AccountVerificationController {
    private final AccountVerificationService service;
    public AccountVerificationController(AccountVerificationService service) { this.service=service; }
    public record EmailRequest(@NotBlank @Email @Size(max=255) String email) {}
    public record CodeRequest(@NotNull UUID challengeId, @NotBlank @Pattern(regexp="[0-9]{6}") String code) {}
    public record ResetRequest(@NotNull UUID challengeId, @NotBlank @Pattern(regexp="[0-9]{6}") String code, @NotBlank @Size(min=8,max=72) String newPassword) {}
    @PostMapping("/password-reset/request")
    public Map<String,Object> requestReset(@Valid @RequestBody EmailRequest body) { return service.request(null,body.email(),"RESET_PASSWORD"); }
    @PostMapping("/password-reset/confirm") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@Valid @RequestBody ResetRequest body) { service.verify(null,body.challengeId(),body.code(),"RESET_PASSWORD",body.newPassword()); }
    @GetMapping("/verification/availability")
    public Map<String,Boolean> availability() { return service.availability(); }
    @PostMapping("/verification/email/request")
    public Map<String,Object> email(Authentication auth) { return service.request(auth.getName(),null,"VERIFY_EMAIL"); }
    @PostMapping("/verification/email/confirm") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void email(Authentication auth,@Valid @RequestBody CodeRequest body) { service.verify(auth.getName(),body.challengeId(),body.code(),"VERIFY_EMAIL",null); }
    @PostMapping("/verification/phone/request")
    public Map<String,Object> phone(Authentication auth) { return service.request(auth.getName(),null,"VERIFY_PHONE"); }
    @PostMapping("/verification/phone/confirm") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void phone(Authentication auth,@Valid @RequestBody CodeRequest body) { service.verify(auth.getName(),body.challengeId(),body.code(),"VERIFY_PHONE",null); }
}
