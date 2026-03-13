package com.ing.assesment.infra.reservation.persistence.adapter;

import com.ing.assesment.domain.reservation.model.Reservation;
import com.ing.assesment.domain.reservation.model.ReservationStatus;
import com.ing.assesment.domain.reservation.port.ReservationRepositoryPort;
import com.ing.assesment.infra.reservation.persistence.entity.ReservationEntity;
import com.ing.assesment.infra.reservation.persistence.repository.ReservationJpaRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class ReservationRepositoryAdapter implements ReservationRepositoryPort {

    private final ReservationJpaRepository reservationJpaRepository;

    @Override
    public Reservation save(Reservation reservation) {
        ReservationEntity entity = new ReservationEntity();
        entity.setId(reservation.getId());
        entity.setEventId(reservation.getEventId());
        entity.setUserId(reservation.getUserId());
        entity.setStatus(reservation.getStatus());
        entity.setSeats(reservation.getSeats());
        entity.setCreatedAt(reservation.getCreatedAt());

        ReservationEntity saved = reservationJpaRepository.saveAndFlush(entity);
        return toDomain(saved);
    }

    @Override
    public int sumActiveReservedSeats(Long eventId) {
        return reservationJpaRepository.sumReservedSeatsByEventIdAndStatuses(
                eventId,
                List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED)
        );
    }

    @Override
    public Optional<Reservation> findById(Long id) {
        return reservationJpaRepository.findById(id).map(this::toDomain);
    }

    private Reservation toDomain(ReservationEntity entity) {
        Reservation reservation = Reservation.createPending(
                entity.getEventId(),
                entity.getUserId(),
                entity.getSeats()
        );
        reservation.setId(entity.getId());
        reservation.setCreatedAt(entity.getCreatedAt());
        reservation.setStatus(entity.getStatus());
        return reservation;
    }
}
