package com.example.EscapeCenter_Demo;

public class Booking {
    private String bookingID, room;
    private String firstName, lastName, phoneNumber, email, experience, notes, color;
    private int participants;

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
