package com.ing.assesment.domain.common.exception;

public class EventNotPublishedException extends RuntimeException {

    public EventNotPublishedException(Long eventId) {
        super("Event is not published: " + eventId);
    }
}
