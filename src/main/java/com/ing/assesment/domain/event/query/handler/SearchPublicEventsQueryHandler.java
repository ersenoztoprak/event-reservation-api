package com.ing.assesment.domain.event.query.handler;

import com.ing.assesment.domain.common.handler.CommandHandler;
import com.ing.assesment.domain.event.model.Event;
import com.ing.assesment.domain.event.port.EventRepositoryPort;
import com.ing.assesment.domain.event.query.SearchPublicEventsQuery;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class SearchPublicEventsQueryHandler implements CommandHandler<SearchPublicEventsQuery, List<Event>> {

    private final EventRepositoryPort eventRepository;

    public List<Event> handle(SearchPublicEventsQuery query) {
        return eventRepository.searchPublishedEvents(
                query.from(),
                query.to(),
                query.q()
        );
    }
}
