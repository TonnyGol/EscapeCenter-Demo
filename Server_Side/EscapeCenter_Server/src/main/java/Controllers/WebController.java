package Controllers;

import Entities.Booking;
import Entities.Client;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Map;

import DataBaseService.BookingService;
import DataBaseService.ClientsService;
import Requests.DeleteRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "")
@RestController
public class WebController {
    private static final Logger LOG = LoggerFactory.getLogger(WebController.class);
    private static final String BOOKING_ALREADY_EXISTS_MESSAGE = "A booking with this ID already exists.";
    private static final String DATABASE_ERROR_MESSAGE = "Database error. Please try again later.";

    @GetMapping("/server/test")
    public String testAdmin() {
        return "Admin access OK!";
    }

    @PutMapping("/server/updateBooking")
    public ResponseEntity<String> updateBooking(@RequestBody Booking booking) throws SQLException {
        if (BookingService.updateBooking(booking) == 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.status(HttpStatus.OK).body("Booking updated.");
    }

    @PostMapping("/server/deleteBooking")
    public ResponseEntity<String> deleteBooking(@RequestBody DeleteRequest credentials) throws SQLException {
        String bookingID = credentials.getBookingID();
        String roomName = credentials.getRoomName();
        if (BookingService.deleteBooking(bookingID, roomName) == 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/server/clients")
    public ResponseEntity<Map<String, Client>> getAllClients() {
        return ResponseEntity.ok(ClientsService.getAllClients());
    }


    @PostMapping("/addBooking")
    public ResponseEntity<String> createBooking(@RequestBody Booking booking) throws SQLException {
        BookingService.addBooking(booking);
        //EmailService.sendBookMail(booking, booking.getBookingID().split("/")[0], booking.getBookingID().split("/")[1]);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("Booking saved successfully!");
    }

    @GetMapping("/bookings")
    public ResponseEntity<Map<String, Booking>> getAllBookings() {
        return ResponseEntity.ok(BookingService.getAllBookings());
    }

    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public ResponseEntity<String> handleDuplicateBooking() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(BOOKING_ALREADY_EXISTS_MESSAGE);
    }

    @ExceptionHandler(SQLException.class)
    public ResponseEntity<String> handleDatabaseError(SQLException exception) {
        LOG.error("Database error while handling a request", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(DATABASE_ERROR_MESSAGE);
    }
}
