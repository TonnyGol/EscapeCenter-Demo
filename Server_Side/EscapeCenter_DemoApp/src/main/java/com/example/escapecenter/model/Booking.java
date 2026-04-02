package com.example.escapecenter.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Immutable booking data record.
 * Represents a single escape room booking with client details and metadata.
 */
public record Booking(
        @JsonProperty("bookingID") String bookingID,
        @JsonProperty("room") String room,
        @JsonProperty("firstName") String firstName,
        @JsonProperty("lastName") String lastName,
        @JsonProperty("phoneNumber") String phoneNumber,
        @JsonProperty("email") String email,
        @JsonProperty("experience") String experience,
        @JsonProperty("notes") String notes,
        @JsonProperty("participants") int participants,
        @JsonProperty("color") String color
) {
    @JsonCreator
    public Booking {}
}
