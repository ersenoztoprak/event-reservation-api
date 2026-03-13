package com.ing.assesment.domain.event.command.handler;

import com.ing.assesment.domain.auth.model.AuthenticatedUser;
import com.ing.assesment.domain.auth.port.CurrentUserPort;
import com.ing.assesment.domain.common.CommandHandler;
import com.ing.assesment.domain.common.exception.AccessDeniedException;
import com.ing.assesment.domain.event.command.CreateEventCommand;
import com.ing.assesment.domain.event.model.Event;
import com.ing.assesment.domain.event.port.EventRepositoryPort;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateEventCommandHandler implements CommandHandler<CreateEventCommand, Event> {

    private final EventRepositoryPort eventRepository;
    private final CurrentUserPort currentUserPort;

    @Override
    public Event handle(CreateEventCommand command) {
        AuthenticatedUser currentUser = currentUserPort.getCurrentUser();

        if (!currentUser.isAdmin() && !currentUser.isOrganizer()) {
            throw new AccessDeniedException("Only organizer or admin can create events");
        }

        Event event = Event.createDraft(
                currentUser.id(),
                command.title(),
                command.venue(),
                command.startsAt(),
                command.endsAt(),
                command.capacity()
        );

        return eventRepository.save(event);
    }
}
