package com.ing.assesment.domain.reservation;

import com.ing.assesment.domain.auth.model.AuthenticatedUser;
import com.ing.assesment.domain.auth.model.UserRole;
import com.ing.assesment.domain.auth.port.CurrentUserPort;
import com.ing.assesment.domain.common.exception.EventAlreadyStartedException;
import com.ing.assesment.domain.common.exception.EventNotPublishedException;
import com.ing.assesment.domain.common.exception.IdempotencyConflictException;
import com.ing.assesment.domain.common.exception.IdempotencyKeyRequiredException;
import com.ing.assesment.domain.common.exception.ReservationCapacityExceededException;
import com.ing.assesment.domain.event.model.Event;
import com.ing.assesment.domain.event.port.EventRepositoryPort;
import com.ing.assesment.domain.idempotency.model.IdempotencyKeyRecord;
import com.ing.assesment.domain.idempotency.model.IdempotencyStatus;
import com.ing.assesment.domain.idempotency.port.IdempotencyKeyRepositoryPort;
import com.ing.assesment.domain.idempotency.port.RequestHashPort;
import com.ing.assesment.domain.reservation.command.CreateReservationCommand;
import com.ing.assesment.domain.reservation.command.handler.CreateReservationCommandHandler;
import com.ing.assesment.domain.reservation.model.Reservation;
import com.ing.assesment.domain.reservation.port.ReservationRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateReservationCommandHandlerTest {

    @Mock
    private EventRepositoryPort eventRepository;

    @Mock
    private ReservationRepositoryPort reservationRepository;

    @Mock
    private IdempotencyKeyRepositoryPort idempotencyKeyRepository;

    @Mock
    private RequestHashPort requestHashPort;

    @Mock
    private CurrentUserPort currentUserPort;

    private CreateReservationCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CreateReservationCommandHandler(
                eventRepository,
                reservationRepository,
                idempotencyKeyRepository,
                requestHashPort,
                currentUserPort);
    }

    @Test
    void shouldCreatePendingReservationSuccessfully() {
        CreateReservationCommand command = new CreateReservationCommand(10L, 3, "idem-1");

        AuthenticatedUser customer = new AuthenticatedUser(
                101L,
                "customer@test.com",
                Set.of(UserRole.CUSTOMER)
        );

        Event event = publishedFutureEvent(10L, 500L, 100);

        when(currentUserPort.getCurrentUser()).thenReturn(customer);
        when(requestHashPort.hash(anyString())).thenReturn("hash-1");
        when(idempotencyKeyRepository.findByKeyAndEndpoint(eq("idem-1"), anyString())).thenReturn(Optional.empty());
        when(idempotencyKeyRepository.save(any(IdempotencyKeyRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventRepository.findByIdForReservation(10L)).thenReturn(Optional.of(event));
        when(reservationRepository.sumActiveReservedSeats(10L)).thenReturn(20);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation reservation = invocation.getArgument(0);
            reservation.setId(999L);
            return reservation;
        });

        Reservation result = handler.handle(command);

        assertNotNull(result);
        assertEquals(999L, result.getId());
        assertEquals(10L, result.getEventId());
        assertEquals(101L, result.getUserId());
        assertEquals(3, result.getSeats());

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());

        Reservation saved = captor.getValue();
        assertEquals(10L, saved.getEventId());
        assertEquals(101L, saved.getUserId());
        assertEquals(3, saved.getSeats());
    }

    @Test
    void shouldThrowWhenIdempotencyKeyMissing() {
        CreateReservationCommand command = new CreateReservationCommand(10L, 2, null);

        assertThrows(IdempotencyKeyRequiredException.class, () -> handler.handle(command));

        verifyNoInteractions(currentUserPort, eventRepository, reservationRepository, idempotencyKeyRepository);
    }

    @Test
    void shouldThrowWhenEventNotPublished() {
        CreateReservationCommand command = new CreateReservationCommand(10L, 2, "idem-2");

        AuthenticatedUser customer = new AuthenticatedUser(
                101L,
                "customer@test.com",
                Set.of(UserRole.CUSTOMER)
        );

        Event event = draftFutureEvent(10L, 500L, 100);

        when(currentUserPort.getCurrentUser()).thenReturn(customer);
        when(requestHashPort.hash(anyString())).thenReturn("hash-2");
        when(idempotencyKeyRepository.findByKeyAndEndpoint(eq("idem-2"), anyString())).thenReturn(Optional.empty());
        when(idempotencyKeyRepository.save(any(IdempotencyKeyRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventRepository.findByIdForReservation(10L)).thenReturn(Optional.of(event));

        assertThrows(EventNotPublishedException.class, () -> handler.handle(command));
    }

    @Test
    void shouldThrowWhenEventAlreadyStarted() {
        CreateReservationCommand command = new CreateReservationCommand(10L, 2, "idem-3");

        AuthenticatedUser customer = new AuthenticatedUser(
                101L,
                "customer@test.com",
                Set.of(UserRole.CUSTOMER)
        );

        Event event = publishedStartedEvent(10L, 500L, 100);

        when(currentUserPort.getCurrentUser()).thenReturn(customer);
        when(requestHashPort.hash(anyString())).thenReturn("hash-3");
        when(idempotencyKeyRepository.findByKeyAndEndpoint(eq("idem-3"), anyString())).thenReturn(Optional.empty());
        when(idempotencyKeyRepository.save(any(IdempotencyKeyRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventRepository.findByIdForReservation(10L)).thenReturn(Optional.of(event));

        assertThrows(EventAlreadyStartedException.class, () -> handler.handle(command));
    }

    @Test
    void shouldThrowWhenCapacityExceeded() {
        CreateReservationCommand command = new CreateReservationCommand(10L, 15, "idem-4");

        AuthenticatedUser customer = new AuthenticatedUser(
                101L,
                "customer@test.com",
                Set.of(UserRole.CUSTOMER)
        );

        Event event = publishedFutureEvent(10L, 500L, 20);

        when(currentUserPort.getCurrentUser()).thenReturn(customer);
        when(requestHashPort.hash(anyString())).thenReturn("hash-4");
        when(idempotencyKeyRepository.findByKeyAndEndpoint(eq("idem-4"), anyString())).thenReturn(Optional.empty());
        when(idempotencyKeyRepository.save(any(IdempotencyKeyRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventRepository.findByIdForReservation(10L)).thenReturn(Optional.of(event));
        when(reservationRepository.sumActiveReservedSeats(10L)).thenReturn(10);

        assertThrows(ReservationCapacityExceededException.class, () -> handler.handle(command));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void shouldReturnExistingReservationForSameIdempotentRequest() {
        CreateReservationCommand command = new CreateReservationCommand(10L, 2, "idem-5");

        AuthenticatedUser customer = new AuthenticatedUser(
                101L,
                "customer@test.com",
                Set.of(UserRole.CUSTOMER)
        );

        IdempotencyKeyRecord record = IdempotencyKeyRecord.start("idem-5", "POST /api/events/{id}/reservations", "hash-5", Instant.now().plusSeconds(3600));
        record.setResponseHash("777");
        record.setStatus(IdempotencyStatus.COMPLETED);

        Reservation existingReservation = Reservation.createPending(10L, 101L, 2);
        existingReservation.setId(777L);

        when(currentUserPort.getCurrentUser()).thenReturn(customer);
        when(requestHashPort.hash(anyString())).thenReturn("hash-5");
        when(idempotencyKeyRepository.findByKeyAndEndpoint(eq("idem-5"), anyString())).thenReturn(Optional.of(record));
        when(reservationRepository.findById(777L)).thenReturn(Optional.of(existingReservation));

        Reservation result = handler.handle(command);

        assertEquals(777L, result.getId());
        verify(eventRepository, never()).findByIdForReservation(anyLong());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenSameIdempotencyKeyUsedForDifferentRequest() {
        CreateReservationCommand command = new CreateReservationCommand(10L, 5, "idem-6");

        AuthenticatedUser customer = new AuthenticatedUser(
                101L,
                "customer@test.com",
                Set.of(UserRole.CUSTOMER)
        );

        IdempotencyKeyRecord record = IdempotencyKeyRecord.start("idem-6", "POST /api/events/{id}/reservations", "old-hash", Instant.now().plusSeconds(3600));
        record.setStatus(IdempotencyStatus.COMPLETED);

        when(currentUserPort.getCurrentUser()).thenReturn(customer);
        when(requestHashPort.hash(anyString())).thenReturn("new-hash");
        when(idempotencyKeyRepository.findByKeyAndEndpoint(eq("idem-6"), anyString())).thenReturn(Optional.of(record));

        assertThrows(IdempotencyConflictException.class, () -> handler.handle(command));
    }

    private Event publishedFutureEvent(Long id, Long ownerId, int capacity) {
        Event event = Event.createDraft(
                ownerId,
                "Published Event",
                "Venue",
                Instant.now().plusSeconds(3600),
                Instant.now().plusSeconds(7200),
                capacity
        );
        event.setId(id);
        event.setPublished(true);
        event.setVersion(0L);
        return event;
    }

    private Event draftFutureEvent(Long id, Long ownerId, int capacity) {
        Event event = Event.createDraft(
                ownerId,
                "Draft Event",
                "Venue",
                Instant.now().plusSeconds(3600),
                Instant.now().plusSeconds(7200),
                capacity
        );
        event.setId(id);
        event.setPublished(false);
        event.setVersion(0L);
        return event;
    }

    private Event publishedStartedEvent(Long id, Long ownerId, int capacity) {
        Event event = Event.createDraft(
                ownerId,
                "Started Event",
                "Venue",
                Instant.now().minusSeconds(3600),
                Instant.now().plusSeconds(3600),
                capacity
        );
        event.setId(id);
        event.setPublished(true);
        event.setVersion(0L);
        return event;
    }
}
