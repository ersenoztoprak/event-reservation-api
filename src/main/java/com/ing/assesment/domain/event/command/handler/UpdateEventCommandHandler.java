package com.ing.assesment.domain.event.command.handler;

import com.ing.assesment.domain.auth.model.AuthenticatedUser;
import com.ing.assesment.domain.auth.port.CurrentUserPort;
import com.ing.assesment.domain.common.CommandHandler;
import com.ing.assesment.domain.common.exception.AccessDeniedException;
import com.ing.assesment.domain.common.exception.EventNotFoundException;
import com.ing.assesment.domain.event.command.UpdateEventCommand;
import com.ing.assesment.domain.event.model.Event;
import com.ing.assesment.domain.event.port.EventRepositoryPort;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UpdateEventCommandHandler implements CommandHandler<UpdateEventCommand, Event> {

    private final EventRepositoryPort eventRepository;
    private final CurrentUserPort currentUserPort;

    @Override
    public Event handle(UpdateEventCommand command) {
        AuthenticatedUser currentUser = currentUserPort.getCurrentUser();

        Event event = eventRepository.findById(command.eventId())
                .orElseThrow(() -> new EventNotFoundException(command.eventId()));

        boolean isOwner = event.isOwnedBy(currentUser.id());
        boolean isAdmin = currentUser.isAdmin();

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("Only event owner or admin can update the event");
        }

        event.update(
                command.title(),
                command.venue(),
                command.startsAt(),
                command.endsAt(),
                command.capacity()
        );

        event.setVersion(command.version());

        return eventRepository.save(event);
    }
}
