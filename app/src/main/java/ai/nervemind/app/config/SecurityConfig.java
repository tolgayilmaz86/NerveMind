package ai.nervemind.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the NerveMind application.
 *
 * <p>
 * This configuration enables HTTP Basic authentication for the application
 * while allowing unrestricted access to the H2 database console for development
 * purposes.
 * </p>
 *
 * <p>
 * <strong>Local-first default:</strong> Allowing <code>/h2-console/**</code>
 * as <code>permitAll()</code> is intentional for desktop/local usage.
 * For shared or network-accessible deployments, this should be hardened
 * (disable H2 console or require authentication).
 * </p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

        /**
         * Configures the security filter chain.
         *
         * @param http the HTTP security configuration
         * @return the configured security filter chain
         * @throws Exception if configuration fails
         */
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .authorizeHttpRequests(authz -> authz
                                                // Local-first default: unrestricted H2 console access.
                                                // Harden this for shared/networked deployments.
                                                .requestMatchers("/h2-console/**").permitAll()
                                                // Allow access to static resources
                                                .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico")
                                                .permitAll()
                                                // Require authentication for all other requests
                                                .anyRequest().authenticated())
                                .httpBasic(httpBasic -> {
                                }) // Enable HTTP Basic authentication
                                .csrf(csrf -> csrf
                                                // Disable CSRF for H2 console (required for H2 console to work)
                                                .ignoringRequestMatchers("/h2-console/**"))
                                .headers(headers -> headers
                                                // Disable X-Frame-Options for H2 console (required for H2 console to
                                                // work)
                                                .frameOptions(frameOptions -> frameOptions.disable()));

                return http.build();
        }
}