package service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import model.Booking;
import model.BookingDetails;
import model.Client;
import model.CreateBookingRequest;
import model.Room;
import model.UpdateBookingRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// The server is a real HTTP server on a free local port, so the test covers the real HttpClient and Jackson code.
class ApiServiceTest {
    private static final String CONNECTION_ERROR_MESSAGE = "לא ניתן להתחבר לשרת.";
    private static final String SERVER_URL_PROPERTY = "server.url";
    private static final String REQUEST_TIMEOUT_PROPERTY = "request.timeout.seconds";
    private static final String SLOW_RESPONSE_PROPERTY_VALUE = "1";
    private static final long SLOW_SERVER_DELAY_MILLIS = 3_000;
    private static final String UNREACHABLE_SERVER_URL = "http://localhost:1";
    private static final String DETAIL_REASON = "This slot is already booked.";
    private static final String PROBLEM_JSON = "{\"status\":409,\"detail\":\"" + DETAIL_REASON + "\"}";
    private static final String ROOMS_JSON =
            "[{\"id\":1,\"name\":\"Vault\",\"capacity\":6,\"durationMinutes\":60,\"somethingNew\":true}]";
    private static final String SLOTS_JSON = "[{\"startAt\":\"2099-01-01T10:00:00\",\"endAt\":\"2099-01-01T11:00:00\","
            + "\"available\":true},{\"startAt\":\"2099-01-01T12:00:00\",\"endAt\":\"2099-01-01T13:00:00\",\"available\":false}]";
    private static final String BOOKINGS_JSON = "[{\"id\":5,\"roomName\":\"Vault\",\"startAt\":\"2099-01-01T10:00:00\","
            + "\"endAt\":\"2099-01-01T11:00:00\",\"client\":{\"firstName\":\"Dana\",\"lastName\":\"Levi\","
            + "\"phoneNumber\":\"0501234567\",\"email\":null},\"participants\":4,\"experience\":\"beginner\","
            + "\"notes\":\"\",\"color\":\"red\"}]";
    private static final String CLIENTS_JSON =
            "[{\"firstName\":\"Dana\",\"lastName\":\"Levi\",\"phoneNumber\":\"0501234567\",\"email\":null}]";
    private static final String STAFF_CHECK = "GET /api/staff/check";
    private static final String ROOMS = "GET /api/rooms";
    private static final String BOOKINGS = "GET /api/bookings";
    private static final String CLIENTS = "GET /api/clients";
    private static final String CREATE_BOOKING = "POST /api/bookings";
    private static final String UPDATE_BOOKING = "PUT /api/bookings/5";
    private static final String DELETE_BOOKING = "DELETE /api/bookings/5";
    private static final String ROOM_SLOTS = "GET /api/rooms/1/availability";
    private static final String PHONE = "0501234567";
    private static final String BOOKING_SLOT_KEY = "2099-01-01T10:00/Vault";
    private static final Client DANA = new Client("Dana", "Levi", PHONE, null);
    private static final BookingDetails DETAILS = new BookingDetails(4, "beginner", "", "red");
    private static final CreateBookingRequest NEW_BOOKING = new CreateBookingRequest(1L, "2099-01-01T10:00", DANA, DETAILS);
    private static final LocalDate DAY = LocalDate.of(2099, 1, 1);

    private record Reply(int status, String body) {
    }

    private record ReceivedRequest(String methodAndPath, String query, String authorization, String contentType,
                                   String body) {
    }

