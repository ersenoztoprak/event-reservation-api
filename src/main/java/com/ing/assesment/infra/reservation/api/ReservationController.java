package com.ing.assesment.infra.reservation.api;

import com.ing.assesment.domain.audit.model.AuditAction;
import com.ing.assesment.domain.audit.model.AuditResourceType;
import com.ing.assesment.domain.common.handler.CommandHandler;
import com.ing.assesment.domain.reservation.command.CancelReservationCommand;
import com.ing.assesment.domain.reservation.command.ConfirmReservationCommand;
import com.ing.assesment.domain.reservation.command.CreateReservationCommand;
import com.ing.assesment.domain.reservation.model.Reservation;
import com.ing.assesment.infra.audit.aop.Auditable;
import com.ing.assesment.infra.reservation.api.request.CreateReservationRequest;
import com.ing.assesment.infra.reservation.api.response.ReservationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReservationController {

    private final CommandHandler<CreateReservationCommand, Reservation> createReservationCommandHandler;
    private final CommandHandler<ConfirmReservationCommand, Reservation> confirmReservationCommandHandler;
    private final CommandHandler<CancelReservationCommand, Reservation> cancelReservationCommandHandler;


    @Operation(summary = "Reserves seats from an event")
    @ApiResponse(responseCode = "201", description = "User created")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "409", description = "Conflict")
    @Auditable(action = AuditAction.CREATE_RESERVATION, resourceType = AuditResourceType.RESERVATION)
    @PostMapping("/events/{id}/reservations")
    public ResponseEntity<ReservationResponse> create(@PathVariable Long id,
                                                      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                      @Valid @RequestBody CreateReservationRequest request) {
        Reservation reservation = createReservationCommandHandler.handle(
                new CreateReservationCommand(
                        id,
                        request.seats(),
                        idempotencyKey));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(reservation));
    }

    @Operation(
            summary = "Confirm reservation",
            description = "Confirms a PENDING reservation. Only reservation owner or ADMIN can confirm."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reservation confirmed",
                    content = @Content(schema = @Schema(implementation = ReservationResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Reservation not found"),
            @ApiResponse(responseCode = "409", description = "Invalid reservation state")
    })
    @Auditable(action = AuditAction.CONFIRM_RESERVATION, resourceType = AuditResourceType.RESERVATION)
    @PostMapping("/reservations/{id}/confirm")
    public ResponseEntity<ReservationResponse> confirm(@PathVariable Long id) {
        Reservation reservation = confirmReservationCommandHandler.handle(
                new ConfirmReservationCommand(id));

        return ResponseEntity.ok(toResponse(reservation));
    }

    @Operation(
            summary = "Cancel reservation",
            description = "Cancels a reservation. Only reservation owner or ADMIN can cancel."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reservation cancelled",
                    content = @Content(schema = @Schema(implementation = ReservationResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Reservation not found"),
            @ApiResponse(responseCode = "409", description = "Invalid reservation state")
    })
    @Auditable(action = AuditAction.CANCEL_RESERVATION, resourceType = AuditResourceType.RESERVATION)
    @PostMapping("/reservations/{id}/cancel")
    public ResponseEntity<ReservationResponse> cancel(@PathVariable Long id) {
        Reservation reservation = cancelReservationCommandHandler.handle(
                new CancelReservationCommand(id)
        );

        return ResponseEntity.ok(toResponse(reservation));
    }

    private ReservationResponse toResponse(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getEventId(),
                reservation.getUserId(),
                reservation.getStatus(),
                reservation.getSeats(),
                reservation.getCreatedAt());
    }
}
