package Controllers;

import App.EscapeCenterServer;
import DataBaseService.DefaultService;
import Entities.Booking;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// shortcut: runs against the Docker MySQL (`docker compose up -d`) with DB_* and APP_ADMIN_* set; roadmap Phase 5 moves it to Testcontainers.
@SpringBootTest(classes = EscapeCenterServer.class)
@AutoConfigureMockMvc
class WebControllerStatusCodesTest {

    private static final String BOOKING_ID = "TEST-01.01/10:00-11:00";
    private static final String UNKNOWN_BOOKING_ID = "TEST-99.99/00:00-01:00";
    private static final String ROOM = "TestRoom";
    private static final String TEST_CLIENT_FIRST_NAME = "TestFirst";
    private static final String ORIGINAL_PHONE = "050-0000000";
    private static final String CHANGED_PHONE = "051-1111111";
    private static final String ALL_BOOKINGS_URL = "/bookings";
    private static final String ADD_BOOKING_URL = "/addBooking";
    private static final String UPDATE_BOOKING_URL = "/server/updateBooking";
    private static final String DELETE_BOOKING_URL = "/server/deleteBooking";
    private static final String STAFF_BOOKINGS_URL = "/server/bookings";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void removeTestRows() throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                DefaultService.JDBC_URL, DefaultService.DB_USER, DefaultService.DB_PASS)) {
            execute(connection, "DELETE FROM bookings WHERE BookingID = ?", BOOKING_ID);
            execute(connection, "DELETE FROM clients WHERE firstName = ?", TEST_CLIENT_FIRST_NAME);
        }
    }

    @Test
    void createBooking_returns201_thenSameBookingAgainReturns409() throws Exception {
        mockMvc.perform(jsonPost(ADD_BOOKING_URL, bookingJson(BOOKING_ID))).andExpect(status().isCreated());
        mockMvc.perform(jsonPost(ADD_BOOKING_URL, bookingJson(BOOKING_ID))).andExpect(status().isConflict());
    }

    @Test
    void createBooking_withBrokenJson_returns400() throws Exception {
        mockMvc.perform(jsonPost(ADD_BOOKING_URL, "{oops")).andExpect(status().isBadRequest());
    }

    @Test
    void createBooking_withBlankFirstName_returns400NamingTheField() throws Exception {
        mockMvc.perform(jsonPost(ADD_BOOKING_URL, bookingJsonWith("", 2)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("firstName")));
    }

    @Test
    void createBooking_withTooManyParticipants_returns400NamingTheField() throws Exception {
        mockMvc.perform(jsonPost(ADD_BOOKING_URL, bookingJsonWith(TEST_CLIENT_FIRST_NAME, Booking.MAX_PARTICIPANTS + 1)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("participants")));
    }

    @Test
    void updateBooking_returns200ForExistingAnd404ForUnknown() throws Exception {
        mockMvc.perform(jsonPost(ADD_BOOKING_URL, bookingJson(BOOKING_ID))).andExpect(status().isCreated());

        mockMvc.perform(asAdmin(put(UPDATE_BOOKING_URL)).contentType(MediaType.APPLICATION_JSON)
                .content(bookingJson(BOOKING_ID))).andExpect(status().isOk());
        mockMvc.perform(asAdmin(put(UPDATE_BOOKING_URL)).contentType(MediaType.APPLICATION_JSON)
                .content(bookingJson(UNKNOWN_BOOKING_ID))).andExpect(status().isNotFound());
    }

    @Test
    void updateBooking_savesTheChangedPhoneNumber() throws Exception {
        mockMvc.perform(jsonPost(ADD_BOOKING_URL, bookingJson(BOOKING_ID))).andExpect(status().isCreated());

        mockMvc.perform(asAdmin(put(UPDATE_BOOKING_URL)).contentType(MediaType.APPLICATION_JSON)
                .content(bookingJson(BOOKING_ID, CHANGED_PHONE))).andExpect(status().isOk());

        mockMvc.perform(asAdmin(get(STAFF_BOOKINGS_URL)))
                .andExpect(jsonPath("$['" + BOOKING_ID + "/" + ROOM + "'].phoneNumber").value(CHANGED_PHONE));
    }

    @Test
    void deleteBooking_returns204ForExisting_thenSameBookingAgainReturns404() throws Exception {
        mockMvc.perform(jsonPost(ADD_BOOKING_URL, bookingJson(BOOKING_ID))).andExpect(status().isCreated());

        mockMvc.perform(asAdmin(post(DELETE_BOOKING_URL)).contentType(MediaType.APPLICATION_JSON)
                .content(deleteJson(BOOKING_ID))).andExpect(status().isNoContent());
        mockMvc.perform(asAdmin(post(DELETE_BOOKING_URL)).contentType(MediaType.APPLICATION_JSON)
                .content(deleteJson(BOOKING_ID))).andExpect(status().isNotFound());
    }

    @Test
    void staffEndpoints_withoutLogin_return401() throws Exception {
        mockMvc.perform(jsonPost(DELETE_BOOKING_URL, deleteJson(BOOKING_ID))).andExpect(status().isUnauthorized());
        mockMvc.perform(put(UPDATE_BOOKING_URL).contentType(MediaType.APPLICATION_JSON)
                .content(bookingJson(BOOKING_ID))).andExpect(status().isUnauthorized());
        mockMvc.perform(get(STAFF_BOOKINGS_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void publicBookings_listsTakenSlotsWithoutPersonalData() throws Exception {
        mockMvc.perform(jsonPost(ADD_BOOKING_URL, bookingJson(BOOKING_ID))).andExpect(status().isCreated());

        mockMvc.perform(get(ALL_BOOKINGS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasItem(BOOKING_ID + "/" + ROOM)))
                .andExpect(content().string(not(containsString(TEST_CLIENT_FIRST_NAME))));
    }

    private static MockHttpServletRequestBuilder jsonPost(String url, String body) {
        return post(url).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(System.getenv("APP_ADMIN_USER"), System.getenv("APP_ADMIN_PASSWORD"));
        return request.headers(headers);
    }

    private String bookingJson(String bookingId) throws Exception {
        return bookingJson(bookingId, ORIGINAL_PHONE);
    }

    private String bookingJson(String bookingId, String phoneNumber) throws Exception {
        return objectMapper.writeValueAsString(new Booking(bookingId, ROOM, TEST_CLIENT_FIRST_NAME,
                "TestLast", phoneNumber, "test@example.com", "beginner", "note", 2, "#ffffff"));
    }

    private String bookingJsonWith(String firstName, int participants) throws Exception {
        return objectMapper.writeValueAsString(new Booking(BOOKING_ID, ROOM, firstName,
                "TestLast", ORIGINAL_PHONE, "test@example.com", "beginner", "note", participants, "#ffffff"));
    }

    private static String deleteJson(String bookingId) {
        return "{\"bookingID\":\"" + bookingId + "\",\"roomName\":\"" + ROOM + "\"}";
    }

    private static void execute(Connection connection, String sql, String value) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            statement.executeUpdate();
        }
    }
}
