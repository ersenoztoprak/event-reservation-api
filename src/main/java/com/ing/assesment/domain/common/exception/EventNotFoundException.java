package com.ing.assesment.domain.common.exception;

public class EventNotFoundException extends RuntimeException {

    public EventNotFoundException(Long eventId) {
        super("Event not found: " + eventId);
    }
}
