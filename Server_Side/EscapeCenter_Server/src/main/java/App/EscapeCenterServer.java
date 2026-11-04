package App;

import Controllers.ApiController;
import DataBaseService.BookingService;
import Exceptions.GlobalExceptionHandler;
import Entities.Booking;
import Repositories.BookingRepository;
import Security.SecurityConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.List;

// This class is not in a parent package of the controllers, services, entities and configs, so Spring must be told where they are.
// Add a class here when a new package gets Spring beans (CorsConfig shares the package of SecurityConfig).
@SpringBootApplication(scanBasePackageClasses = {ApiController.class, BookingService.class, GlobalExceptionHandler.class, SecurityConfig.class})
@EntityScan(basePackageClasses = Booking.class)
@EnableJpaRepositories(basePackageClasses = BookingRepository.class)
public class EscapeCenterServer {

	private static final List<String> REQUIRED_ENV_VARS = List.of("MYSQL_USER", "MYSQL_PASSWORD");
	private static final String MISSING_ENV_MESSAGE = " environment variable must be set";

	public static void main(String[] args) {
		requireEnvVars();
		SpringApplication.run(EscapeCenterServer.class, args);
	}

	// Spring leaves a missing ${MYSQL_USER} in application.properties as plain text, so without this check
	// the server starts and fails later with a confusing "Access denied for user '${MYSQL_USER}'".
	private static void requireEnvVars() {
		for (String name : REQUIRED_ENV_VARS) {
			String value = System.getenv(name);
			if (value == null || value.isBlank()) {
				throw new IllegalStateException(name + MISSING_ENV_MESSAGE);
			}
		}
	}

}
