package com.ing.assesment.domain.reservation.port;

import com.ing.assesment.domain.reservation.model.Reservation;

import java.util.Optional;

public interface ReservationRepositoryPort {

    Reservation save(Reservation reservation);

    int sumActiveReservedSeats(Long eventId);

    Optional<Reservation> findById(Long id);
}
