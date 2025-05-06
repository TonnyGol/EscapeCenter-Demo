package com.example.EscapeCenter_Demo;

import java.sql.*;

public class WorkersService {
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/escapecenter";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "1234";

    public static boolean authenticate(String username, String password) {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS)) {
            String query = "SELECT * FROM workers WHERE username = ? AND password = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            stmt.setString(2, password); // Note: for real apps, use hashed passwords!
            ResultSet rs = stmt.executeQuery();

            return rs.next(); // User exists and password matches
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
