package DataBaseService;

import Entities.Booking;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class BookingService extends DefaultService {

    public static void addBooking(Booking booking) throws SQLException {
        String insertClientSQL = "INSERT IGNORE INTO clients (firstName, lastName, phoneNumber, email) VALUES (?, ?, ?, ?)";
        String insertBookingSQL = "INSERT INTO bookings (BookingID, room, firstName, lastName," +
                "phoneNumber, email, experience, notes, participants, color) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS)) {
            conn.setAutoCommit(false);

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
                bookingStmt.setString(2, booking.getRoom());
                bookingStmt.setString(3, booking.getFirstName());
                bookingStmt.setString(4, booking.getLastName());
                bookingStmt.setString(5, booking.getPhoneNumber());
                bookingStmt.setString(6, booking.getEmail());
                bookingStmt.setString(7, booking.getExperience());
                bookingStmt.setString(8, booking.getNotes());
                bookingStmt.setInt(9, booking.getParticipants());
                bookingStmt.setString(10, booking.getColor());
                bookingStmt.executeUpdate();

                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }


    public static int updateBooking(Booking booking) throws SQLException {
        String sql = "UPDATE bookings SET firstName=?, lastName=?, phoneNumber=?, email=?, experience=?, notes=?, participants=?, color=? WHERE BookingID=? AND room=?";
        try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, booking.getFirstName());
            stmt.setString(2, booking.getLastName());
            stmt.setString(3, booking.getPhoneNumber());
            stmt.setString(4, booking.getEmail());
            stmt.setString(5, booking.getExperience());
            stmt.setString(6, booking.getNotes());
            stmt.setInt(7, booking.getParticipants());
            stmt.setString(8, booking.getColor());
            stmt.setString(9, booking.getBookingID());
            stmt.setString(10, booking.getRoom());

            return stmt.executeUpdate();
        }
    }

    public static int deleteBooking(String bookingID, String room) throws SQLException {
        String sql = "DELETE FROM bookings WHERE BookingID=? AND room=?";
        try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, bookingID);
            stmt.setString(2, room);

            return stmt.executeUpdate();
        }
    }

    public static Map<String, Booking> getAllBookings() throws SQLException {
        Map<String, Booking> allBookings = new HashMap<>();

        try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM bookings")) {

            while (rs.next()) {
                String key = rs.getString("BookingID");
                Booking booking = new Booking(
                        key,
                        rs.getString("room"),
                        rs.getString("firstName"),
                        rs.getString("lastName"),
                        rs.getString("phoneNumber"),
                        rs.getString("email"),
                        rs.getString("experience"),
                        rs.getString("notes"),
                        rs.getInt("participants"),
                        rs.getString("color")
                );
                allBookings.put(key+"/"+booking.getRoom(), booking);
            }
        }

        return allBookings;
    }
}

