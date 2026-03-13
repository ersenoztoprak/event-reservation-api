package com.ing.assesment.infra.reservation.command.handler;

import com.ing.assesment.domain.common.CommandHandler;
import com.ing.assesment.domain.reservation.command.CreateReservationCommand;
import com.ing.assesment.domain.reservation.model.Reservation;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class TransactionalCreateReservationCommandHandler implements CommandHandler<CreateReservationCommand, Reservation> {

    private final CommandHandler<CreateReservationCommand, Reservation> delegate;

    @Override
    @Transactional
    public Reservation handle(CreateReservationCommand command) {
        return delegate.handle(command);
    }
}
