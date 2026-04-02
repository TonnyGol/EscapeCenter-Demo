package com.example.escapecenter.service;

import com.example.escapecenter.model.Booking;
import com.example.escapecenter.model.Client;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.Map;

/**
 * Centralized HTTP API service.
 * All REST communication with the server goes through this class.
 */
public final class ApiService {

    private static final ApiService INSTANCE = new ApiService();

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

    // ---- Authentication ----

    /**
     * Attempt login against the server. Returns true on success (HTTP 200).
     */
    public boolean login(String username, String password) {
        try {
            Session.getInstance().login(username, password);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl() + "/server/test"))
                    .header("Authorization", auth())
                    .GET()
                    .build();

            HttpResponse<String> response =
                    http().send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
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

    // ---- Bookings ----

    /**
     * Load all bookings from the server.
     * @return map of booking-key → Booking, or empty map on failure
     */
    public Map<String, Booking> loadBookings() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl() + "/bookings"))
                    .GET()
                    .build();

            HttpResponse<String> response =
                    http().send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Booking> result = mapper().readValue(
                        response.body(), new TypeReference<>() {});
                System.out.println("[ApiService] Loaded " + result.size() + " bookings.");
                return result;
            } else {
                System.err.println("[ApiService] Load bookings failed: HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("[ApiService] Load bookings error: " + e.getMessage());
        }
        return Collections.emptyMap();
    }

    /**
     * Add a new booking.
     * @return true if server accepted it
     */
    public boolean addBooking(Booking booking) {
        try {
            String json = mapper().writeValueAsString(booking);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl() + "/addBooking"))
                    .header("Authorization", auth())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response =
                    http().send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("[ApiService] Booking saved successfully!");
                return true;
            } else {
                System.err.println("[ApiService] Add booking failed: HTTP "
                        + response.statusCode() + " — " + response.body());
            }
        } catch (Exception e) {
            System.err.println("[ApiService] Add booking error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Update an existing booking.
     * @return true if server accepted it
     */
    public boolean updateBooking(Booking booking) {
        try {
            String json = mapper().writeValueAsString(booking);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl() + "/server/updateBooking"))
                    .header("Authorization", auth())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response =
                    http().send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("[ApiService] Booking updated successfully!");
                return true;
            } else {
                System.err.println("[ApiService] Update booking failed: HTTP "
                        + response.statusCode() + " — " + response.body());
            }
        } catch (Exception e) {
            System.err.println("[ApiService] Update booking error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Delete a booking by ID and room name.
     * @return true if server accepted it
     */
    public boolean deleteBooking(String bookingID, String roomName) {
        try {
            String json = String.format("{\"bookingID\":\"%s\",\"roomName\":\"%s\"}",
                    bookingID, roomName);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl() + "/server/deleteBooking"))
                    .header("Authorization", auth())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response =
                    http().send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("[ApiService] Booking deleted successfully!");
                return true;
            } else {
                System.err.println("[ApiService] Delete booking failed: HTTP "
                        + response.statusCode() + " — " + response.body());
            }
        } catch (Exception e) {
            System.err.println("[ApiService] Delete booking error: " + e.getMessage());
        }
        return false;
    }

    // ---- Clients ----

    /**
     * Load all clients from the server (requires auth).
     * @return map of client-key → Client, or empty map on failure
     */
    public Map<String, Client> loadClients() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl() + "/server/clients"))
                    .header("Authorization", auth())
                    .GET()
                    .build();

            HttpResponse<String> response =
                    http().send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Client> result = mapper().readValue(
                        response.body(), new TypeReference<>() {});
                System.out.println("[ApiService] Loaded " + result.size() + " clients.");
                return result;
            } else {
                System.err.println("[ApiService] Load clients failed: HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("[ApiService] Load clients error: " + e.getMessage());
        }
        return Collections.emptyMap();
    }
}
