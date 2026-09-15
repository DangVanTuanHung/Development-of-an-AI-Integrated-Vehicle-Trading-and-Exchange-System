package com.ebike.marketplaceModule.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateOfferRequest(
    @NotNull @DecimalMin("0.01") BigDecimal amount,
    @NotNull @DecimalMin("0.00") BigDecimal depositAmount,
    @Size(max = 2000) String message
) {}

