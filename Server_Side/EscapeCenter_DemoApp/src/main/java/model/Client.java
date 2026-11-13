package model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Immutable client data record.
 * Represents a customer who has made bookings. {@code id} is null for a client the server has not saved yet.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Client(Long id, String firstName, String lastName, String phoneNumber, String email) {

    public String fullName() {
        return firstName + " " + lastName;
    }
}
