package com.ing.assesment.infra.reservation.api.response;

import com.ing.assesment.domain.reservation.model.ReservationStatus;

import java.time.Instant;

public record ReservationResponse(
        Long id,
        Long eventId,
        Long userId,
        ReservationStatus status,
        Integer seats,
        Instant createdAt) {
}
