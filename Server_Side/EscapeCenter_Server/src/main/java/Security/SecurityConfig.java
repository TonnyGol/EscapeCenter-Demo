package Security;

import DataBaseService.DefaultService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    private static final String ENV_ADMIN_USER = "APP_ADMIN_USER";
    private static final String ENV_ADMIN_PASSWORD = "APP_ADMIN_PASSWORD";

    // ------------ ADMIN USER ------------
    // shortcut: one admin from env vars, replaced by BCrypt users from the workers table in roadmap Phase 4c.
    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager(
                User.withUsername(DefaultService.requireEnv(ENV_ADMIN_USER))
                        .password(DefaultService.requireEnv(ENV_ADMIN_PASSWORD))
                        .roles("ADMIN")
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
                        .requestMatchers("/bookings", "/addBooking", "/error").permitAll()

                        // ADMIN-only paths
                        .requestMatchers("/server/**").hasRole("ADMIN")

                        // Everything else - require authentication
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults()); // Simple login for admin

        return http.build();
    }
}
