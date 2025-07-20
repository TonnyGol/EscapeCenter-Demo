package com.example.EscapeCenter_Demo.DataBaseService;

import com.example.EscapeCenter_Demo.Client;
import java.sql.*;
import java.util.*;

public class ClientsService extends DefaultService {

    public static Map<String, Client> getAllClients() {
        Map<String, Client> allClients = new HashMap<>();

        String query = "SELECT firstName, lastName, phoneNumber, email FROM clients";

        try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                String firstName = rs.getString("firstName");
                String lastName = rs.getString("lastName");
                String phoneNumber = rs.getString("phoneNumber");
                String email = rs.getString("email");

                String key = firstName + " " + lastName;
                Client client = new Client(firstName, lastName, phoneNumber, email);
                allClients.put(key, client);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return allClients;
    }

}
