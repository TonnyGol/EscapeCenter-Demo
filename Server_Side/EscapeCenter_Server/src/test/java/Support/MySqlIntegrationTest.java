package Support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

/**
 * Base class of every test that needs a database: one throwaway MySQL for the whole test run, so no Docker
 * compose stack or DB_* environment variables are needed. It is never stopped by hand, Testcontainers removes it at exit.
 */
public abstract class MySqlIntegrationTest {
    public static final String ADMIN_USER = "admin";
    public static final String ADMIN_PASSWORD = "test-admin-password";

    private static final String MYSQL_IMAGE = "mysql:8.4";
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(MYSQL_IMAGE);

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void connectToContainer(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("app.admin.user", () -> ADMIN_USER);
        registry.add("app.admin.password", () -> ADMIN_PASSWORD);
    }
}
