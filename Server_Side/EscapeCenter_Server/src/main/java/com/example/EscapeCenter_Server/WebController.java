package com.example.EscapeCenter_Server;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.example.EscapeCenter_Server.DataBaseService.BookingService;
import com.example.EscapeCenter_Server.DataBaseService.ClientsService;
import com.example.EscapeCenter_Server.Requests.DeleteRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "")
@RestController
public class WebController {
    private final AtomicLong counter = new AtomicLong();

    @GetMapping("/server/test")
    public String testAdmin() {
        return "Admin access OK!";
    }

    @PostMapping("/server/updateBooking")
    public ResponseEntity<String> updateBooking(@RequestBody Booking booking) {
        BookingService.addBooking(booking);
        return ResponseEntity.ok("Booking updated.");
    }

    @PostMapping("/server/deleteBooking")
    public ResponseEntity<String> deleteBooking(@RequestBody DeleteRequest credentials) {

        String bookingID = credentials.getBookingID();
        String roomName = credentials.getRoomName();
        BookingService.deleteBooking(bookingID, roomName);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body("Credentials OK. Login successful.");
    }

    @GetMapping("/server/clients")
    public ResponseEntity<Map<String, Client>> getAllClients() {
        return ResponseEntity.ok(ClientsService.getAllClients());
    }


    @PostMapping("/addBooking")
    public ResponseEntity<String> createBooking(@RequestBody Booking booking) {
        BookingService.addBooking(booking);
        //EmailService.sendBookMail(booking, booking.getBookingID().split("/")[0], booking.getBookingID().split("/")[1]);
        return ResponseEntity.ok("Booking saved successfully!");
    }

    @GetMapping("/bookings")
    public ResponseEntity<Map<String, Booking>> getAllBookings() {
        return ResponseEntity.ok(BookingService.getAllBookings());
    }



}
