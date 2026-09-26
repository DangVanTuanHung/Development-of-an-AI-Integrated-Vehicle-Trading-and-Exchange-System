package com.ebike.marketplaceModule.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record CreateAppointmentRequest(
    @NotNull @Future OffsetDateTime scheduledAt,
    @NotBlank @Size(max = 500) String location,
    @Size(max = 2000) String note
) {}
