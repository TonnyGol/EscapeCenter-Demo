package Controllers;

import DataBaseService.BookingService;
import DataBaseService.ClientsService;
import DataBaseService.RoomService;
import Exceptions.GlobalExceptionHandler;
import Repositories.WorkerRepository;
import Requests.BookingDetails;
import Requests.ClientDetails;
import Requests.CreateBookingRequest;
import Requests.UpdateBookingRequest;
import Responses.RoomResponse;
import Responses.SlotResponse;
import Security.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Controller, validation, error mapping and security rules only: the services are mocked, no database needed.
@WebMvcTest(ApiController.class)
@ContextConfiguration(classes = {ApiController.class, GlobalExceptionHandler.class, SecurityConfig.class})
@WithMockUser(roles = ApiControllerWebTest.ADMIN_ROLE_NAME)
class ApiControllerWebTest {
    // Annotations need a literal; WorkerRole.ADMIN.name() is not one. The 401/403 tests would fail if this drifted.
    static final String ADMIN_ROLE_NAME = "ADMIN";
    private static final String ROOMS_URL = "/api/rooms";
    private static final String AVAILABILITY_URL = "/api/rooms/1/availability";
    private static final String BOOKINGS_URL = "/api/bookings";
    private static final String BOOKING_URL = "/api/bookings/5";
    private static final String CLIENTS_URL = "/api/clients";
    private static final String STAFF_CHECK_URL = "/api/staff/check";
    private static final String NOT_ADMIN_ROLE = "USER";
    private static final String DAY = "2099-01-01";
    private static final String RANGE_FROM_PARAM = "from";
    private static final String RANGE_TO_PARAM = "to";
    private static final String DATE_PARAM = "date";
    private static final long ROOM_ID = 1L;
    private static final long BOOKING_ID = 5L;
    private static final LocalDateTime START_AT = LocalDateTime.of(2099, 1, 1, 10, 0);
    private static final String CONFLICT_MESSAGE = "This slot is already booked.";
    private static final String BOOKING_NOT_FOUND_MESSAGE = "Booking not found.";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookingService bookingService;
    @MockitoBean
    private RoomService roomService;
    @MockitoBean
    private ClientsService clientsService;
    // SecurityConfig builds its UserDetailsService from this repository.
    @MockitoBean
    private WorkerRepository workerRepository;

    private static ClientDetails clientDetails(String firstName) {
        return new ClientDetails(firstName, "Levi", "0501234567", "dana@example.com");
    }

    private static BookingDetails bookingDetails() {
        return new BookingDetails(4, "beginner", "", "red");
    }

    private MockHttpServletRequestBuilder withJson(MockHttpServletRequestBuilder request, Object body) throws Exception {
        return request.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
    }

    private MockHttpServletRequestBuilder createRequest(String firstName) throws Exception {
        return withJson(post(BOOKINGS_URL),
                new CreateBookingRequest(ROOM_ID, START_AT, clientDetails(firstName), bookingDetails()));
    }

    private MockHttpServletRequestBuilder updateRequest() throws Exception {
        return withJson(put(BOOKING_URL), new UpdateBookingRequest(clientDetails("Dana"), bookingDetails()));
    }

    // ---------- public endpoints ----------

