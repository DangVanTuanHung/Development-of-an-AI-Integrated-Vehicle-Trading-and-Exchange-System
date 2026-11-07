package com.ebike.authModule.dto.request;

public record UpdateProfileRequest(
    @jakarta.validation.constraints.Size(max=100) String firstName,
    @jakarta.validation.constraints.Size(max=100) String lastName,
    @jakarta.validation.constraints.Email @jakarta.validation.constraints.Size(max=255) String email,
    @jakarta.validation.constraints.Size(max=30) String phoneNumber
) {
}
