package com.ing.assesment.infra.reservation.persistence.repository;

import com.ing.assesment.domain.reservation.model.ReservationStatus;
import com.ing.assesment.infra.reservation.persistence.entity.ReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReservationJpaRepository extends JpaRepository<ReservationEntity, Long> {

    @Query("""
           select coalesce(sum(r.seats), 0)
           from ReservationEntity r
           where r.eventId = :eventId
             and r.status in :statuses
           """)
    int sumReservedSeatsByEventIdAndStatuses(Long eventId, List<ReservationStatus> statuses);
}
