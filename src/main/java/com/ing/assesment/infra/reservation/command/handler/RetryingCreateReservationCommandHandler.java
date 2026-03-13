package com.ing.assesment.infra.reservation.command.handler;

import com.ing.assesment.domain.common.CommandHandler;
import com.ing.assesment.domain.common.exception.OptimisticLockConflictException;
import com.ing.assesment.domain.reservation.command.CreateReservationCommand;
import com.ing.assesment.domain.reservation.model.Reservation;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@RequiredArgsConstructor
public class RetryingCreateReservationCommandHandler implements CommandHandler<CreateReservationCommand, Reservation> {

    private static final int MAX_RETRIES = 3;

    private final CommandHandler<CreateReservationCommand, Reservation> transactionalHandler;

    @Override
    public Reservation handle(CreateReservationCommand command) {
        RuntimeException last = null;

        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                return transactionalHandler.handle(command);
            } catch (OptimisticLockConflictException | ObjectOptimisticLockingFailureException ex) {
                last = ex instanceof RuntimeException re
                        ? re
                        : new OptimisticLockConflictException();
            }
        }

        throw last != null ? last : new OptimisticLockConflictException();
    }
}
