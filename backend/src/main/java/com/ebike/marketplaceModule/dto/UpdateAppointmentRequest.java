package com.ebike.marketplaceModule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAppointmentRequest(
    @NotBlank String status,
    @Size(max = 2000) String note,
    java.time.OffsetDateTime scheduledAt,
    @Size(max=500) String location,
    Integer revision
) {}
