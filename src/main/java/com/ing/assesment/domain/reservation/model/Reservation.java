package com.ing.assesment.domain.reservation.model;


import com.ing.assesment.domain.common.exception.InvalidReservationStateException;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class Reservation {

    private Long id;
    private Long eventId;
    private Long userId;
    private ReservationStatus status;
    private Integer seats;
    private Instant createdAt;

    private Reservation() {
    }

    private Reservation(Long eventId, Long userId, Integer seats) {
        this.eventId = eventId;
        this.userId = userId;
        this.seats = seats;
        this.status = ReservationStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public static Reservation createPending(Long eventId, Long userId, Integer seats) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event id is required");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User id is required");
        }
        if (seats == null || seats <= 0) {
            throw new IllegalArgumentException("Seats must be greater than zero");
        }
        return new Reservation(eventId, userId, seats);
    }

    public void confirm() {
        if (status == ReservationStatus.CONFIRMED) {
            throw new InvalidReservationStateException("Reservation is already confirmed");
        }
        if (status == ReservationStatus.CANCELLED) {
            throw new InvalidReservationStateException("Cancelled reservation cannot be confirmed");
        }
        this.status = ReservationStatus.CONFIRMED;
    }

    public boolean isOwnedBy(Long userId) {
        return this.userId != null && this.userId.equals(userId) ;
    }

    public void cancel() {
        if (status == ReservationStatus.CANCELLED) {
            throw new InvalidReservationStateException("Reservation is already cancelled");
        }
        this.status = ReservationStatus.CANCELLED;
    }

}
