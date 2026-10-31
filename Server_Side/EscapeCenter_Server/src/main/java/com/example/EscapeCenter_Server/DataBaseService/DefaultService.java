package com.example.EscapeCenter_Server.DataBaseService;

public abstract class DefaultService {

    private static final String ENV_DB_URL = "DB_URL";
    private static final String ENV_DB_USER = "DB_USER";
    private static final String ENV_DB_PASS = "DB_PASS";
    private static final String MISSING_ENV_MESSAGE = " environment variable must be set";

    public static final String JDBC_URL = System.getenv().getOrDefault(
            ENV_DB_URL, "jdbc:mysql://localhost:3306/escapecenter"
    );

    public static final String DB_USER = System.getenv().getOrDefault(ENV_DB_USER, "root");

    public static final String DB_PASS = requireEnv(ENV_DB_PASS);

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + MISSING_ENV_MESSAGE);
        }
        return value;
    }
}
