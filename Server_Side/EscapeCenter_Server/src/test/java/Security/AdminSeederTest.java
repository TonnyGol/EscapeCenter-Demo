package Security;

import App.EscapeCenterServer;
import Entities.Worker;
import Repositories.WorkerRepository;
import Support.MySqlIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = EscapeCenterServer.class)
class AdminSeederTest extends MySqlIntegrationTest {

    private static final String SEEDED_USER = "SeederTestAdmin";
    private static final String FIRST_PASSWORD = "first-password";
    private static final String ROTATED_PASSWORD = "rotated-password";

    @Autowired
    private WorkerRepository workers;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void removeSeededWorker() {
        workers.findByUsername(SEEDED_USER).ifPresent(workers::delete);
    }

    @Test
    void run_createsTheAdminAndFollowsPasswordChanges() {
        seedWith(FIRST_PASSWORD);
        seedWith(ROTATED_PASSWORD);

        Worker admin = workers.findByUsername(SEEDED_USER).orElseThrow();
        assertTrue(passwordEncoder.matches(ROTATED_PASSWORD, admin.getPasswordHash()));
        assertEquals(1, workers.findAll().stream().filter(w -> w.getUsername().equals(SEEDED_USER)).count());
    }

    @Test
    void run_refusesABlankPassword() {
        assertThrows(IllegalStateException.class, () -> seedWith(" "));
    }

    private void seedWith(String password) {
        new AdminSeeder(workers, passwordEncoder, SEEDED_USER, password).run(null);
    }
}
