package Exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GlobalExceptionHandlerTest {

    private static final int CONFLICT = 409;
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void duplicateSlot_saysTheSlotIsTaken() {
        ProblemDetail slot = handler.handleIntegrityViolation(violation("Duplicate entry '1-x' for key 'bookings.uq_bookings_room_start'"));
        ProblemDetail phone = handler.handleIntegrityViolation(violation("Duplicate entry '050' for key 'clients.uq_clients_phone'"));

        assertEquals(CONFLICT, slot.getStatus());
        assertEquals(CONFLICT, phone.getStatus());
        assertNotEquals(slot.getDetail(), phone.getDetail());
        assertNull(slot.getProperties());
        assertEquals(GlobalExceptionHandler.TRY_AGAIN_CODE,
                phone.getProperties().get(GlobalExceptionHandler.ERROR_CODE_PROPERTY));
    }

    private static DataIntegrityViolationException violation(String databaseMessage) {
        return new DataIntegrityViolationException("could not execute statement", new SQLException(databaseMessage));
    }
}
