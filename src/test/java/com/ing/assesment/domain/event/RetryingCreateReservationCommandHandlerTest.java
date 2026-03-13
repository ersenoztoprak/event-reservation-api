package com.ing.assesment.domain.event;


import com.ing.assesment.domain.common.CommandHandler;
import com.ing.assesment.domain.common.exception.OptimisticLockConflictException;
import com.ing.assesment.domain.reservation.command.CreateReservationCommand;
import com.ing.assesment.domain.reservation.model.Reservation;
import com.ing.assesment.infra.reservation.command.handler.RetryingCreateReservationCommandHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class RetryingCreateReservationCommandHandlerTest {

    private CommandHandler<CreateReservationCommand, Reservation> transactionalHandler;
    private RetryingCreateReservationCommandHandler handler;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        transactionalHandler = mock(CommandHandler.class);
        handler = new RetryingCreateReservationCommandHandler(transactionalHandler);
    }

    @Test
    void shouldRetryAndSucceedAfterOptimisticConflict() {
        CreateReservationCommand command = new CreateReservationCommand(10L, 3, "idem-7");

        Reservation reservation = Reservation.createPending(10L, 101L, 3);
        reservation.setId(1000L);

        when(transactionalHandler.handle(command))
                .thenThrow(new OptimisticLockConflictException())
                .thenReturn(reservation);

        Reservation result = handler.handle(command);

        assertEquals(1000L, result.getId());
        verify(transactionalHandler, times(2)).handle(command);
    }

    @Test
    void shouldRetryAndSucceedAfterSpringOptimisticConflict() {
        CreateReservationCommand command = new CreateReservationCommand(10L, 3, "idem-8");

        Reservation reservation = Reservation.createPending(10L, 101L, 3);
        reservation.setId(2000L);

        when(transactionalHandler.handle(command))
                .thenThrow(new ObjectOptimisticLockingFailureException("EventEntity", 10L))
                .thenReturn(reservation);

        Reservation result = handler.handle(command);

        assertEquals(2000L, result.getId());
        verify(transactionalHandler, times(2)).handle(command);
    }

    @Test
    void shouldThrowAfterRetriesExhausted() {
        CreateReservationCommand command = new CreateReservationCommand(10L, 3, "idem-9");

        when(transactionalHandler.handle(command))
                .thenThrow(new OptimisticLockConflictException())
                .thenThrow(new OptimisticLockConflictException())
                .thenThrow(new OptimisticLockConflictException());

        assertThrows(OptimisticLockConflictException.class, () -> handler.handle(command));

        verify(transactionalHandler, times(3)).handle(command);
    }
}
