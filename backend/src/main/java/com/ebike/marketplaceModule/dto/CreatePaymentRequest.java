package com.ebike.marketplaceModule.dto;

import jakarta.validation.constraints.NotBlank;

public record CreatePaymentRequest(@NotBlank String stage, @NotBlank String provider) {}

