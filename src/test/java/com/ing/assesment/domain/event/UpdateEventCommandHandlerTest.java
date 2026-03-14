package com.ing.assesment.domain.event;

import com.ing.assesment.domain.auth.model.AuthenticatedUser;
import com.ing.assesment.domain.auth.model.UserRole;
import com.ing.assesment.domain.auth.port.CurrentUserPort;
import com.ing.assesment.domain.common.exception.AccessDeniedException;
import com.ing.assesment.domain.common.exception.EventNotFoundException;
import com.ing.assesment.domain.event.command.UpdateEventCommand;
import com.ing.assesment.domain.event.command.handler.UpdateEventCommandHandler;
import com.ing.assesment.domain.event.model.Event;
import com.ing.assesment.domain.event.port.EventRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateEventCommandHandlerTest {

    @Mock
    private EventRepositoryPort eventRepository;

    @Mock
    private CurrentUserPort currentUserPort;

    private UpdateEventCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new UpdateEventCommandHandler(eventRepository, currentUserPort);
    }

    @Test
    void shouldUpdateEventWhenCallerIsOwner() {
        UpdateEventCommand command = new UpdateEventCommand(
                10L,
                "Updated Title",
                "Updated Venue",
                Instant.parse("2026-09-01T09:00:00Z"),
                Instant.parse("2026-09-01T18:00:00Z"),
                250,
                3L
        );

        Event existingEvent = Event.createDraft(
                101L,
                "Old Title",
                "Old Venue",
                Instant.parse("2026-09-01T08:00:00Z"),
                Instant.parse("2026-09-01T17:00:00Z"),
                100
        );
        existingEvent.setId(10L);
        existingEvent.setOwnerId(101L);
        existingEvent.setPublished(false);
        existingEvent.setVersion(2L);

        AuthenticatedUser owner = new AuthenticatedUser(
                101L,
                "owner@test.com",
                Set.of(UserRole.ORGANIZER)
        );

        when(currentUserPort.getCurrentUser()).thenReturn(owner);
        when(eventRepository.findById(10L)).thenReturn(Optional.of(existingEvent));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event result = handler.handle(command);

        assertNotNull(result);
        assertEquals("Updated Title", result.getTitle());
        assertEquals("Updated Venue", result.getVenue());
        assertEquals(250, result.getCapacity());
        assertEquals(3L, result.getVersion());

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());

        Event savedEvent = captor.getValue();
        assertEquals(10L, savedEvent.getId());
        assertEquals(101L, savedEvent.getOwnerId());
        assertEquals("Updated Title", savedEvent.getTitle());
        assertEquals("Updated Venue", savedEvent.getVenue());
        assertEquals(3L, savedEvent.getVersion());
    }

    @Test
    void shouldUpdateEventWhenCallerIsAdmin() {
        UpdateEventCommand command = new UpdateEventCommand(
                10L,
                "Admin Updated Title",
                "Admin Venue",
                Instant.parse("2026-10-01T09:00:00Z"),
                Instant.parse("2026-10-01T19:00:00Z"),
                500,
                5L
        );

        Event existingEvent = Event.createDraft(
                200L,
                "Existing",
                "Venue",
                Instant.parse("2026-10-01T08:00:00Z"),
                Instant.parse("2026-10-01T17:00:00Z"),
                100
        );
        existingEvent.setId(10L);
        existingEvent.setOwnerId(200L);
        existingEvent.setVersion(4L);

        AuthenticatedUser admin = new AuthenticatedUser(
                1L,
                "admin@test.com",
                Set.of(UserRole.ADMIN)
        );

        when(currentUserPort.getCurrentUser()).thenReturn(admin);
        when(eventRepository.findById(10L)).thenReturn(Optional.of(existingEvent));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event result = handler.handle(command);

        assertEquals("Admin Updated Title", result.getTitle());
        assertEquals("Admin Venue", result.getVenue());
        assertEquals(500, result.getCapacity());
        assertEquals(5L, result.getVersion());
    }

    @Test
    void shouldThrowWhenCallerIsNeitherOwnerNorAdmin() {
        UpdateEventCommand command = new UpdateEventCommand(
                10L,
                "Updated Title",
                "Updated Venue",
                Instant.parse("2026-09-01T09:00:00Z"),
                Instant.parse("2026-09-01T18:00:00Z"),
                250,
                1L
        );

        Event existingEvent = Event.createDraft(
                101L,
                "Old Title",
                "Old Venue",
                Instant.parse("2026-09-01T08:00:00Z"),
                Instant.parse("2026-09-01T17:00:00Z"),
                100
        );
        existingEvent.setId(10L);
        existingEvent.setOwnerId(101L);

        AuthenticatedUser anotherUser = new AuthenticatedUser(
                999L,
                "other@test.com",
                Set.of(UserRole.ORGANIZER)
        );

        when(currentUserPort.getCurrentUser()).thenReturn(anotherUser);
        when(eventRepository.findById(10L)).thenReturn(Optional.of(existingEvent));

        assertThrows(AccessDeniedException.class, () -> handler.handle(command));

        verify(eventRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenEventNotFound() {
        UpdateEventCommand command = new UpdateEventCommand(
                999L,
                "Updated Title",
                "Updated Venue",
                Instant.parse("2026-09-01T09:00:00Z"),
                Instant.parse("2026-09-01T18:00:00Z"),
                250,
                1L
        );

        AuthenticatedUser admin = new AuthenticatedUser(
                1L,
                "admin@test.com",
                Set.of(UserRole.ADMIN)
        );

        when(currentUserPort.getCurrentUser()).thenReturn(admin);
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EventNotFoundException.class, () -> handler.handle(command));

        verify(eventRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenDatesAreInvalid() {
        UpdateEventCommand command = new UpdateEventCommand(
                10L,
                "Updated Title",
                "Updated Venue",
                Instant.parse("2026-09-01T18:00:00Z"),
                Instant.parse("2026-09-01T09:00:00Z"),
                250,
                1L
        );

        Event existingEvent = Event.createDraft(
                101L,
                "Old Title",
                "Old Venue",
                Instant.parse("2026-09-01T08:00:00Z"),
                Instant.parse("2026-09-01T17:00:00Z"),
                100
        );
        existingEvent.setId(10L);
        existingEvent.setOwnerId(101L);

        AuthenticatedUser owner = new AuthenticatedUser(
                101L,
                "owner@test.com",
                Set.of(UserRole.ORGANIZER)
        );

        when(currentUserPort.getCurrentUser()).thenReturn(owner);
        when(eventRepository.findById(10L)).thenReturn(Optional.of(existingEvent));

        assertThrows(IllegalArgumentException.class, () -> handler.handle(command));

        verify(eventRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenCapacityIsInvalid() {
        UpdateEventCommand command = new UpdateEventCommand(
                10L,
                "Updated Title",
                "Updated Venue",
                Instant.parse("2026-09-01T09:00:00Z"),
                Instant.parse("2026-09-01T18:00:00Z"),
                0,
                1L
        );

        Event existingEvent = Event.createDraft(
                101L,
                "Old Title",
                "Old Venue",
                Instant.parse("2026-09-01T08:00:00Z"),
                Instant.parse("2026-09-01T17:00:00Z"),
                100
        );
        existingEvent.setId(10L);
        existingEvent.setOwnerId(101L);

        AuthenticatedUser owner = new AuthenticatedUser(
                101L,
                "owner@test.com",
                Set.of(UserRole.ORGANIZER)
        );

        when(currentUserPort.getCurrentUser()).thenReturn(owner);
        when(eventRepository.findById(10L)).thenReturn(Optional.of(existingEvent));

        assertThrows(IllegalArgumentException.class, () -> handler.handle(command));

        verify(eventRepository, never()).save(any());
    }
}
