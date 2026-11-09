package Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Slows down scripts: public booking requests and anything with a login header (password guessing)
 * are limited per client address and minute.
 * shortcut: counts in memory and uses the socket address, so it is per server and, behind a reverse proxy,
 * sees the proxy's address; when a proxy is added (roadmap phase 8) turn on forwarded headers or limit there.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitFilter extends OncePerRequestFilter {
    private static final String BOOKINGS_PATH = "/api/bookings";
    private static final long WINDOW_MILLIS = 60_000;
    private static final int MAX_TRACKED_ADDRESSES = 10_000;
    private static final String RETRY_AFTER_SECONDS = "60";
    private static final String TOO_MANY_REQUESTS_BODY = "{\"status\":429,\"detail\":\"Too many requests. Please wait a minute.\"}";

    private record Window(long startMillis, int count) {
    }

    private final int bookingsPerMinute;
    private final int authenticatedPerMinute;
    private final Map<String, Window> bookingWindows = new ConcurrentHashMap<>();
    private final Map<String, Window> authenticatedWindows = new ConcurrentHashMap<>();

    public RateLimitFilter(@Value("${app.rate-limit.bookings-per-minute}") int bookingsPerMinute,
                           @Value("${app.rate-limit.authenticated-per-minute}") int authenticatedPerMinute) {
        this.bookingsPerMinute = bookingsPerMinute;
        this.authenticatedPerMinute = authenticatedPerMinute;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String address = request.getRemoteAddr();
        boolean isPublicBooking = HttpMethod.POST.matches(request.getMethod()) && BOOKINGS_PATH.equals(request.getRequestURI());
        boolean hasLogin = request.getHeader(HttpHeaders.AUTHORIZATION) != null;

        boolean allowed = (!isPublicBooking || withinLimit(bookingWindows, address, bookingsPerMinute))
                && (!hasLogin || withinLimit(authenticatedWindows, address, authenticatedPerMinute));
        if (allowed) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write(TOO_MANY_REQUESTS_BODY);
    }

    private static boolean withinLimit(Map<String, Window> windows, String address, int limit) {
        long now = System.currentTimeMillis();
        if (windows.size() > MAX_TRACKED_ADDRESSES) {
            windows.values().removeIf(window -> now - window.startMillis() >= WINDOW_MILLIS);
        }
        Window current = windows.compute(address, (key, old) ->
                old == null || now - old.startMillis() >= WINDOW_MILLIS
                        ? new Window(now, 1)
                        : new Window(old.startMillis(), old.count() + 1));
        return current.count() <= limit;
    }
}
