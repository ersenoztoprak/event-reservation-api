package com.ing.assesment.domain.reservation.command.handler;

import com.ing.assesment.domain.auth.model.AuthenticatedUser;
import com.ing.assesment.domain.auth.port.CurrentUserPort;
import com.ing.assesment.domain.common.exception.AccessDeniedException;
import com.ing.assesment.domain.common.exception.ReservationNotFoundException;
import com.ing.assesment.domain.common.handler.CommandHandler;
import com.ing.assesment.domain.reservation.command.ConfirmReservationCommand;
import com.ing.assesment.domain.reservation.model.Reservation;
import com.ing.assesment.domain.reservation.port.ReservationRepositoryPort;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConfirmReservationCommandHandler implements CommandHandler<ConfirmReservationCommand, Reservation> {

    private final ReservationRepositoryPort reservationRepository;
    private final CurrentUserPort currentUserPort;

    @Override
    public Reservation handle(ConfirmReservationCommand command) {
        AuthenticatedUser currentUser = currentUserPort.getCurrentUser();

        Reservation reservation = reservationRepository.findById(command.reservationId())
                .orElseThrow(() -> new ReservationNotFoundException(command.reservationId()));

        boolean isOwner = reservation.isOwnedBy(currentUser.id());
        boolean isAdmin = currentUser.isAdmin();

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("Only reservation owner or admin can confirm reservation");
        }

        reservation.confirm();

        return reservationRepository.save(reservation);
    }
}
