package escapecenter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class MainAppTest {

    @Test
    void sessionSingletonExists() {
        assertNotNull(service.Session.getInstance());
    }
}
