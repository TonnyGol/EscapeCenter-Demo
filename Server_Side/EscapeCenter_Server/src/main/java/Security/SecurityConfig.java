package Security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;

@Configuration
public class SecurityConfig {

    public static final String ADMIN_ROLE = "ADMIN";

    // ------------ ADMIN USER ------------
    // shortcut: one admin from env vars, replaced by BCrypt users from the workers table in roadmap Phase 4c.
    @Bean
    public UserDetailsService userDetailsService(@Value("${app.admin.user}") String adminUser,
                                                 @Value("${app.admin.password}") String adminPassword) {
        return new InMemoryUserDetailsManager(
                User.withUsername(adminUser)
                        .password(adminPassword)
                        .roles(ADMIN_ROLE)
                        .build()
        );
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance(); // for testing only
    }

    // ------------ SECURITY RULES ------------
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())   // For React / external frontend
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth

                        // Public paths (client can access)
                        // /error: Spring forwards failed requests there, so without it a public 400 or 500 shows up as 401.
                        .requestMatchers("/error").permitAll()
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
