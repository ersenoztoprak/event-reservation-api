package com.ing.assesment.domain.event;

import com.ing.assesment.domain.event.model.Event;
import com.ing.assesment.domain.event.port.EventRepositoryPort;
import com.ing.assesment.domain.event.query.SearchPublicEventsQuery;
import com.ing.assesment.domain.event.query.handler.SearchPublicEventsQueryHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchPublicEventsQueryHandlerTest {

    @Mock
    private EventRepositoryPort eventRepository;

    private SearchPublicEventsQueryHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SearchPublicEventsQueryHandler(eventRepository);
    }

    @Test
    void shouldReturnPublishedEventsFromRepository() {
        Instant from = Instant.parse("2027-01-01T00:00:00Z");
        Instant to = Instant.parse("2027-12-31T23:59:59Z");
        String q = "spring";

        List<Event> events = List.of(
                createEvent(1L, 100L, "Spring Summit", "Istanbul", true),
                createEvent(2L, 101L, "Java Days", "Ankara", true)
        );

        when(eventRepository.searchPublishedEvents(from, to, q)).thenReturn(events);

        List<Event> result = handler.handle(new SearchPublicEventsQuery(from, to, q));

        assertEquals(2, result.size());
        assertEquals("Spring Summit", result.get(0).getTitle());
        assertEquals("Java Days", result.get(1).getTitle());

        verify(eventRepository).searchPublishedEvents(from, to, q);
    }

    @Test
    void shouldPassNullFiltersToRepository() {
        when(eventRepository.searchPublishedEvents(null, null, null)).thenReturn(List.of());

        List<Event> result = handler.handle(new SearchPublicEventsQuery(null, null, null));

        assertEquals(0, result.size());
        verify(eventRepository).searchPublishedEvents(null, null, null);
    }

    private Event createEvent(Long id, Long ownerId, String title, String venue, boolean published) {
        Event event = Event.createDraft(
                ownerId,
                title,
                venue,
                Instant.parse("2027-06-10T09:00:00Z"),
                Instant.parse("2027-06-10T17:00:00Z"),
                100
        );
        event.setId(id);
        event.setPublished(published);
        event.setVersion(0L);
        return event;
    }
}
