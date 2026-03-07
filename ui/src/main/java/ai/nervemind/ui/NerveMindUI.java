package ai.nervemind.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;

import ai.nervemind.ui.console.ExecutionConsoleAppender;
import ai.nervemind.ui.console.ExecutionConsoleService;
import ai.nervemind.ui.tray.SystemTrayManager;
import atlantafx.base.theme.NordDark;
import ch.qos.logback.classic.Logger;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
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
    private SystemTrayManager systemTrayManager;

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
            imageView.setFitWidth(280);
            imageView.setFitHeight(210);

            // Create loading text and progress bar
            Label loadingLabel = new Label("Starting NerveMind...");
            loadingLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");

            ProgressBar progressBar = new ProgressBar();
            progressBar.setPrefWidth(220);
            progressBar.setProgress(0.0);

            // Layout
            VBox splashLayout = new VBox(12);
            splashLayout.setAlignment(javafx.geometry.Pos.CENTER);
            splashLayout.setStyle("-fx-background-color: #2d3748; -fx-padding: 20;");
            splashLayout.getChildren().addAll(imageView, loadingLabel, progressBar);

            Scene splashScene = new Scene(splashLayout);
            splashStage.setScene(splashScene);

            // Center on screen
            splashStage.centerOnScreen();
            splashStage.show();

            // Preload on a background thread so progress bar updates are visible
            Thread.ofVirtual().name("splash-loader").start(
                    () -> preloadMainWindow(primaryStage, splashStage, progressBar, loadingLabel));

        } catch (Exception e) {
            LOGGER.warning("Failed to load splash image, proceeding to main window: " + e.getMessage());
            splashStage.hide();
            showMainWindow(primaryStage);
        }
    }

    private void updateSplash(ProgressBar progressBar, Label loadingLabel, double progress, String text) {
        Platform.runLater(() -> {
            loadingLabel.setText(text);
            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.millis(400),
                            new KeyValue(progressBar.progressProperty(), progress)));
            timeline.play();
        });
    }

    /**
     * Preloads the main window components while updating the splash screen
     * progress.
     * Runs on a background thread; all JavaFX work is posted via Platform.runLater.
     */
    private void preloadMainWindow(Stage primaryStage, Stage splashStage, ProgressBar progressBar, Label loadingLabel) {
        try {
            // Step 1: Initialize application
            updateSplash(progressBar, loadingLabel, 0.1, "Initializing application...");

            // Apply AtlantaFX theme (must be on FX thread)
            Platform.runLater(() -> Application.setUserAgentStylesheet(new NordDark().getUserAgentStylesheet()));

            // Step 2: Get FxWeaver
            updateSplash(progressBar, loadingLabel, 0.2, "Loading Spring beans...");
            FxWeaver fxWeaver = springContext.getBean(FxWeaver.class);

            // Step 3: Load main view (this is the heavy operation — must be on FX thread)
            updateSplash(progressBar, loadingLabel, 0.3, "Loading main interface...");

            // FxWeaver.load() touches the scene graph, so it must run on the FX thread.
            // We use a CountDownLatch to wait for it from this background thread.
            var latch = new java.util.concurrent.CountDownLatch(1);
            // Use a single-element array to pass results out of the lambda
            final Scene[] sceneHolder = new Scene[1];
            final ai.nervemind.ui.view.MainViewController[] controllerHolder = new ai.nervemind.ui.view.MainViewController[1];

            Platform.runLater(() -> {
                try {
                    var cav = fxWeaver.load(ai.nervemind.ui.view.MainViewController.class);
                    Parent view = (Parent) cav.getView().get();
                    sceneHolder[0] = new Scene(view, 1400.0, 900.0);
                    controllerHolder[0] = cav.getController();
                } finally {
                    latch.countDown();
                }
            });
            latch.await();

            Scene scene = sceneHolder[0];
            var controller = controllerHolder[0];
            if (scene == null) {
                throw new IllegalStateException("Failed to load main view on FX thread");
            }

            // Step 4: Load CSS
            updateSplash(progressBar, loadingLabel, 0.6, "Loading stylesheets...");
            Platform.runLater(() -> {
                scene.getStylesheets().add(getClass().getResource("/ai/nervemind/ui/styles/main.css").toExternalForm());
                scene.getStylesheets()
                        .add(getClass().getResource("/ai/nervemind/ui/styles/console.css").toExternalForm());
            });

            // Step 5: Setup console appender
            updateSplash(progressBar, loadingLabel, 0.8, "Setting up console...");
            Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            ExecutionConsoleAppender appender = new ExecutionConsoleAppender();
            appender.setContext(root.getLoggerContext());
            appender.start();
            root.addAppender(appender);

            // Step 6 + 7: Configure stage and show (must be on FX thread)
            updateSplash(progressBar, loadingLabel, 0.9, "Almost ready...");
            String appName = applicationClass != null ? applicationClass.getSimpleName().replace("Application", "")
                    : "NerveMind";

            updateSplash(progressBar, loadingLabel, 1.0, "Ready!");
            // Small pause so the user sees "Ready!" at 100%
            Thread.sleep(300);

            Platform.runLater(() -> {
                primaryStage.setOnCloseRequest(event -> {
                    if (!controller.checkUnsavedChanges()) {
                        event.consume();
                    }
                });

                primaryStage.setTitle(appName + " - Workflow Automation");
                primaryStage.setScene(scene);
                primaryStage.setMinWidth(1200);
                primaryStage.setMinHeight(700);
                loadApplicationIcons(primaryStage);

                primaryStage.setMaximized(true);
                primaryStage.show();

                // Install system tray (overrides close handler with minimize-to-tray)
                installSystemTray(primaryStage);

                splashStage.hide();

                LOGGER.info(() -> "Starting "
                        + (applicationClass != null ? applicationClass.getSimpleName() : "JavaFX") + " UI");
            });

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.severe("Splash loading interrupted");
            Platform.runLater(() -> {
                splashStage.hide();
                showMainWindow(primaryStage);
            });
        } catch (Exception e) {
            LOGGER.severe("Failed to preload main window: " + e.getMessage());
            Platform.runLater(() -> {
                splashStage.hide();
                showMainWindow(primaryStage);
            });
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
        var cav = fxWeaver.load(ai.nervemind.ui.view.MainViewController.class);
        Parent view = (Parent) cav.getView().get();
        Scene scene = new Scene(view, 1400.0, 900.0);
        var controller = cav.getController();

        // Handle window close request
        primaryStage.setOnCloseRequest(event -> {
            if (!controller.checkUnsavedChanges()) {
                event.consume();
            }
        });

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

        // Install system tray (overrides close handler with minimize-to-tray)
        installSystemTray(primaryStage);
    }

    /**
     * Installs the system tray icon with workflow menu and minimize-to-tray
     * support.
     */
    private void installSystemTray(Stage primaryStage) {
        try {
            var workflowService = springContext.getBean(
                    ai.nervemind.common.service.WorkflowServiceInterface.class);
            var executionService = springContext.getBean(
                    ai.nervemind.common.service.ExecutionServiceInterface.class);

            systemTrayManager = new SystemTrayManager(
                    primaryStage, workflowService, executionService, this::performQuit);

            if (!systemTrayManager.install()) {
                LOGGER.warning("System tray not available; close will exit normally");
                systemTrayManager = null;
            }
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.WARNING, "Failed to install system tray", e);
            systemTrayManager = null;
        }
    }

    /**
     * Full application quit invoked from system tray Quit action.
     */
    private void performQuit() {
        if (systemTrayManager != null) {
            systemTrayManager.uninstall();
            systemTrayManager = null;
        }
        ExecutionConsoleService.getInstance().shutdown();
        if (springContext != null) {
            springContext.close();
        }
        Platform.exit();
    }

    @Override
    public void stop() {
        // Shutdown subsidiary windows/services before exiting
        if (systemTrayManager != null) {
            systemTrayManager.uninstall();
            systemTrayManager = null;
        }
        ExecutionConsoleService.getInstance().shutdown();

        // Shutdown Spring context
        if (springContext != null) {
            springContext.close();
        }
        Platform.exit();
    }
}
