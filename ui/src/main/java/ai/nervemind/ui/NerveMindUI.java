package ai.nervemind.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;

import ai.nervemind.ui.console.ExecutionConsoleAppender;
import ai.nervemind.ui.console.ExecutionConsoleService;
import atlantafx.base.theme.NordDark;
import ch.qos.logback.classic.Logger;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import net.rgielen.fxweaver.core.FxWeaver;

/**
 * JavaFX Application entry point for the NerveMind user interface.
 * 
 * This class serves as the bridge between Spring Boot and JavaFX. It is
 * launched
 * from the main application class (ai.nervemind.app.NerveMindApplication) after
 * Spring Boot has
 * initialized,
 * ensuring all services and repositories are available to UI controllers.
 *
 * 
 * <h2>Initialization Flow</h2>
 * <ol>
 * <li>Spring Boot context is set via
 * {@link #setSpringContext(ConfigurableApplicationContext)}</li>
 * <li>JavaFX launches this class via {@code Application.launch()}</li>
 * <li>{@link #init()} validates the Spring context is available</li>
 * <li>{@link #start(Stage)} creates the main window with Spring-wired
 * controllers</li>
 * </ol>
 * 
 * <h2>Spring Integration</h2>
 * <p>
 * Uses <a href="https://github.com/rgielen/fxweaver">FxWeaver</a> to load FXML
 * files with Spring-managed controllers. This allows controllers to use
 * {@code @Autowired}
 * for dependency injection.
 * </p>
 * 
 * <h2>Theming</h2>
 * <p>
 * Uses <a href="https://mkpaz.github.io/atlantafx/">AtlantaFX</a> for modern
 * UI styling. The default theme is NordDark.
 * </p>
 * 
 * <h2>Lifecycle</h2>
 * <ul>
 * <li><strong>start()</strong> - Creates the main window and loads the initial
 * view</li>
 * <li><strong>stop()</strong> - Closes the Spring context and exits the
 * platform</li>
 * </ul>
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see "ai.nervemind.app.NerveMindApplication Main application entry point"
 * @see ai.nervemind.ui.view.MainViewController Main view controller
 */
public class NerveMindUI extends Application {

    /**
     * Default constructor for JavaFX Application.
     * Required by JavaFX framework - do not use directly.
     */
    public NerveMindUI() {
        // Default constructor required by JavaFX
    }

    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger
            .getLogger(NerveMindUI.class.getName());

    private static ConfigurableApplicationContext springContext;
    private static Class<?> applicationClass;

    /**
     * Sets the Spring application context.
     * 
     * <p>
     * Must be called before {@code Application.launch()} to provide
     * Spring dependencies to UI controllers.
     * </p>
     * 
     * @param context the Spring Boot application context
     */
    public static void setSpringContext(ConfigurableApplicationContext context) {
        springContext = context;
    }

