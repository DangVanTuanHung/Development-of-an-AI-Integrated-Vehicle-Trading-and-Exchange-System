package com.ebike.marketplaceModule.advisor;

import java.math.BigDecimal;

public record AdvisorEntities(
    BigDecimal minPrice, BigDecimal maxPrice, Integer minYear, Integer maxYear,
    Integer seats, Integer maxMileage, String fuelType, String transmission,
    String location, Integer resultOrdinal, boolean cheaper, String normalizedText
) {}
