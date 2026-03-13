package com.ing.assesment.domain.event.command;

import java.time.Instant;

public record UpdateEventCommand(
        Long eventId,
        String title,
        String venue,
        Instant startsAt,
        Instant endsAt,
        Integer capacity,
        Long version) {
}
