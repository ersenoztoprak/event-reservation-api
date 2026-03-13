package com.ing.assesment.infra.common.exception;

import com.ing.assesment.domain.common.exception.AccessDeniedException;
import com.ing.assesment.domain.common.exception.EventAlreadyPublishedException;
import com.ing.assesment.domain.common.exception.EventAlreadyStartedException;
import com.ing.assesment.domain.common.exception.EventNotFoundException;
import com.ing.assesment.domain.common.exception.EventNotPublishedException;
import com.ing.assesment.domain.common.exception.IdempotencyConflictException;
import com.ing.assesment.domain.common.exception.IdempotencyKeyRequiredException;
import com.ing.assesment.domain.common.exception.InvalidCredentialsException;
import com.ing.assesment.domain.common.exception.InvalidRefreshTokenException;
import com.ing.assesment.domain.common.exception.InvalidReservationStateException;
import com.ing.assesment.domain.common.exception.OptimisticLockConflictException;
import com.ing.assesment.domain.common.exception.ReservationCapacityExceededException;
import com.ing.assesment.domain.common.exception.ReservationNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        log.error("Invalid Credential: ", ex);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problemDetail.setTitle("Unauthorized");
        return problemDetail;
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ProblemDetail handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
        log.error("Invalid Refresh Token: ", ex);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problemDetail.setTitle("Unauthorized");
        return problemDetail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArg(IllegalArgumentException ex) {
        log.error("A business exception occurred: ", ex);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Bad Request");
        return problemDetail;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        log.error("Access Denied: ", ex);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problemDetail.setTitle("Forbidden");
        return problemDetail;
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ProblemDetail handleSpringAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
        log.error("Spring Access Denied: ", ex);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problemDetail.setTitle("Forbidden");
        return problemDetail;
    }

    @ExceptionHandler(EventNotFoundException.class)
    public ProblemDetail handleEventNotFound(EventNotFoundException ex) {
        log.error("Event Not Found: ", ex);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Not Found");
        return problemDetail;
    }

    @ExceptionHandler(EventAlreadyPublishedException.class)
    public ProblemDetail handleEventAlreadyPublished(EventAlreadyPublishedException ex) {
        log.error("Event Already Published: ", ex);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Conflict");
        return problemDetail;
    }

    @ExceptionHandler(IdempotencyKeyRequiredException.class)
    public ProblemDetail handleIdempotencyKeyRequired(IdempotencyKeyRequiredException ex) {
        log.error("Idempotency Key Required: ", ex);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setTitle("Bad Request");
        return problemDetail;
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ProblemDetail handleIdempotencyConflict(IdempotencyConflictException ex) {
        log.error("Idempotency Conflict: ", ex);
        var problem = ProblemDetail.forStatusAndDetail(
                org.springframework.http.HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problem.setTitle("Conflict");
        return problem;
    }

    @ExceptionHandler(EventNotPublishedException.class)
    public org.springframework.http.ProblemDetail handleEventNotPublished(EventNotPublishedException ex) {
        var problem = ProblemDetail.forStatusAndDetail(
                org.springframework.http.HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problem.setTitle("Conflict");
        return problem;
    }

    @ExceptionHandler(EventAlreadyStartedException.class)
    public org.springframework.http.ProblemDetail handleEventAlreadyStarted(
            EventAlreadyStartedException ex) {
        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problem.setTitle("Conflict");
        return problem;
    }

    @ExceptionHandler(ReservationCapacityExceededException.class)
    public ProblemDetail handleReservationCapacityExceeded(
            ReservationCapacityExceededException ex) {
        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problem.setTitle("Conflict");
        return problem;
    }

    @ExceptionHandler({
            ObjectOptimisticLockingFailureException.class,
            OptimisticLockConflictException.class
    })
    public ProblemDetail handleOptimisticConflict(Exception ex) {
        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Concurrent modification detected"
        );
        problem.setTitle("Conflict");
        return problem;
    }

    @ExceptionHandler(ReservationNotFoundException.class)
    public ProblemDetail handleReservationNotFound(
            ReservationNotFoundException ex) {
        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problem.setTitle("Not Found");
        return problem;
    }

    @ExceptionHandler(InvalidReservationStateException.class)
    public ProblemDetail handleInvalidReservationState(
            InvalidReservationStateException ex) {
        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problem.setTitle("Conflict");
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error("An unknown exception occurred: ", ex);
        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage());
        problem.setTitle("Internal Server Error");
        return problem;
    }
}
