package com.ing.assesment.infra.reservation.command.handler;

import com.ing.assesment.domain.common.exception.OptimisticLockConflictException;
import com.ing.assesment.domain.common.handler.CommandHandler;
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

        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                return transactionalHandler.handle(command);
            } catch (OptimisticLockConflictException | ObjectOptimisticLockingFailureException ex) {
                if (i == MAX_RETRIES - 1) {
                    throw new OptimisticLockConflictException();
                }
            }
        }

        throw new IllegalStateException("Unreachable code");
    }
}
