package com.ing.assesment.domain.event.command.handler;

import com.ing.assesment.domain.auth.model.AuthenticatedUser;
import com.ing.assesment.domain.auth.port.CurrentUserPort;
import com.ing.assesment.domain.common.CommandHandler;
import com.ing.assesment.domain.common.exception.AccessDeniedException;
import com.ing.assesment.domain.common.exception.EventNotFoundException;
import com.ing.assesment.domain.event.command.PublishEventCommand;
import com.ing.assesment.domain.event.model.Event;
import com.ing.assesment.domain.event.port.EventRepositoryPort;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PublishEventCommandHandler implements CommandHandler<PublishEventCommand, Event> {

    private final EventRepositoryPort eventRepository;
    private final CurrentUserPort currentUserPort;

    @Override
    public Event handle(PublishEventCommand command) {
        AuthenticatedUser currentUser = currentUserPort.getCurrentUser();

        Event event = eventRepository.findById(command.eventId())
                .orElseThrow(() -> new EventNotFoundException(command.eventId()));

        boolean isAdmin = currentUser.isAdmin();
        boolean isOwner = event.isOwnedBy(currentUser.id());

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("Only event owner or admin can publish event");
        }

        event.publish();
        event.setVersion(command.version());

        return eventRepository.save(event);
    }
}
