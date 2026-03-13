package com.ing.assesment.domain.reservation.command.handler;

import com.ing.assesment.domain.auth.model.AuthenticatedUser;
import com.ing.assesment.domain.auth.model.UserRole;
import com.ing.assesment.domain.auth.port.CurrentUserPort;
import com.ing.assesment.domain.common.CommandHandler;
import com.ing.assesment.domain.common.exception.*;
import com.ing.assesment.domain.event.model.Event;
import com.ing.assesment.domain.event.port.EventRepositoryPort;
import com.ing.assesment.domain.idempotency.model.IdempotencyKeyRecord;
import com.ing.assesment.domain.idempotency.model.IdempotencyStatus;
import com.ing.assesment.domain.idempotency.port.IdempotencyKeyRepositoryPort;
import com.ing.assesment.domain.idempotency.port.RequestHashPort;
import com.ing.assesment.domain.reservation.command.CreateReservationCommand;
import com.ing.assesment.domain.reservation.model.Reservation;
import com.ing.assesment.domain.reservation.port.ReservationRepositoryPort;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@RequiredArgsConstructor
public class CreateReservationCommandHandler implements CommandHandler<CreateReservationCommand, Reservation> {

    public static final String ENDPOINT = "POST /api/events/{id}/reservations";

    private final EventRepositoryPort eventRepository;
    private final ReservationRepositoryPort reservationRepository;
    private final IdempotencyKeyRepositoryPort idempotencyKeyRepository;
    private final RequestHashPort requestHashPort;
    private final CurrentUserPort currentUserPort;

    @Override
    public Reservation handle(CreateReservationCommand command) {
        if (command.idempotencyKey() == null || command.idempotencyKey().isBlank()) {
            throw new IdempotencyKeyRequiredException();
        }

        AuthenticatedUser currentUser = currentUserPort.getCurrentUser();
        validateReservationActor(currentUser);

        String requestHash = requestHashPort.hash(
                command.eventId() + ":" + currentUser.id() + ":" + command.seats()
        );

        IdempotencyKeyRecord existing = idempotencyKeyRepository
                .findByKeyAndEndpoint(command.idempotencyKey(), ENDPOINT)
                .orElse(null);

        if (existing != null) {
            return handleExistingIdempotentRequest(existing, requestHash);
        }

        IdempotencyKeyRecord record = IdempotencyKeyRecord.start(
                command.idempotencyKey(),
                ENDPOINT,
                requestHash,
                Instant.now().plusSeconds(24 * 60 * 60)
        );
        record = idempotencyKeyRepository.save(record);

        try {
            Reservation reservation = tryCreate(command, currentUser.id());
            record.markCompleted(String.valueOf(reservation.getId()));
            idempotencyKeyRepository.save(record);
            return reservation;
        } catch (RuntimeException ex) {
            record.markFailed();
            idempotencyKeyRepository.save(record);
            throw ex;
        }
    }

    private Reservation tryCreate(CreateReservationCommand command, Long userId) {
        Event event = eventRepository.findByIdForReservation(command.eventId())
                .orElseThrow(() -> new EventNotFoundException(command.eventId()));

        if (!event.isPublishedEvent()) {
            throw new EventNotPublishedException(command.eventId());
        }

        if (event.isStartedAt(Instant.now())) {
            throw new EventAlreadyStartedException(command.eventId());
        }

        int activeReservedSeats = reservationRepository.sumActiveReservedSeats(command.eventId());
        int remainingSeats = event.getCapacity() - activeReservedSeats;

        if (command.seats() == null || command.seats() <= 0) {
            throw new IllegalArgumentException("Seats must be greater than zero");
        }

        if (command.seats() > remainingSeats) {
            throw new ReservationCapacityExceededException();
        }

        Reservation reservation = Reservation.createPending(
                command.eventId(),
                userId,
                command.seats()
        );

        return reservationRepository.save(reservation);
    }

    private Reservation handleExistingIdempotentRequest(IdempotencyKeyRecord existing, String requestHash) {
        if (!existing.isSameRequest(requestHash)) {
            throw new IdempotencyConflictException("Idempotency key was already used with a different request");
        }

        if (existing.getStatus() == IdempotencyStatus.COMPLETED) {
            Long reservationId = Long.valueOf(existing.getResponseHash());
            return reservationRepository.findById(reservationId)
                    .orElseThrow(() -> new IdempotencyConflictException("Completed idempotent response could not be restored"));
        }

        if (existing.getStatus() == IdempotencyStatus.IN_PROGRESS) {
            throw new IdempotencyConflictException("A request with the same idempotency key is already in progress");
        }

        throw new IdempotencyConflictException("Previous request with this idempotency key failed. Please use a new key");
    }

    private void validateReservationActor(AuthenticatedUser currentUser) {
        boolean isCustomer = currentUser.roles().contains(UserRole.CUSTOMER);
        boolean isAdmin = currentUser.isAdmin();

        if (!isCustomer && !isAdmin) {
            throw new AccessDeniedException("Only customer or admin can create reservations");
        }
    }
}
