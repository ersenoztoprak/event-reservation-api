package com.ing.assesment.domain.event.query;

import java.time.Instant;

public record SearchPublicEventsQuery(
        Instant from,
        Instant to,
        String q
) {
}
