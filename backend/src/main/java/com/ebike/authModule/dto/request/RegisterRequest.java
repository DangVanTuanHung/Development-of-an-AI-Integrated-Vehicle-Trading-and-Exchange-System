package com.ebike.authModule.dto.request;

public record RegisterRequest(
    @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=100) String username,
    @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Email @jakarta.validation.constraints.Size(max=255) String email,
    @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(min=6,max=72) String password,
    @jakarta.validation.constraints.Size(max=100) String firstName,
    @jakarta.validation.constraints.Size(max=100) String lastName,
    @jakarta.validation.constraints.Size(max=30) String phoneNumber
) {
}
