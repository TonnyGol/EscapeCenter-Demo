package com.example.EscapeCenter_Server.Requests;

public class DeleteRequest {
    private final String bookingID, roomName;

    public DeleteRequest(String bookingID, String roomName) {
        this.bookingID = bookingID;
        this.roomName = roomName;
    }

    public String getBookingID() {
        return bookingID;
    }

    public String getRoomName() {
        return roomName;
    }
}
