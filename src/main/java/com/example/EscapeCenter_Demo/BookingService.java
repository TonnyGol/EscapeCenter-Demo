package com.example.EscapeCenter_Demo;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class BookingService {
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/escapecenter";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "1234";

    public static void addBooking(Booking booking) {
        String insertClientSQL = "INSERT IGNORE INTO clients (firstName, lastName, phoneNumber, email) VALUES (?, ?, ?, ?)";
        String insertBookingSQL = "INSERT INTO bookings (BookingID, firstName, lastName, phoneNumber, email, experience, notes, participants) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS)) {
            conn.setAutoCommit(false); // Start transaction

            try (PreparedStatement clientStmt = conn.prepareStatement(insertClientSQL);
                 PreparedStatement bookingStmt = conn.prepareStatement(insertBookingSQL)) {

                // Insert client if not exists
                clientStmt.setString(1, booking.getFirstName());
                clientStmt.setString(2, booking.getLastName());
                clientStmt.setString(3, booking.getPhoneNumber());
                clientStmt.setString(4, booking.getEmail());
                clientStmt.executeUpdate();

                // Insert booking
                bookingStmt.setString(1, booking.getBookingID());
                bookingStmt.setString(2, booking.getFirstName());
                bookingStmt.setString(3, booking.getLastName());
                bookingStmt.setString(4, booking.getPhoneNumber());
                bookingStmt.setString(5, booking.getEmail());
                bookingStmt.setString(6, booking.getExperience());
                bookingStmt.setString(7, booking.getNotes());
                bookingStmt.setInt(8, booking.getParticipants());
                bookingStmt.executeUpdate();

                conn.commit(); // Commit both
            } catch (SQLException ex) {
                conn.rollback(); // Rollback if error
                ex.printStackTrace();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static void updateBooking(Booking booking) {
        String sql = "UPDATE bookings SET firstName=?, lastName=?, email=?, experience=?, notes=?, participants=? WHERE BookingID=?";
        try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, booking.getFirstName());
            stmt.setString(2, booking.getLastName());
            stmt.setString(3, booking.getEmail());
            stmt.setString(4, booking.getExperience());
            stmt.setString(5, booking.getNotes());
            stmt.setInt(6, booking.getParticipants());
            stmt.setString(7, booking.getBookingID());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void deleteBooking(String bookingID) {
        String sql = "DELETE FROM bookings WHERE BookingID=?";
        try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, bookingID);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Map<String, Booking> getAllBookings() {
        Map<String, Booking> allBookings = new HashMap<>();

        // Example with JDBC
        try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM bookings")) {

            while (rs.next()) {
                String key = rs.getString("BookingID"); // Make sure this column exists
                Booking booking = new Booking(
                        key,
                        rs.getString("firstName"),
                        rs.getString("lastName"),
                        rs.getString("phoneNumber"),
                        rs.getString("email"),
                        rs.getString("experience"),
                        rs.getString("notes"),
                        rs.getInt("participants")
                );
                allBookings.put(key, booking);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return allBookings;
    }
}

