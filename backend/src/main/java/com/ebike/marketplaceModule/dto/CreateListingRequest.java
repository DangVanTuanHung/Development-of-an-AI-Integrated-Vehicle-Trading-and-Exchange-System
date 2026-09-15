package com.ebike.marketplaceModule.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateListingRequest(
    @NotNull Long categoryId,
    Long brandId,
    @NotBlank @Size(max = 220) String title,
    @NotBlank @Size(max = 10000) String description,
    @NotBlank String listingType,
    @NotBlank String condition,
    @NotNull @DecimalMin("0.01") BigDecimal price,
    boolean negotiable,
    boolean exchangeAllowed,
    @NotBlank @Size(max = 120) String province,
    @Size(max = 120) String district,
    @Size(max = 500) String addressText,
    Integer manufactureYear,
    Integer registrationYear,
    Integer mileageKm,
    @Size(max = 80) String exteriorColor,
    @Size(max = 30) String fuelType,
    @Size(max = 30) String transmission,
    Integer engineCapacityCc,
    Integer rangeKm,
    Integer seats,
    Integer ownersCount,
    @Size(max = 120) String origin
) {}
