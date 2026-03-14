package com.ing.assesment.domain.event;

import com.ing.assesment.domain.auth.model.AuthenticatedUser;
import com.ing.assesment.domain.auth.model.UserRole;
import com.ing.assesment.domain.auth.port.CurrentUserPort;
import com.ing.assesment.domain.common.exception.AccessDeniedException;
import com.ing.assesment.domain.event.model.Event;
import com.ing.assesment.domain.event.port.EventRepositoryPort;
import com.ing.assesment.domain.event.query.ListEventsQuery;
import com.ing.assesment.domain.event.query.handler.ListEventsQueryHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListEventsQueryHandlerTest {

    @Mock
    private EventRepositoryPort eventRepository;

    @Mock
    private CurrentUserPort currentUserPort;

    private ListEventsQueryHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ListEventsQueryHandler(eventRepository, currentUserPort);
    }

    @Test
    void shouldListAllEventsWhenCallerIsAdminAndOwnerIdIsNull() {
        AuthenticatedUser admin = new AuthenticatedUser(
                1L,
                "admin@test.com",
                Set.of(UserRole.ADMIN)
        );

        List<Event> events = List.of(
                createEvent(10L, 101L, "Event 1"),
                createEvent(11L, 102L, "Event 2")
        );

        when(currentUserPort.getCurrentUser()).thenReturn(admin);
        when(eventRepository.findAll()).thenReturn(events);

        List<Event> result = handler.handle(new ListEventsQuery(null));

        assertEquals(2, result.size());
        verify(eventRepository).findAll();
        verify(eventRepository, never()).findAllByOwnerId(anyLong());
    }

    @Test
    void shouldFilterByOwnerIdWhenCallerIsAdmin() {
        AuthenticatedUser admin = new AuthenticatedUser(
                1L,
                "admin@test.com",
                Set.of(UserRole.ADMIN)
        );

        List<Event> events = List.of(
                createEvent(10L, 101L, "Event 1"),
                createEvent(11L, 101L, "Event 2")
        );

        when(currentUserPort.getCurrentUser()).thenReturn(admin);
        when(eventRepository.findAllByOwnerId(101L)).thenReturn(events);

        List<Event> result = handler.handle(new ListEventsQuery(101L));

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(event -> event.getOwnerId().equals(101L)));
        verify(eventRepository).findAllByOwnerId(101L);
        verify(eventRepository, never()).findAll();
    }

    @Test
    void shouldListOwnEventsWhenCallerIsOrganizerAndOwnerIdIsNull() {
        AuthenticatedUser organizer = new AuthenticatedUser(
                101L,
                "organizer@test.com",
                Set.of(UserRole.ORGANIZER)
        );

        List<Event> events = List.of(
                createEvent(10L, 101L, "Event 1"),
                createEvent(11L, 101L, "Event 2")
        );

        when(currentUserPort.getCurrentUser()).thenReturn(organizer);
        when(eventRepository.findAllByOwnerId(101L)).thenReturn(events);

        List<Event> result = handler.handle(new ListEventsQuery(null));

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(event -> event.getOwnerId().equals(101L)));
        verify(eventRepository).findAllByOwnerId(101L);
        verify(eventRepository, never()).findAll();
    }

    @Test
    void shouldListOwnEventsWhenOrganizerRequestsOwnOwnerId() {
        AuthenticatedUser organizer = new AuthenticatedUser(
                101L,
                "organizer@test.com",
                Set.of(UserRole.ORGANIZER)
        );

        List<Event> events = List.of(
                createEvent(10L, 101L, "Event 1")
        );

        when(currentUserPort.getCurrentUser()).thenReturn(organizer);
        when(eventRepository.findAllByOwnerId(101L)).thenReturn(events);

        List<Event> result = handler.handle(new ListEventsQuery(101L));

        assertEquals(1, result.size());
        assertEquals(101L, result.get(0).getOwnerId());
        verify(eventRepository).findAllByOwnerId(101L);
    }

    @Test
    void shouldThrowWhenOrganizerRequestsAnotherOwnersEvents() {
        AuthenticatedUser organizer = new AuthenticatedUser(
                101L,
                "organizer@test.com",
                Set.of(UserRole.ORGANIZER)
        );

        when(currentUserPort.getCurrentUser()).thenReturn(organizer);

        assertThrows(AccessDeniedException.class, () -> handler.handle(new ListEventsQuery(999L)));

        verify(eventRepository, never()).findAll();
        verify(eventRepository, never()).findAllByOwnerId(anyLong());
    }

    @Test
    void shouldThrowWhenCallerIsCustomer() {
        AuthenticatedUser customer = new AuthenticatedUser(
                201L,
                "customer@test.com",
                Set.of(UserRole.CUSTOMER)
        );

        when(currentUserPort.getCurrentUser()).thenReturn(customer);

        assertThrows(AccessDeniedException.class, () -> handler.handle(new ListEventsQuery(null)));

        verify(eventRepository, never()).findAll();
        verify(eventRepository, never()).findAllByOwnerId(anyLong());
    }

    private Event createEvent(Long id, Long ownerId, String title) {
        Event event = Event.createDraft(
                ownerId,
                title,
                "Venue",
                Instant.parse("2027-12-01T09:00:00Z"),
                Instant.parse("2027-12-01T17:00:00Z"),
                100
        );
        event.setId(id);
        event.setOwnerId(ownerId);
        event.setPublished(false);
        event.setVersion(0L);
        return event;
    }
}
