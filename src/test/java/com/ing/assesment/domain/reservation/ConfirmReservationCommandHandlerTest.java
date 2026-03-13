package com.ing.assesment.domain.reservation;

import com.ing.assesment.domain.auth.model.AuthenticatedUser;
import com.ing.assesment.domain.auth.model.UserRole;
import com.ing.assesment.domain.auth.port.CurrentUserPort;
import com.ing.assesment.domain.common.exception.AccessDeniedException;
import com.ing.assesment.domain.common.exception.InvalidReservationStateException;
import com.ing.assesment.domain.common.exception.ReservationNotFoundException;
import com.ing.assesment.domain.reservation.command.ConfirmReservationCommand;
import com.ing.assesment.domain.reservation.command.handler.ConfirmReservationCommandHandler;
import com.ing.assesment.domain.reservation.model.Reservation;
import com.ing.assesment.domain.reservation.model.ReservationStatus;
import com.ing.assesment.domain.reservation.port.ReservationRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfirmReservationCommandHandlerTest {

    @Mock
    private ReservationRepositoryPort reservationRepository;

    @Mock
    private CurrentUserPort currentUserPort;

    private ConfirmReservationCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ConfirmReservationCommandHandler(reservationRepository, currentUserPort);
    }

    @Test
    void shouldConfirmReservationWhenCallerIsOwner() {
        ConfirmReservationCommand command = new ConfirmReservationCommand(10L);

        Reservation reservation = Reservation.createPending(100L, 200L, 2);
        reservation.setId(10L);

        AuthenticatedUser owner = new AuthenticatedUser(
                200L,
                "owner@test.com",
                Set.of(UserRole.CUSTOMER)
        );

        when(currentUserPort.getCurrentUser()).thenReturn(owner);
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Reservation result = handler.handle(command);

        assertEquals(ReservationStatus.CONFIRMED, result.getStatus());

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());

        Reservation saved = captor.getValue();
        assertEquals(ReservationStatus.CONFIRMED, saved.getStatus());
    }

    @Test
    void shouldConfirmReservationWhenCallerIsAdmin() {
        ConfirmReservationCommand command = new ConfirmReservationCommand(11L);

        Reservation reservation = Reservation.createPending(100L, 200L, 3);
        reservation.setId(11L);

        AuthenticatedUser admin = new AuthenticatedUser(
                1L,
                "admin@test.com",
                Set.of(UserRole.ADMIN)
        );

        when(currentUserPort.getCurrentUser()).thenReturn(admin);
        when(reservationRepository.findById(11L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Reservation result = handler.handle(command);

        assertEquals(ReservationStatus.CONFIRMED, result.getStatus());
    }

    @Test
    void shouldThrowWhenCallerIsNeitherOwnerNorAdmin() {
        ConfirmReservationCommand command = new ConfirmReservationCommand(12L);

        Reservation reservation = Reservation.createPending(100L, 200L, 1);
        reservation.setId(12L);

        AuthenticatedUser anotherUser = new AuthenticatedUser(
                999L,
                "other@test.com",
                Set.of(UserRole.CUSTOMER)
        );

        when(currentUserPort.getCurrentUser()).thenReturn(anotherUser);
        when(reservationRepository.findById(12L)).thenReturn(Optional.of(reservation));

        assertThrows(AccessDeniedException.class, () -> handler.handle(command));

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenReservationNotFound() {
        ConfirmReservationCommand command = new ConfirmReservationCommand(999L);

        AuthenticatedUser admin = new AuthenticatedUser(
                1L,
                "admin@test.com",
                Set.of(UserRole.ADMIN)
        );

        when(currentUserPort.getCurrentUser()).thenReturn(admin);
        when(reservationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ReservationNotFoundException.class, () -> handler.handle(command));

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenReservationAlreadyConfirmed() {
        ConfirmReservationCommand command = new ConfirmReservationCommand(13L);

        Reservation reservation = Reservation.createPending(100L, 200L, 2);
        reservation.setId(13L);
        reservation.setStatus(ReservationStatus.CONFIRMED);

        AuthenticatedUser owner = new AuthenticatedUser(
                200L,
                "owner@test.com",
                Set.of(UserRole.CUSTOMER)
        );

        when(currentUserPort.getCurrentUser()).thenReturn(owner);
        when(reservationRepository.findById(13L)).thenReturn(Optional.of(reservation));

        assertThrows(InvalidReservationStateException.class, () -> handler.handle(command));

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenReservationIsCancelled() {
        ConfirmReservationCommand command = new ConfirmReservationCommand(14L);

        Reservation reservation = Reservation.createPending(100L, 200L, 2);
        reservation.setId(14L);
        reservation.setStatus(ReservationStatus.CANCELLED);

        AuthenticatedUser owner = new AuthenticatedUser(
                200L,
                "owner@test.com",
                Set.of(UserRole.CUSTOMER)
        );

        when(currentUserPort.getCurrentUser()).thenReturn(owner);
        when(reservationRepository.findById(14L)).thenReturn(Optional.of(reservation));

        assertThrows(InvalidReservationStateException.class, () -> handler.handle(command));

        verify(reservationRepository, never()).save(any());
    }
}
