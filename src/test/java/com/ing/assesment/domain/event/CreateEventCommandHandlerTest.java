package com.ing.assesment.domain.event;

import com.ing.assesment.domain.auth.model.AuthenticatedUser;
import com.ing.assesment.domain.auth.model.UserRole;
import com.ing.assesment.domain.auth.port.CurrentUserPort;
import com.ing.assesment.domain.common.exception.AccessDeniedException;
import com.ing.assesment.domain.event.command.CreateEventCommand;
import com.ing.assesment.domain.event.command.handler.CreateEventCommandHandler;
import com.ing.assesment.domain.event.model.Event;
import com.ing.assesment.domain.event.port.EventRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateEventCommandHandlerTest {

    @Mock
    private EventRepositoryPort eventRepository;

    @Mock
    private CurrentUserPort currentUserPort;

    private CreateEventCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CreateEventCommandHandler(eventRepository, currentUserPort);
    }

    @Test
    void shouldCreateDraftEventForOrganizer() {
        CreateEventCommand command = new CreateEventCommand(
                "Java Conference",
                "Istanbul Congress Center",
                Instant.parse("2026-04-10T10:00:00Z"),
                Instant.parse("2026-04-10T18:00:00Z"),
                250
        );

        AuthenticatedUser organizer = new AuthenticatedUser(
                101L,
                "organizer@test.com",
                Set.of(UserRole.ORGANIZER)
        );

        when(currentUserPort.getCurrentUser()).thenReturn(organizer);
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            event.setId(1L);
            event.setVersion(0L);
            return event;
        });

        Event result = handler.handle(command);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(101L, result.getOwnerId());
        assertEquals("Java Conference", result.getTitle());
        assertEquals("Istanbul Congress Center", result.getVenue());
        assertEquals(250, result.getCapacity());
        assertFalse(result.isPublished());
        assertEquals(0L, result.getVersion());

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(eventCaptor.capture());

        Event savedEvent = eventCaptor.getValue();
        assertEquals(101L, savedEvent.getOwnerId());
        assertEquals("Java Conference", savedEvent.getTitle());
        assertFalse(savedEvent.isPublished());
    }

    @Test
    void shouldCreateDraftEventForAdmin() {
        CreateEventCommand command = new CreateEventCommand(
                "Admin Event",
                "Ankara",
                Instant.parse("2026-05-01T09:00:00Z"),
                Instant.parse("2026-05-01T17:00:00Z"),
                100
        );

        AuthenticatedUser admin = new AuthenticatedUser(
                1L,
                "admin@test.com",
                Set.of(UserRole.ADMIN)
        );

        when(currentUserPort.getCurrentUser()).thenReturn(admin);
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event result = handler.handle(command);

        assertEquals(1L, result.getOwnerId());
        assertFalse(result.isPublished());
    }

    @Test
    void shouldThrowWhenUserIsCustomer() {
        CreateEventCommand command = new CreateEventCommand(
                "Forbidden Event",
                "Izmir",
                Instant.parse("2026-06-01T09:00:00Z"),
                Instant.parse("2026-06-01T17:00:00Z"),
                50
        );

        AuthenticatedUser customer = new AuthenticatedUser(
                200L,
                "customer@test.com",
                Set.of(UserRole.CUSTOMER)
        );

        when(currentUserPort.getCurrentUser()).thenReturn(customer);

        assertThrows(AccessDeniedException.class, () -> handler.handle(command));

        verify(eventRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenDatesAreInvalid() {
        CreateEventCommand command = new CreateEventCommand(
                "Broken Event",
                "Bursa",
                Instant.parse("2026-07-01T18:00:00Z"),
                Instant.parse("2026-07-01T10:00:00Z"),
                50
        );

        AuthenticatedUser organizer = new AuthenticatedUser(
                101L,
                "organizer@test.com",
                Set.of(UserRole.ORGANIZER)
        );

        when(currentUserPort.getCurrentUser()).thenReturn(organizer);

        assertThrows(IllegalArgumentException.class, () -> handler.handle(command));

        verify(eventRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenCapacityIsInvalid() {
        CreateEventCommand command = new CreateEventCommand(
                "Broken Event",
                "Bursa",
                Instant.parse("2026-07-01T10:00:00Z"),
                Instant.parse("2026-07-01T18:00:00Z"),
                0
        );

        AuthenticatedUser organizer = new AuthenticatedUser(
                101L,
                "organizer@test.com",
                Set.of(UserRole.ORGANIZER)
        );

        when(currentUserPort.getCurrentUser()).thenReturn(organizer);

        assertThrows(IllegalArgumentException.class, () -> handler.handle(command));

        verify(eventRepository, never()).save(any());
    }

}
