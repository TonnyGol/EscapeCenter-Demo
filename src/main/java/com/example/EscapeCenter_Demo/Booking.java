package com.example.EscapeCenter_Demo;

public class Booking {
    private String bookingID;
    private String firstName, lastName, phoneNumber, email, experience, additionalNumber,
            notes;
    private int participants;

    public Booking(String bookingID, String firstName, String lastName, String phoneNumber, String email,
                   String experience, String notes, int participants) {
        this.bookingID = bookingID;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.experience = experience;
        this.additionalNumber = "";
        this.notes = notes;
        this.participants = participants;
    }

    public String getBookingID() {
        return bookingID;
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

    public String getAdditionalNumber() {
        return additionalNumber;
    }

    public String getNotes() {
        return notes;
    }

    public int getParticipants() {
        return participants;
    }
}
