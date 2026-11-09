package Security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateLimitFilterTest {

    private static final int LIMIT = 2;
    private static final int TOO_MANY_REQUESTS = 429;
    private static final String BOOKINGS_URL = "/api/bookings";
    private static final String ROOMS_URL = "/api/rooms";

    private final RateLimitFilter filter = new RateLimitFilter(LIMIT, LIMIT);
    private final FilterChain passThrough = (request, response) -> { };

    @Test
    void publicBookings_areLimitedPerAddress() throws Exception {
        for (int attempt = 0; attempt < LIMIT; attempt++) {
            assertEquals(200, send("POST", BOOKINGS_URL, "1.1.1.1", false));
        }
        assertEquals(TOO_MANY_REQUESTS, send("POST", BOOKINGS_URL, "1.1.1.1", false));
        assertEquals(200, send("POST", BOOKINGS_URL, "2.2.2.2", false));
    }

    @Test
    void requestsWithALogin_areLimited_andPlainReadsAreNot() throws Exception {
        for (int attempt = 0; attempt < LIMIT; attempt++) {
            assertEquals(200, send("GET", ROOMS_URL, "1.1.1.1", true));
        }
        assertEquals(TOO_MANY_REQUESTS, send("GET", ROOMS_URL, "1.1.1.1", true));
        for (int attempt = 0; attempt < LIMIT * 3; attempt++) {
            assertEquals(200, send("GET", ROOMS_URL, "3.3.3.3", false));
        }
    }

    private int send(String method, String url, String address, boolean withLogin) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, url);
        request.setRemoteAddr(address);
        if (withLogin) {
            request.addHeader("Authorization", "Basic abc");
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, passThrough);
        return response.getStatus();
    }
}
