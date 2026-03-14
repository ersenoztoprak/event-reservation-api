package com.ing.assesment.infra.event.persistence.adapter;


import com.ing.assesment.domain.common.exception.OptimisticLockConflictException;
import com.ing.assesment.domain.event.model.Event;
import com.ing.assesment.domain.event.port.EventRepositoryPort;
import com.ing.assesment.infra.event.persistence.entity.EventEntity;
import com.ing.assesment.infra.event.persistence.repository.EventJpaRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class EventRepositoryAdapter implements EventRepositoryPort {

    private final EventJpaRepository eventJpaRepository;

    @Override
    public Event save(Event event) {
        try {
            EventEntity entity = new EventEntity();
            entity.setId(event.getId());
            entity.setOwnerId(event.getOwnerId());
            entity.setTitle(event.getTitle());
            entity.setVenue(event.getVenue());
            entity.setStartsAt(event.getStartsAt());
            entity.setEndsAt(event.getEndsAt());
            entity.setCapacity(event.getCapacity());
            entity.setPublished(event.isPublished());
            entity.setVersion(event.getVersion());

            // optimistic lock conflict’i transaction sonunda değil, daha erken yüzeye çıkarır
            EventEntity saved = eventJpaRepository.saveAndFlush(entity);
            return toDomain(saved);
        } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
            throw new OptimisticLockConflictException();
        }
    }

    @Override
    public Optional<Event> findById(Long id) {
        return eventJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Event> findByIdForReservation(Long id) {
        return eventJpaRepository.findByIdForReservation(id).map(this::toDomain);
    }

    @Override
    public List<Event> findAll() {
        return eventJpaRepository.findAll()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Event> findAllByOwnerId(Long ownerId) {
        return eventJpaRepository.findAllByOwnerId(ownerId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Event> searchPublishedEvents(Instant from, Instant to, String q) {
        return eventJpaRepository.searchPublishedEvents(from, to, q)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private Event toDomain(EventEntity entity) {
        Event event = Event.createDraft(
                entity.getOwnerId(),
                entity.getTitle(),
                entity.getVenue(),
                entity.getStartsAt(),
                entity.getEndsAt(),
                entity.getCapacity()
        );
        event.setId(entity.getId());
        event.setOwnerId(entity.getOwnerId());
        event.setPublished(entity.isPublished());
        event.setVersion(entity.getVersion());
        return event;
    }
}
