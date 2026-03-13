package com.ing.assesment.domain.event.model;

import com.ing.assesment.domain.common.exception.EventAlreadyPublishedException;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
public class Event {

    private Long id;
    private Long ownerId;
    private String title;
    private String venue;
    private Instant startsAt;
    private Instant endsAt;
    private Integer capacity;
    private boolean published;
    private Long version;

    private Event(Long ownerId,
                  String title,
                  String venue,
                  Instant startsAt,
                  Instant endsAt,
                  Integer capacity) {
        this.ownerId = ownerId;
        this.title = title;
        this.venue = venue;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.capacity = capacity;
        this.published = false;
    }

    public static Event createDraft(Long ownerId,
                                    String title,
                                    String venue,
                                    Instant startsAt,
                                    Instant endsAt,
                                    Integer capacity) {
        validate(title, venue, startsAt, endsAt, capacity);
        return new Event(ownerId, title, venue, startsAt, endsAt, capacity);
    }

    public void update(String title,
                       String venue,
                       Instant startsAt,
                       Instant endsAt,
                       Integer capacity) {
        validate(title, venue, startsAt, endsAt, capacity);
        this.title = title;
        this.venue = venue;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.capacity = capacity;
    }

    public boolean isOwnedBy(Long userId) {
        return ownerId != null && ownerId.equals(userId);
    }

    private boolean isDraft() {
        return !published;
    }

    public boolean isPublishedEvent() {
        return published;
    }

    public boolean isStartedAt(Instant now) {
        return startsAt != null && !startsAt.isAfter(now);
    }

    public void publish() {
        if (!isDraft()) {
            throw new EventAlreadyPublishedException(getId());
        }
        this.published = true;
    }

    private static void validate(String title,
                                 String venue,
                                 Instant startsAt,
                                 Instant endsAt,
                                 Integer capacity) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (venue == null || venue.isBlank()) {
            throw new IllegalArgumentException("Venue is required");
        }
        if (startsAt == null) {
            throw new IllegalArgumentException("Start date is required");
        }
        if (endsAt == null) {
            throw new IllegalArgumentException("End date is required");
        }
        if (!startsAt.isBefore(endsAt)) {
            throw new IllegalArgumentException("startsAt must be before endsAt");
        }
        if (capacity == null || capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than zero");
        }
    }
}
