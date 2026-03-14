package com.ing.assesment.infra.event.api;

import com.ing.assesment.domain.audit.model.AuditAction;
import com.ing.assesment.domain.audit.model.AuditResourceType;
import com.ing.assesment.domain.common.handler.CommandHandler;
import com.ing.assesment.domain.event.command.CreateEventCommand;
import com.ing.assesment.domain.event.command.PublishEventCommand;
import com.ing.assesment.domain.event.command.UpdateEventCommand;
import com.ing.assesment.domain.event.model.Event;
import com.ing.assesment.domain.event.query.ListEventsQuery;
import com.ing.assesment.domain.event.query.SearchPublicEventsQuery;
import com.ing.assesment.infra.audit.aop.Auditable;
import com.ing.assesment.infra.event.api.request.CreateEventRequest;
import com.ing.assesment.infra.event.api.request.PublishEventRequest;
import com.ing.assesment.infra.event.api.request.UpdateEventRequest;
import com.ing.assesment.infra.event.api.response.CreateEventResponse;
import com.ing.assesment.infra.event.api.response.EventResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final CommandHandler<CreateEventCommand, Event> createEventCommandHandler;
    private final CommandHandler<UpdateEventCommand, Event> updateEventCommandHandler;
    private final CommandHandler<PublishEventCommand, Event> publishEventCommandHandler;
    private final CommandHandler<ListEventsQuery, List<Event>> listEventsQueryHandler;
    private final CommandHandler<SearchPublicEventsQuery, List<Event>> searchPublicEventsQueryHandler;


    @Operation(summary = "Create draft event")
    @ApiResponse(responseCode = "201", description = "Draft event created")
    @ApiResponse(responseCode = "403", description = "Only organizer or admin can create events")
    @Auditable(action = AuditAction.CREATE_EVENT, resourceType = AuditResourceType.EVENT)
    @PostMapping
    public ResponseEntity<CreateEventResponse> create(@Valid @RequestBody CreateEventRequest request) {
        Event event = createEventCommandHandler.handle(
                new CreateEventCommand(
                        request.title(),
                        request.venue(),
                        request.startsAt(),
                        request.endsAt(),
                        request.capacity()
                )
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreateEventResponse(
                        event.getId(),
                        event.getOwnerId(),
                        event.getTitle(),
                        event.getVenue(),
                        event.getStartsAt(),
                        event.getEndsAt(),
                        event.getCapacity(),
                        event.isPublished(),
                        event.getVersion()
                ));
    }

    @Operation(summary = "Update existing event")
    @ApiResponse(responseCode = "200", description = "Event updated")
    @ApiResponse(responseCode = "403", description = "Only owner or admin can update event")
    @ApiResponse(responseCode = "404", description = "Event not found")
    @ApiResponse(responseCode = "409", description = "Concurrent update conflict")
    @Auditable(action = AuditAction.UPDATE_EVENT, resourceType = AuditResourceType.EVENT)
    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody UpdateEventRequest request) {
        Event event = updateEventCommandHandler.handle(
                new UpdateEventCommand(
                        id,
                        request.title(),
                        request.venue(),
                        request.startsAt(),
                        request.endsAt(),
                        request.capacity(),
                        request.version()
                )
        );

        return ResponseEntity.ok(toResponse(event));
    }

    @Operation(summary = "Publish draft event")
    @ApiResponse(responseCode = "200", description = "Event published successfully")
    @ApiResponse(responseCode = "403", description = "Only owner or admin can publish the event")
    @ApiResponse(responseCode = "404", description = "Event not found")
    @ApiResponse(responseCode = "409", description = "Event is already published or update conflict occurred")
    @Auditable(action = AuditAction.PUBLISH_EVENT, resourceType = AuditResourceType.EVENT)
    @PostMapping("/{id}/publish")
    public ResponseEntity<EventResponse> publish(@PathVariable Long id,
                                                 @Valid @RequestBody PublishEventRequest request) {
        Event event = publishEventCommandHandler.handle(
                new PublishEventCommand(id, request.version())
        );

        return ResponseEntity.ok(toResponse(event));
    }

    @Operation(summary = "List events with optional owner filter")
    @ApiResponse(responseCode = "200", description = "Events listed successfully")
    @ApiResponse(responseCode = "403", description = "Only organizer or admin can list events")
    @GetMapping
    public ResponseEntity<List<EventResponse>> list(@RequestParam(required = false) Long ownerId) {
        List<EventResponse> response = listEventsQueryHandler.handle(new ListEventsQuery(ownerId))
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Search public events",
            description = "Returns published events with optional date range and text filtering."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Public events listed successfully")
    })
    @GetMapping("/public")
    public ResponseEntity<List<EventResponse>> searchPublic(
            @Parameter(description = "Lower bound for event start time")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant from,

            @Parameter(description = "Upper bound for event start time")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant to,

            @Parameter(description = "Search text for title or venue")
            @RequestParam(required = false)
            String q) {
        List<EventResponse> response = searchPublicEventsQueryHandler.handle(
                        new SearchPublicEventsQuery(from, to, q)
                ).stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    private EventResponse toResponse(Event event) {
        return new EventResponse(
                event.getId(),
                event.getOwnerId(),
                event.getTitle(),
                event.getVenue(),
                event.getStartsAt(),
                event.getEndsAt(),
                event.getCapacity(),
                event.isPublished(),
                event.getVersion()
        );
    }
}
