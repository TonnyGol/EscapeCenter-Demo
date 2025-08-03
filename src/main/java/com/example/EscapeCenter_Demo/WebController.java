package com.example.EscapeCenter_Demo;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.example.EscapeCenter_Demo.DataBaseService.BookingService;
import com.example.EscapeCenter_Demo.gui.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
public class WebController {
    private final AtomicLong counter = new AtomicLong();

    @PostMapping("/booking")
    public ResponseEntity<String> createBooking(@RequestBody Booking booking) {
        BookingService.addBooking(booking);
        EmailService.sendBookMail(booking,
                booking.getBookingID().split("/")[0],
                booking.getBookingID().split("/")[1]);
        return ResponseEntity.ok("Booking saved!");
    }

    @GetMapping("/booking")
    public ResponseEntity<Map<String, Booking>> getAllBookings() {
        return ResponseEntity.ok(BookingService.getAllBookings());
    }

}
