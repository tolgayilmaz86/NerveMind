package ai.nervemind.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import ai.nervemind.ui.NerveMindUI;
import javafx.application.Application;

/**
 * Main entry point for the NerveMind application.
 * 
 * <p>
 * This class bootstraps both the Spring Boot backend and the JavaFX frontend,
 * integrating them into a unified desktop application for workflow automation.
 * </p>
 * 
 * <h2>Application Architecture</h2>
 * 
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │                           NerveMind Application                              │
 * ├─────────────────────────────────────────────────────────────────────────────┤
 * │  ┌─────────────────────────────┐    ┌─────────────────────────────────────┐ │
 * │  │     Spring Boot Backend     │    │        JavaFX Frontend              │ │
 * │  │  ┌───────────────────────┐  │    │  ┌───────────────────────────────┐  │ │
 * │  │  │   Workflow Engine     │  │    │  │     Workflow Canvas           │  │ │
 * │  │  │   Plugin System       │  │    │  │     Node Palette              │  │ │
 * │  │  │   Database (H2/SQLite)│  │    │  │     Properties Panel          │  │ │
 * │  │  │   REST API (optional) │  │    │  │     Execution Console         │  │ │
 * │  │  └───────────────────────┘  │    │  └───────────────────────────────┘  │ │
 * │  └─────────────────────────────┘    └─────────────────────────────────────┘ │
 * └─────────────────────────────────────────────────────────────────────────────┘
 * </pre>
 * 
 * <h2>Startup Sequence</h2>
 * <ol>
 * <li><strong>Spring Boot initialization</strong> - Services, repositories, and
 * plugins are loaded</li>
 * <li><strong>JavaFX launch</strong> - The UI is started on the JavaFX
 * Application Thread</li>
 * <li><strong>Context injection</strong> - Spring context is passed to UI
 * components</li>
 * </ol>
 * 
 * <h2>Command Line Options</h2>
 * <ul>
 * <li>{@code --no-ui} - Run in headless mode (useful for testing and
 * CI/CD)</li>
 * </ul>
 * 
 * <h2>Module Dependencies</h2>
 * 
 * <pre>
 *     app (this module)
 *        ├── common (shared interfaces, DTOs, domain models)
 *        ├── plugin-api (plugin development API)
 *        ├── plugins/file-watcher (bundled file trigger plugin)
 *        └── ui (JavaFX user interface)
 * </pre>
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see ai.nervemind.ui.NerveMindUI JavaFX application entry point
 * @see ai.nervemind.app.service.PluginService Plugin discovery and management
 *      /**
 *      Main Spring Boot application class for NerveMind.
 *      Configures the application with scheduling, JPA repositories, and
 *      component scanning.
 *
 * @see ai.nervemind.app.service.WorkflowService Workflow management
 */
@SpringBootApplication(scanBasePackages = "ai.nervemind")
@EnableScheduling
@EnableJpaRepositories(basePackages = "ai.nervemind.app.database.repository")
public class NerveMindApplication {

    // Default constructor for Spring Boot
    /**
     * Default constructor for Spring Boot.
     */
    public NerveMindApplication() {
    }

    private static final Logger log = LoggerFactory.getLogger(NerveMindApplication.class);

    /**
     * Application entry point.
     * 
     * <p>
     * Initializes Spring Boot first, then launches the JavaFX UI unless
     * running in headless mode.
     * </p>
     * 
     * @param args command line arguments; supports {@code --no-ui} for headless
     *             mode
     */
    public static void main(final String[] args) {
        log.info("🚀 NerveMind Application Starting...");

        // AWT system tray requires headless=false; must be set before Spring Boot
        // starts, since SpringApplication sets java.awt.headless=true by default.
        System.setProperty("java.awt.headless", "false");

        // First bootstrap Spring Boot
        final ConfigurableApplicationContext context = SpringApplication.run(NerveMindApplication.class, args);

        log.info("✅ Spring Boot initialized successfully");

        // Check if we should skip UI for testing
        if (args.length > 0 && "--no-ui".equals(args[0])) {
            log.info("🎯 Running in headless mode (no UI) for testing");
            // Keep the application running
            try {
                Thread.currentThread().join();
            } catch (InterruptedException _) {
                log.info("Application interrupted, shutting down");
                Thread.currentThread().interrupt();
            }
            return;
        }

        // Set the context on the UI module
        NerveMindUI.setSpringContext(context);
        NerveMindUI.setApplicationClass(NerveMindApplication.class);

        log.info("🎨 Launching JavaFX UI...");

        // Launch JavaFX application
        Application.launch(NerveMindUI.class, args);
    }
}
