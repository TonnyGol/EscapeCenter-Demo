package App;

import Controllers.WebController;
import DataBaseService.DefaultService;
import Security.SecurityConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Objects;


// This class is not in a parent package of the controllers and configs, so Spring must be told where they are.
// Add a class here when a new package gets Spring beans (CorsConfig shares the package of SecurityConfig).
@SpringBootApplication(scanBasePackageClasses = {WebController.class, SecurityConfig.class})
public class EscapeCenterServer {

	public static void main(String[] args) {
		System.setProperty("java.awt.headless", "false");
		// Touching DB_PASS loads DefaultService now, so a missing env var fails at startup instead of on the first request.
		Objects.requireNonNull(DefaultService.DB_PASS);

		SpringApplication.run(EscapeCenterServer.class, args);
		System.out.println("Starting server on http://localhost:8080");

	}

}
