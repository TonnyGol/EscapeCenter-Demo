package com.example.escapecenter.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Immutable client data record.
 * Represents a customer who has made bookings.
 */
public record Client(
        @JsonProperty("firstName") String firstName,
        @JsonProperty("lastName") String lastName,
        @JsonProperty("phoneNumber") String phoneNumber,
        @JsonProperty("email") String email
) {
    @JsonCreator
    public Client {}
}
