package com.example.EscapeCenter_Server.Security;

import com.example.EscapeCenter_Server.DataBaseService.WorkersService;
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

    // ------------ ADMIN USER ------------
    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager(
                User.withUsername("admin")
                        .password("1234")       // change to strong password in real use
                        .roles("ADMIN")
                        .build(),

                User.withUsername("*****")
                        .password("*****")
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
                        .requestMatchers("/bookings", "/addBooking").permitAll()

                        // ADMIN-only paths
                        .requestMatchers("/server/**").hasRole("ADMIN")

                        // Everything else - require authentication
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults()); // Simple login for admin

        return http.build();
    }
}
