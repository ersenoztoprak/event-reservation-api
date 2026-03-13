package com.ing.assesment.infra.reservation.api.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateReservationRequest(
        @NotNull @Min(1) Integer seats
) {
}
