package Entities;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class Booking {
    // Limits match the column sizes in db/init/01-schema.sql.
    public static final int MAX_ID_LENGTH = 50;
    // dd.MM.yyyy/HH:mm-HH:mm. The year keeps a slot from clashing with the same slot a year later.
    public static final String BOOKING_ID_FORMAT = "\\d{2}\\.\\d{2}\\.\\d{4}/\\d{2}:\\d{2}-\\d{2}:\\d{2}";
    public static final int MAX_NAME_LENGTH = 50;
    public static final int MAX_PHONE_LENGTH = 20;
    public static final int MAX_EMAIL_LENGTH = 100;
    public static final int MAX_EXPERIENCE_LENGTH = 50;
    public static final int MAX_COLOR_LENGTH = 20;
    public static final int MAX_NOTES_LENGTH = 1000;
    public static final int MIN_PARTICIPANTS = 1;
    public static final int MAX_PARTICIPANTS = 12;

    @NotBlank @Size(max = MAX_ID_LENGTH) @Pattern(regexp = BOOKING_ID_FORMAT)
    private final String bookingID;
    @NotBlank @Size(max = MAX_ID_LENGTH)
    private final String room;
    @NotBlank @Size(max = MAX_NAME_LENGTH)
    private final String firstName;
    @NotBlank @Size(max = MAX_NAME_LENGTH)
    private final String lastName;
    @NotBlank @Size(max = MAX_PHONE_LENGTH)
    private final String phoneNumber;
    @Email @Size(max = MAX_EMAIL_LENGTH)
    private final String email;
    @Size(max = MAX_EXPERIENCE_LENGTH)
    private final String experience;
    @Size(max = MAX_NOTES_LENGTH)
    private final String notes;
    @Size(max = MAX_COLOR_LENGTH)
    private final String color;
    @Min(MIN_PARTICIPANTS) @Max(MAX_PARTICIPANTS)
    private final int participants;

    public Booking(String bookingID, String room, String firstName, String lastName, String phoneNumber, String email,
                   String experience, String notes, int participants, String color) {
        this.bookingID = bookingID;
        this.room = room;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.experience = experience;
        this.notes = notes;
        this.participants = participants;
        this.color = color;
    }

    public String getBookingID() {
        return bookingID;
    }

    public String getRoom() {
        return room;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public String getExperience() {
        return experience;
    }

    public String getNotes() {
        return notes;
    }

    public int getParticipants() {
        return participants;
    }

    public String getColor() {
        return color;
    }
}
