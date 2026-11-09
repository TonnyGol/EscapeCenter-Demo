package Controllers;

import DataBaseService.BookingService;
import DataBaseService.ClientsService;
import DataBaseService.RoomService;
import Requests.CreateBookingRequest;
import Requests.UpdateBookingRequest;
import Responses.ClientResponse;
import Responses.RoomResponse;
import Responses.SlotResponse;
import Responses.StaffBookingResponse;
import Security.SecurityConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ApiController {
    private static final String STAFF_LOGIN_OK_MESSAGE = "Admin access OK!";

    private final BookingService bookingService;
    private final RoomService roomService;
    private final ClientsService clientsService;

    public ApiController(BookingService bookingService, RoomService roomService, ClientsService clientsService) {
        this.bookingService = bookingService;
        this.roomService = roomService;
        this.clientsService = clientsService;
    }

    /** The staff app calls this to check a username and password. */
    @GetMapping("/staff/check")
    public String checkStaffLogin() {
        return STAFF_LOGIN_OK_MESSAGE;
    }

    @GetMapping("/rooms")
    public List<RoomResponse> getRooms() {
        return roomService.getRooms();
    }

    @GetMapping("/rooms/{roomId}/availability")
    public List<SlotResponse> getAvailability(@PathVariable Long roomId,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return roomService.getAvailability(roomId, date);
    }

    @PostMapping("/bookings")
    public ResponseEntity<Void> createBooking(@Valid @RequestBody CreateBookingRequest request,
                                              HttpServletRequest httpRequest) {
        bookingService.createBooking(request, httpRequest.isUserInRole(SecurityConfig.ADMIN_ROLE));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/bookings")
    public List<StaffBookingResponse> getBookings(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long roomId) {
        return bookingService.getBookings(from, to, roomId);
    }

    @PutMapping("/bookings/{id}")
    public ResponseEntity<Void> updateBooking(@PathVariable Long id, @Valid @RequestBody UpdateBookingRequest request) {
        bookingService.updateBooking(id, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/bookings/{id}")
    public ResponseEntity<Void> deleteBooking(@PathVariable Long id) {
        bookingService.deleteBooking(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/clients")
    public List<ClientResponse> searchClients(@RequestParam(required = false) String query) {
        return clientsService.searchClients(query);
    }
}
