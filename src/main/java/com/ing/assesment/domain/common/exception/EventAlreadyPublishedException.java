package com.ing.assesment.domain.common.exception;

public class EventAlreadyPublishedException extends RuntimeException {

    public EventAlreadyPublishedException(Long eventId) {
        super("Event is already published: " + eventId);
    }
}