    /**
     * Sets the main application class for display purposes.
     * 
     * @param appClass the main application class (used for window title)
     */
    public static void setApplicationClass(Class<?> appClass) {
        applicationClass = appClass;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * Validates that the Spring context has been set before the UI launches.
     * </p>
     * 
     * @throws IllegalStateException if Spring context is not set
     */
    @Override
    public void init() {
        // Context is set externally by the app module
        if (springContext == null) {
            throw new IllegalStateException("Spring context must be set before launching UI");
        }
        if (applicationClass == null) {
            LOGGER.warning("Application class not set, using default behavior");
        }
    }

    @Override
    public void start(Stage primaryStage) {
        // Show splash screen first
        showSplashScreen(primaryStage);
    }

    /**
     * Shows the splash screen with snapshot.png while preloading the main window
     */
    private void showSplashScreen(Stage primaryStage) {
        // Create splash screen stage
        Stage splashStage = new Stage();
        splashStage.initStyle(StageStyle.UNDECORATED);
        splashStage.setTitle("NerveMind - Starting...");

        // Set splash screen icon so taskbar shows our icon instead of Java logo
        loadApplicationIcons(splashStage);

        try {
            // Load snapshot image
            Image splashImage = new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream("/images/snapshot.png")));

            // Create image view for splash
            javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(splashImage);
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(800); // Reasonable size for splash
            imageView.setFitHeight(600);

            // Create loading text and progress bar
            Label loadingLabel = new Label("Starting NerveMind...");
            loadingLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

            ProgressBar progressBar = new ProgressBar();
            progressBar.setPrefWidth(300);
            progressBar.setProgress(0.0); // Start at 0, will update as we load

            // Layout
            VBox splashLayout = new VBox(20);
            splashLayout.setAlignment(javafx.geometry.Pos.CENTER);
            splashLayout.setStyle("-fx-background-color: #2d3748; -fx-padding: 40;");
            splashLayout.getChildren().addAll(imageView, loadingLabel, progressBar);

            Scene splashScene = new Scene(splashLayout);
            splashStage.setScene(splashScene);

            // Center on screen
            splashStage.centerOnScreen();
            splashStage.show();

            // Start preloading on JavaFX thread with progress updates
            Platform.runLater(() -> preloadMainWindow(primaryStage, splashStage, progressBar, loadingLabel));

        } catch (Exception e) {
            LOGGER.warning("Failed to load splash image, proceeding to main window: " + e.getMessage());
            splashStage.hide();
            showMainWindow(primaryStage);
        }
    }

    /**
     * Preloads the main window components while updating the splash screen progress
     */
    private void preloadMainWindow(Stage primaryStage, Stage splashStage, ProgressBar progressBar, Label loadingLabel) {
        try {
            // Step 1: Initialize application
            loadingLabel.setText("Initializing application...");
            progressBar.setProgress(0.1);

            // Apply AtlantaFX theme
            Application.setUserAgentStylesheet(new NordDark().getUserAgentStylesheet());

            // Step 2: Get FxWeaver
            progressBar.setProgress(0.2);
            FxWeaver fxWeaver = springContext.getBean(FxWeaver.class);

            // Step 3: Load main view (this is the heavy operation)
            loadingLabel.setText("Loading main interface...");
            progressBar.setProgress(0.3);
            Scene scene = new Scene(
                    fxWeaver.loadView(ai.nervemind.ui.view.MainViewController.class),
                    1400, 900);

            // Step 4: Load CSS
            progressBar.setProgress(0.6);
            scene.getStylesheets().add(getClass().getResource("/ai/nervemind/ui/styles/main.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/ai/nervemind/ui/styles/console.css").toExternalForm());

            // Step 5: Setup console appender
            progressBar.setProgress(0.8);
            Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            ExecutionConsoleAppender appender = new ExecutionConsoleAppender();
            appender.setContext(root.getLoggerContext());
            appender.start();
            root.addAppender(appender);

            // Step 6: Configure stage
            progressBar.setProgress(0.9);
            String appName = applicationClass != null ? applicationClass.getSimpleName().replace("Application", "")
                    : "NerveMind";
            primaryStage.setTitle(appName + " - Workflow Automation");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1200);
            primaryStage.setMinHeight(700);

            // Step 7: Load icons
            loadApplicationIcons(primaryStage);
            progressBar.setProgress(1.0);
            loadingLabel.setText("Ready!");

            // Show main window immediately, then hide splash (no gap between windows)
            primaryStage.setMaximized(true);
            primaryStage.show();

            // Small delay to show "Ready!" then hide splash
            PauseTransition finishPause = new PauseTransition(Duration.millis(300));
            finishPause.setOnFinished(event -> {
                splashStage.hide();

                // Log successful startup
                LOGGER.info(() -> "Starting " + (applicationClass != null ? applicationClass.getSimpleName() : "JavaFX")
                        + " UI");
            });
            finishPause.play();

        } catch (Exception e) {
            LOGGER.severe("Failed to preload main window: " + e.getMessage());
            splashStage.hide();
            // Try to show main window anyway
            showMainWindow(primaryStage);
        }
    }

    /**
     * Loads application icons for the given stage
     */
    private void loadApplicationIcons(Stage stage) {
        try {
            // Try to load multiple icon sizes for best quality
            List<Image> icons = new ArrayList<>();

            // Add available icon sizes (system will choose best match)
            try {
                icons.add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/icon-16.png"))));
            } catch (Exception _) {
            }
            try {
                icons.add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/icon-32.png"))));
            } catch (Exception _) {
            }
            try {
                icons.add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/icon-64.png"))));
            } catch (Exception _) {
            }
            try {
                icons.add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/icon-128.png"))));
            } catch (Exception _) {
            }

            if (!icons.isEmpty()) {
                stage.getIcons().addAll(icons);
            } else {
                // Fallback to single large icon
                stage.getIcons().add(new Image(
                        Objects.requireNonNull(getClass().getResourceAsStream("/images/icon.png"))));
            }
        } catch (Exception _) {
            // No icons available, continue without them
        }
    }

    /**
     * Shows the main application window
     */
    private void showMainWindow(Stage primaryStage) {
        // Log application startup with class info
        LOGGER.info(
                () -> "Starting " + (applicationClass != null ? applicationClass.getSimpleName() : "JavaFX") + " UI");

        // Apply AtlantaFX theme
        Application.setUserAgentStylesheet(new NordDark().getUserAgentStylesheet());

        // Get FxWeaver for Spring-managed controllers
        FxWeaver fxWeaver = springContext.getBean(FxWeaver.class);

        // Load main view
        Scene scene = new Scene(
                fxWeaver.loadView(ai.nervemind.ui.view.MainViewController.class),
                1400, 900);

        // Load application CSS, without this left menu would look sparse
        scene.getStylesheets().add(getClass().getResource("/ai/nervemind/ui/styles/main.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/ai/nervemind/ui/styles/console.css").toExternalForm());

        // Programmatically add the ExecutionConsoleAppender
        Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        ExecutionConsoleAppender appender = new ExecutionConsoleAppender();
        appender.setContext(root.getLoggerContext());
        appender.start();
        root.addAppender(appender);

        // Configure stage
        String appName = applicationClass != null ? applicationClass.getSimpleName().replace("Application", "")
                : "NerveMind";
        primaryStage.setTitle(appName + " - Workflow Automation");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1200);
        primaryStage.setMinHeight(700);

        // Load application icons
        loadApplicationIcons(primaryStage);

        // Start maximized on primary monitor
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    @Override
    public void stop() {
        // Shutdown subsidiary windows/services before exiting
        ExecutionConsoleService.getInstance().shutdown();

        // Shutdown Spring context
        if (springContext != null) {
            springContext.close();
        }
        Platform.exit();
    }
}
