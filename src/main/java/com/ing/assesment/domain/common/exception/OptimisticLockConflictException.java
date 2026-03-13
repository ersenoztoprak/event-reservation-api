package com.ing.assesment.domain.common.exception;

public class OptimisticLockConflictException extends RuntimeException {

    public OptimisticLockConflictException() {
        super("Event was modified by another user");
    }
}
