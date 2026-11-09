package Exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

/**
 * Every error leaves the API as an RFC 7807 ProblemDetail. The base class already does this for bad JSON,
 * missing parameters and ResponseStatusException (the 404, 409 and 400 thrown by the services).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String SLOT_TAKEN_MESSAGE = "This slot is already booked.";
    private static final String SLOT_CONSTRAINT_NAME = "uq_bookings_room_start";
    private static final String TRY_AGAIN_MESSAGE = "The booking could not be saved. Please try again.";
    private static final String DATABASE_ERROR_MESSAGE = "Database error. Please try again later.";
    private static final String FIELD_ERROR_SEPARATOR = "; ";

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        String problems = exception.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(FIELD_ERROR_SEPARATOR));
        exception.getBody().setDetail(problems);
        return super.handleMethodArgumentNotValid(exception, headers, status, request);
    }

    // The unique (room, start) constraint is the last line of defence against double booking.
    // Other constraints, such as two requests creating the same new client at once, are not a taken slot.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleIntegrityViolation(DataIntegrityViolationException exception) {
        String cause = exception.getMostSpecificCause().getMessage();
        if (cause != null && cause.contains(SLOT_CONSTRAINT_NAME)) {
            return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, SLOT_TAKEN_MESSAGE);
        }
        LOG.warn("Constraint violation that is not a taken slot", exception);
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, TRY_AGAIN_MESSAGE);
    }

    @ExceptionHandler(DataAccessException.class)
    public ProblemDetail handleDatabaseError(DataAccessException exception) {
        LOG.error("Database error while handling a request", exception);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, DATABASE_ERROR_MESSAGE);
    }
}
