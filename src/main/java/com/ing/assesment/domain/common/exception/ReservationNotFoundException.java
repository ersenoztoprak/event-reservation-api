package com.ing.assesment.domain.common.exception;

public class ReservationNotFoundException extends RuntimeException {

    public ReservationNotFoundException(Long reservationId) {
        super("Reservation not found: " + reservationId);
    }
}
