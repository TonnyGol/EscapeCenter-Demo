package com.example.EscapeCenter_Server.DataBaseService;

public abstract class DefaultService {

    // Read values from environment variables (with defaults if not provided)
    public static final String JDBC_URL = System.getenv().getOrDefault(
            "DB_URL", "jdbc:mysql://localhost:3306/escapecenter"
    );

    public static final String DB_USER = System.getenv().getOrDefault(
            "DB_USER", "root"
    );

    public static final String DB_PASS = System.getenv().getOrDefault(
            "DB_PASS", "1234"
    );
}
