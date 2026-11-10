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
    // One more than the filter's tracking limit of 10,000, so the table is over its limit.
    private static final int FULL_TABLE_ADDRESSES = 10_001;

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

    @Test
    void whenTheTrackingTableIsFull_newAddressesPassUncounted_andTrackedOnesStayLimited() throws Exception {
        for (int address = 0; address <= FULL_TABLE_ADDRESSES; address++) {
            assertEquals(200, send("GET", ROOMS_URL, addressNumber(address), true));
        }

        for (int attempt = 0; attempt < LIMIT * 3; attempt++) {
            assertEquals(200, send("GET", ROOMS_URL, "9.9.9.9", true));
        }
        assertEquals(200, send("GET", ROOMS_URL, addressNumber(0), true));
        assertEquals(TOO_MANY_REQUESTS, send("GET", ROOMS_URL, addressNumber(0), true));
    }

    private static String addressNumber(int number) {
        return "10.0." + number / 256 + "." + number % 256;
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