    @Test
    @WithAnonymousUser
    void getRooms_isPublicAndReturnsRooms() throws Exception {
        when(roomService.getRooms()).thenReturn(List.of(new RoomResponse(ROOM_ID, "Vault", 6, 60)));

        mockMvc.perform(get(ROOMS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Vault"));
    }

    @Test
    @WithAnonymousUser
    void getAvailability_returnsSlots() throws Exception {
        when(roomService.getAvailability(ROOM_ID, LocalDate.parse(DAY)))
                .thenReturn(List.of(new SlotResponse(START_AT, START_AT.plusHours(1), true)));

        mockMvc.perform(get(AVAILABILITY_URL).param(DATE_PARAM, DAY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].available").value(true));
    }

    @Test
    @WithAnonymousUser
    void getAvailability_unknownRoomIs404() throws Exception {
        when(roomService.getAvailability(eq(ROOM_ID), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found."));

        mockMvc.perform(get(AVAILABILITY_URL).param(DATE_PARAM, DAY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Room not found."));
    }

    @Test
    @WithAnonymousUser
    void getAvailability_withoutDateIs400() throws Exception {
        mockMvc.perform(get(AVAILABILITY_URL)).andExpect(status().isBadRequest());
    }

    @Test
    @WithAnonymousUser
    void createBooking_customerPathIsNotStaff() throws Exception {
        mockMvc.perform(createRequest("Dana")).andExpect(status().isCreated());

        verify(bookingService).createBooking(any(CreateBookingRequest.class), eq(false));
    }

    @Test
    void createBooking_adminIsBookedByStaff() throws Exception {
        mockMvc.perform(createRequest("Dana")).andExpect(status().isCreated());

        verify(bookingService).createBooking(any(CreateBookingRequest.class), eq(true));
    }

    @Test
    void createBooking_blankFirstNameIs400WithFieldName() throws Exception {
        mockMvc.perform(createRequest(""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("client.firstName")));
    }

    @Test
    void createBooking_missingBodyFieldsIs400() throws Exception {
        mockMvc.perform(post(BOOKINGS_URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("roomId")));
    }

    @Test
    void createBooking_takenSlotIs409() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, CONFLICT_MESSAGE))
                .when(bookingService).createBooking(any(), eq(true));

        mockMvc.perform(createRequest("Dana"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(CONFLICT_MESSAGE));
    }

    // ---------- staff endpoints: 401 without credentials, 403 with the wrong role ----------

    @Test
    void staffCheck_adminIsOk() throws Exception {
        mockMvc.perform(get(STAFF_CHECK_URL)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = NOT_ADMIN_ROLE)
    void staffEndpoints_wrongRoleIs403() throws Exception {
        mockMvc.perform(get(STAFF_CHECK_URL)).andExpect(status().isForbidden());
        mockMvc.perform(get(CLIENTS_URL)).andExpect(status().isForbidden());
        mockMvc.perform(delete(BOOKING_URL)).andExpect(status().isForbidden());
        mockMvc.perform(updateRequest()).andExpect(status().isForbidden());
        mockMvc.perform(get(BOOKINGS_URL).param(RANGE_FROM_PARAM, DAY).param(RANGE_TO_PARAM, DAY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    void staffEndpoints_noCredentialsIs401() throws Exception {
        mockMvc.perform(get(STAFF_CHECK_URL)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(CLIENTS_URL)).andExpect(status().isUnauthorized());
        mockMvc.perform(delete(BOOKING_URL)).andExpect(status().isUnauthorized());
        mockMvc.perform(updateRequest()).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BOOKINGS_URL).param(RANGE_FROM_PARAM, DAY).param(RANGE_TO_PARAM, DAY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getBookings_returnsList() throws Exception {
        when(bookingService.getBookings(LocalDate.parse(DAY), LocalDate.parse(DAY), null))
                .thenReturn(List.of());

        mockMvc.perform(get(BOOKINGS_URL).param(RANGE_FROM_PARAM, DAY).param(RANGE_TO_PARAM, DAY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getBookings_badRangeIs400() throws Exception {
        when(bookingService.getBookings(any(), any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "bad range"));

        mockMvc.perform(get(BOOKINGS_URL).param(RANGE_FROM_PARAM, DAY).param(RANGE_TO_PARAM, DAY))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getBookings_missingRangeIs400() throws Exception {
        mockMvc.perform(get(BOOKINGS_URL)).andExpect(status().isBadRequest());
    }

    @Test
    void updateBooking_isNoContent() throws Exception {
        mockMvc.perform(updateRequest()).andExpect(status().isNoContent());

        verify(bookingService).updateBooking(eq(BOOKING_ID), any(UpdateBookingRequest.class));
    }

    @Test
    void updateBooking_unknownIdIs404() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, BOOKING_NOT_FOUND_MESSAGE))
                .when(bookingService).updateBooking(eq(BOOKING_ID), any());

        mockMvc.perform(updateRequest())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(BOOKING_NOT_FOUND_MESSAGE));
    }

    @Test
    void updateBooking_blankFirstNameIs400() throws Exception {
        mockMvc.perform(withJson(put(BOOKING_URL), new UpdateBookingRequest(clientDetails(""), bookingDetails())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteBooking_isNoContent() throws Exception {
        mockMvc.perform(delete(BOOKING_URL)).andExpect(status().isNoContent());

        verify(bookingService).deleteBooking(BOOKING_ID);
    }

    @Test
    void deleteBooking_unknownIdIs404() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, BOOKING_NOT_FOUND_MESSAGE))
                .when(bookingService).deleteBooking(BOOKING_ID);

        mockMvc.perform(delete(BOOKING_URL)).andExpect(status().isNotFound());
    }

    @Test
    void searchClients_returnsList() throws Exception {
        when(clientsService.searchClients("dan")).thenReturn(List.of());

        mockMvc.perform(get(CLIENTS_URL).param("query", "dan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
