package model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Immutable client data record.
 * Represents a customer who has made bookings.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Client(String firstName, String lastName, String phoneNumber, String email) {
}
