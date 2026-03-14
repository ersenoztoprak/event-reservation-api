package com.ing.assesment.domain.event;

import com.ing.assesment.domain.auth.model.AuthenticatedUser;
import com.ing.assesment.domain.auth.model.UserRole;
import com.ing.assesment.domain.auth.port.CurrentUserPort;
import com.ing.assesment.domain.common.exception.AccessDeniedException;
import com.ing.assesment.domain.common.exception.EventAlreadyPublishedException;
import com.ing.assesment.domain.common.exception.EventNotFoundException;
import com.ing.assesment.domain.event.command.PublishEventCommand;
import com.ing.assesment.domain.event.command.handler.PublishEventCommandHandler;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublishEventCommandHandlerTest {

    @Mock
    private EventRepositoryPort eventRepository;

    @Mock
    private CurrentUserPort currentUserPort;

    private PublishEventCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new PublishEventCommandHandler(eventRepository, currentUserPort);
    }

    @Test
    void shouldPublishEventWhenCallerIsOwner() {
        PublishEventCommand command = new PublishEventCommand(10L, 2L);

        Event event = Event.createDraft(
                101L,
                "Spring Summit",
                "Istanbul",
                Instant.parse("2027-02-10T09:00:00Z"),
                Instant.parse("2027-02-10T18:00:00Z"),
                300
        );
        event.setId(10L);
        event.setOwnerId(101L);
        event.setPublished(false);
        event.setVersion(1L);

        AuthenticatedUser owner = new AuthenticatedUser(
                101L,
                "owner@test.com",
                Set.of(UserRole.ORGANIZER)
        );

        when(currentUserPort.getCurrentUser()).thenReturn(owner);
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event result = handler.handle(command);

        assertNotNull(result);
        assertTrue(result.isPublished());
        assertEquals(2L, result.getVersion());

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());

        Event savedEvent = captor.getValue();
        assertTrue(savedEvent.isPublished());
        assertEquals(2L, savedEvent.getVersion());
    }

    @Test
    void shouldPublishEventWhenCallerIsAdmin() {
        PublishEventCommand command = new PublishEventCommand(11L, 5L);

        Event event = Event.createDraft(
                200L,
                "Admin Publish Event",
                "Ankara",
                Instant.parse("2027-03-01T09:00:00Z"),
                Instant.parse("2027-03-01T17:00:00Z"),
                150
        );
        event.setId(11L);
        event.setOwnerId(200L);
        event.setPublished(false);
        event.setVersion(4L);

        AuthenticatedUser admin = new AuthenticatedUser(
                1L,
                "admin@test.com",
                Set.of(UserRole.ADMIN)
        );

        when(currentUserPort.getCurrentUser()).thenReturn(admin);
        when(eventRepository.findById(11L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event result = handler.handle(command);

        assertTrue(result.isPublished());
        assertEquals(5L, result.getVersion());
    }

    @Test
    void shouldThrowWhenCallerIsNeitherOwnerNorAdmin() {
        PublishEventCommand command = new PublishEventCommand(12L, 1L);

        Event event = Event.createDraft(
                101L,
                "Restricted Event",
                "Izmir",
                Instant.parse("2027-04-01T09:00:00Z"),
                Instant.parse("2027-04-01T17:00:00Z"),
                100
        );
        event.setId(12L);
        event.setOwnerId(101L);
        event.setPublished(false);
        event.setVersion(0L);

        AuthenticatedUser anotherUser = new AuthenticatedUser(
                999L,
                "other@test.com",
                Set.of(UserRole.ORGANIZER)
        );

        when(currentUserPort.getCurrentUser()).thenReturn(anotherUser);
        when(eventRepository.findById(12L)).thenReturn(Optional.of(event));

        assertThrows(AccessDeniedException.class, () -> handler.handle(command));

        verify(eventRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenEventNotFound() {
        PublishEventCommand command = new PublishEventCommand(999L, 0L);

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
    void shouldThrowWhenEventAlreadyPublished() {
        PublishEventCommand command = new PublishEventCommand(20L, 3L);

        Event event = Event.createDraft(
                101L,
                "Published Event",
                "Bursa",
                Instant.parse("2027-05-01T09:00:00Z"),
                Instant.parse("2027-05-01T17:00:00Z"),
                200
        );
        event.setId(20L);
        event.setOwnerId(101L);
        event.setPublished(true);
        event.setVersion(2L);

        AuthenticatedUser owner = new AuthenticatedUser(
                101L,
                "owner@test.com",
                Set.of(UserRole.ORGANIZER)
        );

        when(currentUserPort.getCurrentUser()).thenReturn(owner);
        when(eventRepository.findById(20L)).thenReturn(Optional.of(event));

        assertThrows(EventAlreadyPublishedException.class, () -> handler.handle(command));

        verify(eventRepository, never()).save(any());
    }
}
