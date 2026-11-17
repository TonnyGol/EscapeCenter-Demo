package model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

/**
 * Immutable booking as the server returns it to staff.
 * {@code startAt} and {@code endAt} stay ISO strings; the calendar parses them where it needs a date.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Booking(
        Long id,
        String roomName,
        String startAt,
        String endAt,
        Client client,
        int participants,
        String experience,
        String notes,
        String color,
        BookingType type,
        String internalNotes,
        BookingStatus status,
        String createdAt
) {
    /** Key the calendar uses to find the booking of a slot: start time, then room. */
    public String slotKey() {
        return LocalDateTime.parse(startAt) + "/" + roomName;
    }

    public BookingDetails details() {
        return new BookingDetails(participants, experience, notes, color, type, internalNotes);
    }

    public Booking withColor(String newColor) {
        return new Booking(id, roomName, startAt, endAt, client, participants, experience, notes, newColor,
                type, internalNotes, status, createdAt);
    }

    public LocalDateTime start() {
        return LocalDateTime.parse(startAt);
    }

    public LocalDateTime end() {
        return LocalDateTime.parse(endAt);
    }

    /** True when the two games share some time. A game that starts exactly when the other ends does not overlap. */
    public boolean overlaps(Booking other) {
        return start().isBefore(other.end()) && other.start().isBefore(end());
    }

    public String room() {
        return roomName;
    }

    public String firstName() {
        return client.firstName();
    }

    public String lastName() {
        return client.lastName();
    }

    public String phoneNumber() {
        return client.phoneNumber();
    }

    public String email() {
        return client.email();
    }
}
