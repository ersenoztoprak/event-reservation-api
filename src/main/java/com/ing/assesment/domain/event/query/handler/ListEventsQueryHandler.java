package com.ing.assesment.domain.event.query.handler;

import com.ing.assesment.domain.auth.model.AuthenticatedUser;
import com.ing.assesment.domain.auth.port.CurrentUserPort;
import com.ing.assesment.domain.common.exception.AccessDeniedException;
import com.ing.assesment.domain.common.handler.CommandHandler;
import com.ing.assesment.domain.event.model.Event;
import com.ing.assesment.domain.event.port.EventRepositoryPort;
import com.ing.assesment.domain.event.query.ListEventsQuery;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ListEventsQueryHandler implements CommandHandler<ListEventsQuery, List<Event>> {

    private final EventRepositoryPort eventRepository;
    private final CurrentUserPort currentUserPort;

    public List<Event> handle(ListEventsQuery query) {
        AuthenticatedUser currentUser = currentUserPort.getCurrentUser();

        if (currentUser.isAdmin()) {
            if (query.ownerId() != null) {
                return eventRepository.findAllByOwnerId(query.ownerId());
            }
            return eventRepository.findAll();
        }

        if (currentUser.isOrganizer()) {
            if (query.ownerId() != null && !query.ownerId().equals(currentUser.id())) {
                throw new AccessDeniedException("Organizer can only list own events");
            }
            return eventRepository.findAllByOwnerId(currentUser.id());
        }

        throw new AccessDeniedException("Only organizer or admin can list events");
    }
}
