package service;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Base64;
import java.util.Properties;

/**
 * Singleton that holds the current user session: authentication state,
 * server URL, and shared instances of HttpClient and ObjectMapper.
 *
 * Usage:
 *   Session.getInstance().login("user", "pass");
 *   Session.getInstance().getHttpClient();
 */
public final class Session {

    // Without timeouts a server that stops answering would freeze the screen that is waiting for it.
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final String REQUEST_TIMEOUT_PROPERTY = "request.timeout.seconds";
    private static final String DEFAULT_REQUEST_TIMEOUT_SECONDS = "10";

    // After the constants: the constructor reads them, and statics are set in the order they are written.
    private static final Session INSTANCE = new Session();

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Properties config;

    private String username;
    private String encodedAuth;

    private Session() {
        this.httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
        this.config = loadConfig();
    }

    public static Session getInstance() {
        return INSTANCE;
    }

    // ---- Authentication ----

    /**
     * Store login credentials for all future API calls.
     */
    public void login(String username, String password) {
        this.username = username;
        String authString = username + ":" + password;
        this.encodedAuth = Base64.getEncoder().encodeToString(authString.getBytes());
    }

    public void logout() {
        this.username = null;
        this.encodedAuth = null;
    }

    public boolean isAuthenticated() {
        return encodedAuth != null;
    }

    public String getUsername() {
        return username;
    }

    public String getAuthHeader() {
        return "Basic " + encodedAuth;
    }

    // ---- Shared Instances ----

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    // ---- Configuration ----

    /**
     * Returns the server base URL (e.g. "http://localhost:8080").
     */
    public String getServerUrl() {
        return config.getProperty("server.url", "http://localhost:8080");
    }

    /** How long one request may wait for the server's answer. */
    public Duration getRequestTimeout() {
        return Duration.ofSeconds(Long.parseLong(
                config.getProperty(REQUEST_TIMEOUT_PROPERTY, DEFAULT_REQUEST_TIMEOUT_SECONDS)));
    }

    private Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (is != null) {
                props.load(is);
            } else {
                System.err.println("[Session] config.properties not found — using defaults.");
            }
        } catch (IOException e) {
            System.err.println("[Session] Failed to load config.properties: " + e.getMessage());
        }
        return props;
    }
}
