package com.ing.assesment.infra.event.api.response;

import java.time.Instant;

public record CreateEventResponse(
        Long id,
        Long ownerId,
        String title,
        String venue,
        Instant startsAt,
        Instant endsAt,
        Integer capacity,
        boolean published,
        Long version
) {
}
