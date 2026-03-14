package com.ing.assesment.infra.reservation.config;

import com.ing.assesment.domain.auth.port.CurrentUserPort;
import com.ing.assesment.domain.common.handler.CommandHandler;
import com.ing.assesment.domain.event.port.EventRepositoryPort;
import com.ing.assesment.domain.idempotency.port.IdempotencyKeyRepositoryPort;
import com.ing.assesment.domain.idempotency.port.RequestHashPort;
import com.ing.assesment.domain.reservation.command.CancelReservationCommand;
import com.ing.assesment.domain.reservation.command.ConfirmReservationCommand;
import com.ing.assesment.domain.reservation.command.CreateReservationCommand;
import com.ing.assesment.domain.reservation.command.handler.CancelReservationCommandHandler;
import com.ing.assesment.domain.reservation.command.handler.ConfirmReservationCommandHandler;
import com.ing.assesment.domain.reservation.command.handler.CreateReservationCommandHandler;
import com.ing.assesment.domain.reservation.model.Reservation;
import com.ing.assesment.domain.reservation.port.ReservationRepositoryPort;
import com.ing.assesment.infra.reservation.command.handler.RetryingCreateReservationCommandHandler;
import com.ing.assesment.infra.reservation.command.handler.TransactionalCreateReservationCommandHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ReservationBeanConfig {

    @Bean
    public CreateReservationCommandHandler createReservationBusinessHandler(
            EventRepositoryPort eventRepositoryPort,
            ReservationRepositoryPort reservationRepositoryPort,
            IdempotencyKeyRepositoryPort idempotencyKeyRepositoryPort,
            RequestHashPort requestHashPort,
            CurrentUserPort currentUserPort
    ) {
        return new CreateReservationCommandHandler(
                eventRepositoryPort,
                reservationRepositoryPort,
                idempotencyKeyRepositoryPort,
                requestHashPort,
                currentUserPort
        );
    }

    @Bean
    public CommandHandler<CreateReservationCommand, Reservation> transactionalCreateReservationCommandHandler(
            @Qualifier("createReservationBusinessHandler")
            CommandHandler<CreateReservationCommand, Reservation> delegate
    ) {
        return new TransactionalCreateReservationCommandHandler(delegate);
    }

    @Bean
    @Primary
    public CommandHandler<CreateReservationCommand, Reservation> createReservationCommandHandler(
            @Qualifier("transactionalCreateReservationCommandHandler")
            CommandHandler<CreateReservationCommand, Reservation> transactionalHandler) {
        return new RetryingCreateReservationCommandHandler(transactionalHandler);
    }

    @Bean
    public CommandHandler<ConfirmReservationCommand, Reservation> confirmReservationCommandHandler(
            ReservationRepositoryPort reservationRepositoryPort,
            CurrentUserPort currentUserPort
    ) {
        return new ConfirmReservationCommandHandler(reservationRepositoryPort, currentUserPort);
    }

    @Bean
    public CommandHandler<CancelReservationCommand, Reservation> cancelReservationCommandHandler(
            ReservationRepositoryPort reservationRepositoryPort,
            CurrentUserPort currentUserPort
    ) {
        return new CancelReservationCommandHandler(reservationRepositoryPort, currentUserPort);
    }
}