    private final ApiService api = ApiService.getInstance();
    private final Map<String, Reply> replies = new HashMap<>();
    private final Set<String> slowAnswers = new HashSet<>();
    private volatile ReceivedRequest lastRequest;
    private HttpServer server;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", this::answer);
        server.start();
        setSessionConfig(SERVER_URL_PROPERTY, "http://localhost:" + server.getAddress().getPort());
        Session.getInstance().login("admin", "secret");
    }

    @AfterEach
    void stopServer() throws Exception {
        server.stop(0);
        setSessionConfig(REQUEST_TIMEOUT_PROPERTY, null);
        Session.getInstance().logout();
    }

    private void answer(HttpExchange exchange) throws IOException {
        String methodAndPath = exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath();
        lastRequest = new ReceivedRequest(methodAndPath, exchange.getRequestURI().getQuery(),
                exchange.getRequestHeaders().getFirst("Authorization"),
                exchange.getRequestHeaders().getFirst("Content-Type"),
                new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        if (slowAnswers.contains(methodAndPath)) {
            sleepLikeAStalledServer();
        }
        Reply reply = replies.getOrDefault(methodAndPath, new Reply(404, ""));
        byte[] body = reply.body().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(reply.status(), body.length == 0 ? -1 : body.length);
        if (body.length > 0) {
            exchange.getResponseBody().write(body);
        }
        exchange.close();
    }

    private static void sleepLikeAStalledServer() {
        try {
            Thread.sleep(SLOW_SERVER_DELAY_MILLIS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    // Session reads its URL from config.properties, which a test must not change; the field is set directly instead.
    private static void setSessionConfig(String key, String value) throws Exception {
        Field field = Session.class.getDeclaredField("config");
        field.setAccessible(true);
        Properties config = (Properties) field.get(Session.getInstance());
        if (value == null) {
            config.remove(key);
        } else {
            config.setProperty(key, value);
        }
    }

    private void serverAnswers(String methodAndPath, int status, String body) {
        replies.put(methodAndPath, new Reply(status, body));
    }

    // ---------- login ----------

    @Test
    void login_succeedsOn200_andSendsBasicCredentials() {
        serverAnswers(STAFF_CHECK, 200, "Admin access OK!");

        assertTrue(api.login("admin", "secret"));

        assertTrue(Session.getInstance().isAuthenticated());
        assertEquals("Basic YWRtaW46c2VjcmV0", lastRequest.authorization());
    }

    @Test
    void login_failsOn401_andForgetsTheCredentials() {
        serverAnswers(STAFF_CHECK, 401, "");

        assertFalse(api.login("admin", "wrong"));

        assertFalse(Session.getInstance().isAuthenticated());
    }

    @Test
    void login_failsWhenTheServerIsDown() throws Exception {
        setSessionConfig(SERVER_URL_PROPERTY, UNREACHABLE_SERVER_URL);

        assertFalse(api.login("admin", "secret"));
        assertFalse(Session.getInstance().isAuthenticated());
    }

    // ---------- loading ----------

    @Test
    void loadRooms_mapsJson_andIgnoresUnknownFields() {
        serverAnswers(ROOMS, 200, ROOMS_JSON);

        Optional<List<Room>> rooms = api.loadRooms();

        assertEquals(List.of(new Room(1L, "Vault", 6, 60)), rooms.orElseThrow());
    }

    @Test
    void loadRooms_isEmptyOnServerError() {
        serverAnswers(ROOMS, 500, "");

        assertTrue(api.loadRooms().isEmpty());
    }

    @Test
    void loadRooms_isEmptyOnMalformedJson() {
        serverAnswers(ROOMS, 200, "not json");

        assertTrue(api.loadRooms().isEmpty());
    }

    @Test
    void aServerThatStopsAnswering_endsInAFailureInsteadOfWaitingForever() throws Exception {
        setSessionConfig(REQUEST_TIMEOUT_PROPERTY, SLOW_RESPONSE_PROPERTY_VALUE);
        serverAnswers(ROOMS, 200, ROOMS_JSON);
        slowAnswers.add(ROOMS);
        serverAnswers(CREATE_BOOKING, 201, "");
        slowAnswers.add(CREATE_BOOKING);

        long startedAt = System.nanoTime();
        Optional<List<Room>> rooms = api.loadRooms();
        Optional<String> refusal = api.addBooking(NEW_BOOKING);
        long waitedMillis = (System.nanoTime() - startedAt) / 1_000_000;

        assertTrue(rooms.isEmpty());
        assertEquals(CONNECTION_ERROR_MESSAGE, refusal.orElseThrow());
        assertTrue(waitedMillis < 2 * SLOW_SERVER_DELAY_MILLIS, "waited " + waitedMillis + " ms");
    }

    @Test
    void loadRooms_isEmptyWhenTheServerIsDown() throws Exception {
        setSessionConfig(SERVER_URL_PROPERTY, UNREACHABLE_SERVER_URL);

        assertTrue(api.loadRooms().isEmpty());
    }

    @Test
    void loadGameStartTimes_returnsTheTimesOfDayInOrder_forTheAskedDate() {
        serverAnswers(ROOM_SLOTS, 200, SLOTS_JSON);

        List<LocalTime> times = api.loadGameStartTimes(1L, DAY).orElseThrow();

        assertEquals(List.of(LocalTime.of(10, 0), LocalTime.of(12, 0)), times);
        assertEquals("date=2099-01-01", lastRequest.query());
    }

    @Test
    void loadBookings_keysThemBySlot_andSendsTheDateRange() {
        serverAnswers(BOOKINGS, 200, BOOKINGS_JSON);

        Map<String, Booking> bookings = api.loadBookings(DAY, DAY.plusDays(6)).orElseThrow();

        assertEquals(Set.of(BOOKING_SLOT_KEY), bookings.keySet());
        assertEquals("Dana", bookings.get(BOOKING_SLOT_KEY).firstName());
        assertEquals("from=2099-01-01&to=2099-01-07", lastRequest.query());
    }

    @Test
    void loadBookings_emptyListMeansNoBookings_notAFailure() {
        serverAnswers(BOOKINGS, 200, "[]");

        assertEquals(Map.of(), api.loadBookings(DAY, DAY).orElseThrow());
    }

    @Test
    void loadBookings_isEmptyOptionalOnServerError() {
        serverAnswers(BOOKINGS, 403, "");

        assertTrue(api.loadBookings(DAY, DAY).isEmpty());
    }

    @Test
    void loadClients_keysThemByPhone_andIsEmptyOnFailure() {
        serverAnswers(CLIENTS, 200, CLIENTS_JSON);
        assertEquals(DANA, api.loadClients().get(PHONE));

        serverAnswers(CLIENTS, 500, "");
        assertTrue(api.loadClients().isEmpty());
    }

    // ---------- changing bookings ----------

    @Test
    void addBooking_sendsJson_andReturnsEmptyOnSuccess() {
        serverAnswers(CREATE_BOOKING, 201, "");

        Optional<String> refusal = api.addBooking(NEW_BOOKING);

        assertTrue(refusal.isEmpty());
        assertEquals("application/json", lastRequest.contentType());
        assertTrue(lastRequest.body().contains("\"roomId\":1"));
        assertTrue(lastRequest.body().contains("\"phoneNumber\":\"" + PHONE + "\""));
    }

    @Test
    void addBooking_returnsTheServersDetail_onProblemJson() {
        serverAnswers(CREATE_BOOKING, 409, PROBLEM_JSON);

        assertEquals(DETAIL_REASON, api.addBooking(NEW_BOOKING).orElseThrow());
    }

    @Test
    void addBooking_fallsBackToTheStatusCode_whenTheBodyIsNotAProblem() {
        serverAnswers(CREATE_BOOKING, 502, "<html>bad gateway</html>");

        assertEquals("HTTP 502", api.addBooking(NEW_BOOKING).orElseThrow());
    }

    @Test
    void addBooking_reportsAConnectionError_whenTheServerIsDown() throws Exception {
        setSessionConfig(SERVER_URL_PROPERTY, UNREACHABLE_SERVER_URL);

        assertEquals(CONNECTION_ERROR_MESSAGE, api.addBooking(NEW_BOOKING).orElseThrow());
    }

    @Test
    void updateBooking_usesPutOnTheBookingPath() {
        serverAnswers(UPDATE_BOOKING, 204, "");

        Optional<String> refusal = api.updateBooking(5L, new UpdateBookingRequest(DANA, DETAILS));

        assertTrue(refusal.isEmpty());
        assertEquals(UPDATE_BOOKING, lastRequest.methodAndPath());
    }

    @Test
    void deleteBooking_returnsEmptyOnSuccess_andTheDetailOnNotFound() {
        serverAnswers(DELETE_BOOKING, 204, "");
        assertTrue(api.deleteBooking(5L).isEmpty());

        serverAnswers(DELETE_BOOKING, 404, "{\"detail\":\"Booking not found.\"}");
        assertEquals("Booking not found.", api.deleteBooking(5L).orElseThrow());
    }
}
