package com.example.EscapeCenter_Demo.DataBaseService;

import java.sql.*;

public class WorkersService extends DefaultService {

    public static boolean authenticate(String username, String password) {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS)) {
            String query = "SELECT * FROM workers WHERE username = ? AND password = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();

            return rs.next(); // User exists and password matches
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
