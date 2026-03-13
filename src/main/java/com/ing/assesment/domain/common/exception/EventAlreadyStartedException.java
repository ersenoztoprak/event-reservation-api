package com.ing.assesment.domain.common.exception;

public class EventAlreadyStartedException extends RuntimeException {

    public EventAlreadyStartedException(Long eventId) {
        super("Event has already started: " + eventId);
    }
}
