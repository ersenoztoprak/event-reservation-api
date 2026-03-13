package com.ing.assesment.domain.event.port;

import com.ing.assesment.domain.event.model.Event;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EventRepositoryPort {

    Event save(Event event);

    Optional<Event> findById(Long id);

    Optional<Event> findByIdForReservation(Long id);

    List<Event> findAllByOwnerId(Long ownerId);

    List<Event> findAll();

    List<Event> searchPublishedEvents(Instant from, Instant to, String q);
}
