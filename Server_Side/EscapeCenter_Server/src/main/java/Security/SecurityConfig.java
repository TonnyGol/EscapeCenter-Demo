package Security;

import Entities.WorkerRole;
import Repositories.WorkerRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig {

    public static final String ADMIN_ROLE = WorkerRole.ADMIN.name();

    // ------------ STAFF USERS ------------
    @Bean
    public UserDetailsService userDetailsService(WorkerRepository workers) {
        return username -> workers.findByUsername(username)
                .map(worker -> User.withUsername(worker.getUsername())
                        .password(worker.getPasswordHash())
                        .roles(worker.getRole().name())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ------------ SECURITY RULES ------------
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // Safe to disable only because the API is stateless: Basic auth is sent on every request and there are no session cookies to forge.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth

                        // Public paths (client can access)
                        // /error: Spring forwards failed requests there, so without it a public 400 or 500 shows up as 401.
                        .requestMatchers("/error").permitAll()
                        // Docker healthcheck, and API docs (the API is public on GitHub anyway; staff calls still need a login).
                        .requestMatchers("/actuator/health", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/rooms", "/api/rooms/*/availability").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/bookings").permitAll()

                        // ADMIN-only paths
                        .requestMatchers("/api/**").hasRole(ADMIN_ROLE)

                        // Everything else - require authentication
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults()); // Simple login for admin

        return http.build();
    }
}
