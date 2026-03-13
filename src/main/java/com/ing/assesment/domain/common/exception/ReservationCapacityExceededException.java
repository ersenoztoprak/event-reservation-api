package com.ing.assesment.domain.common.exception;

public class ReservationCapacityExceededException extends RuntimeException {

    public ReservationCapacityExceededException() {
        super("Not enough seats available");
    }
}
