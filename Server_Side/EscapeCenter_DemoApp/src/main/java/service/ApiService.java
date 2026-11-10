package service;

import model.Booking;
import model.Client;
import model.CreateBookingRequest;
import model.Room;
import model.Slot;
import model.UpdateBookingRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Centralized HTTP API service.
 * All REST communication with the server goes through this class.
 */
public final class ApiService {

    private static final ApiService INSTANCE = new ApiService();

    private static final String STAFF_CHECK_PATH = "/api/staff/check";
    private static final String ROOMS_PATH = "/api/rooms";
    private static final String BOOKINGS_PATH = "/api/bookings";
    private static final String CLIENTS_PATH = "/api/clients";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final String PROBLEM_DETAIL_FIELD = "detail";
    private static final String CONNECTION_ERROR_MESSAGE = "לא ניתן להתחבר לשרת.";

    private ApiService() {}

    public static ApiService getInstance() {
        return INSTANCE;
    }

    // ---- Convenience accessors ----

    private HttpClient http() {
        return Session.getInstance().getHttpClient();
    }

    private ObjectMapper mapper() {
        return Session.getInstance().getObjectMapper();
    }

    private String baseUrl() {
        return Session.getInstance().getServerUrl();
    }

    private String auth() {
        return Session.getInstance().getAuthHeader();
    }

    private static boolean isSuccessful(HttpResponse<?> response) {
        int status = response.statusCode();
        return status >= HttpURLConnection.HTTP_OK && status < HttpURLConnection.HTTP_MULT_CHOICE;
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + path))
                .timeout(Session.getInstance().getRequestTimeout())
                .header(AUTHORIZATION_HEADER, auth());
    }

    // ---- Authentication ----

    /**
     * Attempt login against the server. Returns true on success (HTTP 200).
     */
    public boolean login(String username, String password) {
        try {
            Session.getInstance().login(username, password);

            HttpResponse<String> response =
                    http().send(request(STAFF_CHECK_PATH).GET().build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                return true;
            } else {
                Session.getInstance().logout();
                return false;
            }
        } catch (Exception e) {
            System.err.println("[ApiService] Login failed: " + e.getMessage());
            Session.getInstance().logout();
            return false;
        }
    }

    // ---- Rooms ----

    /**
     * Load the rooms in the order the server returns them.
     * @return the rooms, or empty if the server did not answer
     */
    public Optional<List<Room>> loadRooms() {
        return load(ROOMS_PATH, new TypeReference<List<Room>>() {});
    }

    /**
     * Load the times of day a room starts its games. The hours are the same every day, so any date works.
     * @return the start times in order, or empty if the server did not answer
     */
    public Optional<List<LocalTime>> loadGameStartTimes(Long roomId, LocalDate date) {
        return load(ROOMS_PATH + "/" + roomId + "/availability?date=" + date, new TypeReference<List<Slot>>() {})
                .map(slots -> slots.stream()
                        .map(slot -> LocalDateTime.parse(slot.startAt()).toLocalTime())
                        .toList());
    }

    // ---- Bookings ----

    /**
     * Load the bookings that start between two dates, both included.
     * @return map of {@link Booking#slotKey()} → Booking, or empty if the server did not answer
     * (an empty map means the week really has no bookings)
     */
    public Optional<Map<String, Booking>> loadBookings(LocalDate from, LocalDate to) {
        return load(BOOKINGS_PATH + "?from=" + from + "&to=" + to, new TypeReference<List<Booking>>() {})
                .map(found -> {
                    Map<String, Booking> bySlot = new HashMap<>();
                    for (Booking booking : found) {
                        bySlot.put(booking.slotKey(), booking);
                    }
                    return bySlot;
                });
    }

    /**
     * Add a new booking.
     * @return empty if the server accepted it, otherwise the reason it refused
     */
    public Optional<String> addBooking(CreateBookingRequest booking) {
        return sendJson(request(BOOKINGS_PATH), "POST", booking);
    }

    /**
     * Update the client and details of an existing booking.
     * @return empty if the server accepted it, otherwise the reason it refused
     */
    public Optional<String> updateBooking(Long bookingId, UpdateBookingRequest booking) {
        return sendJson(request(BOOKINGS_PATH + "/" + bookingId), "PUT", booking);
    }

    /**
     * Delete a booking.
     * @return empty if the server accepted it, otherwise the reason it refused
     */
    public Optional<String> deleteBooking(Long bookingId) {
        return send(request(BOOKINGS_PATH + "/" + bookingId).DELETE().build());
    }

    // ---- Clients ----

    /**
     * Load all clients from the server (requires auth).
     * @return map of phone number → Client, or empty map on failure
     */
    public Map<String, Client> loadClients() {
        List<Client> found = load(CLIENTS_PATH, new TypeReference<List<Client>>() {}).orElse(List.of());
        Map<String, Client> byPhone = new LinkedHashMap<>();
        for (Client client : found) {
            byPhone.put(client.phoneNumber(), client);
        }
        return byPhone;
    }

    // ---- Shared request helpers ----

    private <T> Optional<T> load(String path, TypeReference<T> type) {
        try {
            HttpResponse<String> response =
                    http().send(request(path).GET().build(), HttpResponse.BodyHandlers.ofString());

            if (isSuccessful(response)) {
                return Optional.of(mapper().readValue(response.body(), type));
            }
            System.err.println("[ApiService] Load " + path + " failed: HTTP " + response.statusCode());
        } catch (Exception e) {
            System.err.println("[ApiService] Load " + path + " error: " + e.getMessage());
        }
        return Optional.empty();
    }

    private Optional<String> sendJson(HttpRequest.Builder builder, String method, Object body) {
        try {
            HttpRequest request = builder
                    .header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                    .method(method, HttpRequest.BodyPublishers.ofString(mapper().writeValueAsString(body)))
                    .build();
            return send(request);
        } catch (Exception e) {
            System.err.println("[ApiService] " + method + " error: " + e.getMessage());
            return Optional.of(CONNECTION_ERROR_MESSAGE);
        }
    }

    private Optional<String> send(HttpRequest request) {
        try {
            HttpResponse<String> response = http().send(request, HttpResponse.BodyHandlers.ofString());
            if (isSuccessful(response)) {
                return Optional.empty();
            }
            System.err.println("[ApiService] " + request.method() + " " + request.uri().getPath()
                    + " failed: HTTP " + response.statusCode() + " — " + response.body());
            return Optional.of(errorDetail(response));
        } catch (Exception e) {
            System.err.println("[ApiService] " + request.method() + " error: " + e.getMessage());
            return Optional.of(CONNECTION_ERROR_MESSAGE);
        }
    }

    /** The server explains errors in the "detail" field of an RFC 7807 problem; fall back to the status code. */
    private String errorDetail(HttpResponse<String> response) {
        try {
            String detail = mapper().readTree(response.body()).path(PROBLEM_DETAIL_FIELD).asText();
            if (!detail.isBlank()) {
                return detail;
            }
        } catch (Exception ignored) {
            // The body was not JSON; the status code below is all we have.
        }
        return "HTTP " + response.statusCode();
    }
}
