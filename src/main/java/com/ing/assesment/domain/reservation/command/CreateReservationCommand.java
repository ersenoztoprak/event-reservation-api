package com.ing.assesment.domain.reservation.command;

public record CreateReservationCommand(
        Long eventId,
        Integer seats,
        String idempotencyKey
) {
}
