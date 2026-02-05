package ai.nervemind.app.config;

import org.h2.server.web.JakartaWebServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * H2 Console configuration for Spring Boot 4.x.
 *
 * <p>
 * In Spring Boot 4.x, the H2 console servlet needs to be explicitly registered
 * as the automatic configuration may not work properly with the newer servlet
 * API.
 * </p>
 */
@Configuration
public class H2ConsoleConfig {

    /**
     * Registers the H2 console servlet.
     *
     * @return the servlet registration bean for H2 console
     */
    @Bean
    public ServletRegistrationBean<JakartaWebServlet> h2ConsoleServlet() {
        ServletRegistrationBean<JakartaWebServlet> registration = new ServletRegistrationBean<>();
        registration.setServlet(new JakartaWebServlet());
        registration.addUrlMappings("/h2-console/*");
        return registration;
    }
}