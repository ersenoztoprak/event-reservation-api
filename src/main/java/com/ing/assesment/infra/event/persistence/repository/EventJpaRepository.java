package com.ing.assesment.infra.event.persistence.repository;

import com.ing.assesment.infra.event.persistence.entity.EventEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EventJpaRepository extends JpaRepository<EventEntity, Long> {
    List<EventEntity> findAllByOwnerId(Long ownerId);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("select e from EventEntity e where e.id = :id")
    Optional<EventEntity> findByIdForReservation(Long id);

    @Query("""
           select e
           from EventEntity e
           where e.published = true
             and (:from is null or e.startsAt >= :from)
             and (:to is null or e.startsAt <= :to)
             and (
                    :q is null or :q = '' or
                    lower(e.title) like lower(concat('%', :q, '%')) or
                    lower(e.venue) like lower(concat('%', :q, '%'))
                 )
           order by e.startsAt asc
           """)
    List<EventEntity> searchPublishedEvents(Instant from, Instant to, String q);
}
