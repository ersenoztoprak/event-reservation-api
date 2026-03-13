package com.ing.assesment.domain.event.command;

public record PublishEventCommand(
        Long eventId,
        Long version) {
}
