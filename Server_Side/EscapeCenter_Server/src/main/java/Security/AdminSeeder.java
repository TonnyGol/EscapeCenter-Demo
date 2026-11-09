package Security;

import Entities.Worker;
import Entities.WorkerRole;
import Repositories.WorkerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Keeps the admin in the workers table in step with APP_ADMIN_*, so no password lives in a committed migration.
@Component
class AdminSeeder implements ApplicationRunner {

    private static final String BLANK_PASSWORD_MESSAGE = "APP_ADMIN_PASSWORD must not be blank";

    private final WorkerRepository workers;
    private final PasswordEncoder passwordEncoder;
    private final String adminUser;
    private final String adminPassword;

    AdminSeeder(WorkerRepository workers, PasswordEncoder passwordEncoder,
                @Value("${app.admin.user}") String adminUser,
                @Value("${app.admin.password}") String adminPassword) {
        this.workers = workers;
        this.passwordEncoder = passwordEncoder;
        this.adminUser = adminUser;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminPassword.isBlank()) {
            throw new IllegalStateException(BLANK_PASSWORD_MESSAGE);
        }
        Worker admin = workers.findByUsername(adminUser)
                .orElseGet(() -> new Worker(adminUser, null, WorkerRole.ADMIN));
        if (admin.getPasswordHash() == null || !passwordEncoder.matches(adminPassword, admin.getPasswordHash())) {
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            workers.save(admin);
        }
    }
}
