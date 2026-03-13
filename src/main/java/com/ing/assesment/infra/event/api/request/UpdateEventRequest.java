package com.ing.assesment.infra.event.api.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record UpdateEventRequest(
        @NotBlank String title,
        @NotBlank String venue,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt,
        @NotNull @Min(1) Integer capacity,
        @NotNull Long version
) {
}
