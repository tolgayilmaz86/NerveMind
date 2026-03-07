package ai.nervemind.ui.tray;

import java.awt.AWTException;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignE;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignW;

import ai.nervemind.common.dto.ExecutionDTO;
import ai.nervemind.common.dto.WorkflowDTO;
import ai.nervemind.common.enums.ExecutionStatus;
import ai.nervemind.common.service.ExecutionServiceInterface;
import ai.nervemind.common.service.WorkflowServiceInterface;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Manages the system tray icon and a styled JavaFX popup menu.
 *
 * <p>
 * Provides a tray icon using {@code logo-trans.png} that allows the user to:
 * </p>
 * <ul>
 * <li>Show/hide the main application window</li>
 * <li>Run any saved workflow directly from the tray</li>
 * <li>Quit the application</li>
 * </ul>
 *
 * <p>
 * Uses a JavaFX undecorated Stage as the popup menu instead of the bare
 * AWT PopupMenu, giving full control over styling and layout.
 * </p>
 *
 * @since 1.2.0
 */
public class SystemTrayManager {

    private static final Logger LOGGER = Logger.getLogger(SystemTrayManager.class.getName());

    private static final String MENU_BG = "#2e3440";
    private static final String MENU_HOVER = "#3b4252";
    private static final String MENU_TEXT = "#d8dee9";
    private static final String MENU_ACCENT = "#88c0d0";
    private static final String MENU_SEPARATOR = "#4c566a";
    private static final String MENU_MUTED = "#7b88a1";
    private static final String MENU_SUCCESS = "#a3be8c";
    private static final String MENU_RUNNING = "#ebcb8b";

    private final Stage primaryStage;
    private final WorkflowServiceInterface workflowService;
    private final ExecutionServiceInterface executionService;
    private final Runnable onQuit;

    private TrayIcon trayIcon;
    private Stage popupStage;
    private Stage hiddenOwner;

    /**
     * Creates a new SystemTrayManager.
     *
     * @param primaryStage     the main application stage
     * @param workflowService  service to list available workflows
     * @param executionService service to execute workflows
     * @param onQuit           callback invoked when the user selects Quit
     */
    public SystemTrayManager(Stage primaryStage,
            WorkflowServiceInterface workflowService,
            ExecutionServiceInterface executionService,
            Runnable onQuit) {
        this.primaryStage = primaryStage;
        this.workflowService = workflowService;
        this.executionService = executionService;
        this.onQuit = onQuit;
    }

    /**
     * Initializes the system tray icon and installs the close-to-tray behavior
     * on the primary stage.
     *
     * @return true if the system tray was set up successfully
     */
    public boolean install() {
        if (!SystemTray.isSupported()) {
            LOGGER.warning("System tray is not supported on this platform");
            return false;
        }

        try {
            Platform.setImplicitExit(false);

            // Create a hidden utility stage as the owner for the popup.
            // Utility-owned stages don't appear in the taskbar.
            Platform.runLater(() -> {
                hiddenOwner = new Stage();
                hiddenOwner.initStyle(StageStyle.UTILITY);
                hiddenOwner.setOpacity(0);
                hiddenOwner.setWidth(0);
                hiddenOwner.setHeight(0);
                hiddenOwner.setX(Double.MAX_VALUE);
                hiddenOwner.show();
            });

            java.awt.Image image = loadTrayImage();

            // No AWT popup — we use a JavaFX stage instead
            trayIcon = new TrayIcon(image, "NerveMind");
            trayIcon.setImageAutoSize(true);

            trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                        showWindow();
                    } else if (e.getClickCount() == 1) {
                        final int x = e.getXOnScreen();
                        final int y = e.getYOnScreen();
                        Platform.runLater(() -> showPopup(x, y));
                    }
                }
            });

            SystemTray.getSystemTray().add(trayIcon);

            primaryStage.setOnCloseRequest(event -> {
                event.consume();
                hideToTray();
            });

            LOGGER.info("System tray icon installed");
            return true;

        } catch (AWTException | IOException e) {
            LOGGER.log(Level.WARNING, "Failed to install system tray icon", e);
            return false;
        }
    }

    /**
     * Removes the tray icon from the system tray.
     */
    public void uninstall() {
        if (trayIcon != null && SystemTray.isSupported()) {
            SystemTray.getSystemTray().remove(trayIcon);
            trayIcon = null;
        }
        Platform.runLater(() -> {
            if (popupStage != null) {
                popupStage.hide();
                popupStage = null;
            }
            if (hiddenOwner != null) {
                hiddenOwner.hide();
                hiddenOwner = null;
            }
        });
    }

    // ===== JavaFX Popup Menu =====

    private void showPopup(double screenX, double screenY) {
        if (popupStage != null && popupStage.isShowing()) {
            popupStage.hide();
            popupStage = null;
            return;
        }

        popupStage = new Stage();
        popupStage.initStyle(StageStyle.TRANSPARENT);
        if (hiddenOwner != null) {
            popupStage.initOwner(hiddenOwner);
        }
        popupStage.setAlwaysOnTop(true);

        VBox menu = buildMenuContent();

        Scene scene = new Scene(menu);
        scene.setFill(Color.TRANSPARENT);

        // Close popup when clicking elsewhere
        popupStage.focusedProperty().addListener((_, _, focused) -> {
            if (!focused && popupStage != null) {
                popupStage.hide();
                popupStage = null;
            }
        });

        popupStage.setScene(scene);

        // Position above the tray icon (taskbar is usually at the bottom)
        popupStage.setOnShown(_ -> {
            double popupW = popupStage.getWidth();
            double popupH = popupStage.getHeight();
            // Keep within screen bounds
            javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
            double x = Math.max(screenBounds.getMinX(),
                    Math.min(screenX - popupW / 2, screenBounds.getMaxX() - popupW));
            double y = screenY - popupH - 8;
            // If not enough room above, show below
            if (y < screenBounds.getMinY()) {
                y = screenY + 8;
            }
            popupStage.setX(x);
            popupStage.setY(y);
        });

        popupStage.show();
        popupStage.requestFocus();
    }

    private VBox buildMenuContent() {
        VBox root = new VBox();
        root.setPadding(new Insets(10));
        root.setSpacing(3);
        root.setStyle(
                "-fx-background-color: " + MENU_BG + ";"
                        + "-fx-background-radius: 10;"
                        + "-fx-border-color: " + MENU_SEPARATOR + ";"
                        + "-fx-border-radius: 10;"
                        + "-fx-border-width: 1;"
                        + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 16, 0, 0, 4);");
        root.setMinWidth(320);

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 14, 10, 14));

        try {
            Image logoImg = new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream("/images/logo-trans.png")));
            ImageView logo = new ImageView(logoImg);
            logo.setFitWidth(28);
            logo.setFitHeight(28);
            logo.setPreserveRatio(true);
            header.getChildren().add(logo);
        } catch (Exception _) {
            // Skip logo if unavailable
        }

        Label title = new Label("NerveMind");
        title.setStyle("-fx-text-fill: " + MENU_ACCENT + "; -fx-font-size: 16px; -fx-font-weight: bold;");
        header.getChildren().add(title);
        root.getChildren().add(header);

        root.getChildren().add(createSeparator());

        // Show Window
        root.getChildren().add(createMenuItem("Show Window", MaterialDesignW.WINDOW_RESTORE, this::showWindow));

        root.getChildren().add(createSeparator());

        // Workflow section header
        Label wfHeader = new Label("  Run Workflow");
        wfHeader.setStyle("-fx-text-fill: " + MENU_MUTED + "; -fx-font-size: 12px; -fx-padding: 6 14 3 14;");
        root.getChildren().add(wfHeader);

        // Workflow items (clicking does NOT close the popup — allows multiple runs)
        try {
            List<WorkflowDTO> workflows = workflowService.findAll();
            if (workflows.isEmpty()) {
                Label empty = new Label("  No workflows saved");
                empty.setStyle("-fx-text-fill: " + MENU_MUTED + "; -fx-font-size: 13px; -fx-padding: 8 14;");
                root.getChildren().add(empty);
            } else {
                for (WorkflowDTO wf : workflows) {
                    root.getChildren().add(createWorkflowItem(wf));
                }
                if (workflows.size() > 1) {
                    root.getChildren().add(createRunAllItem(workflows));
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load workflows for tray menu", e);
            Label err = new Label("  Error loading workflows");
            err.setStyle("-fx-text-fill: #bf616a; -fx-font-size: 13px; -fx-padding: 8 14;");
            root.getChildren().add(err);
        }

        root.getChildren().add(createSeparator());

        // Quit
        root.getChildren().add(createMenuItem("Quit", MaterialDesignE.EXIT_RUN, this::quit));

        return root;
    }

    private HBox createMenuItem(String text, org.kordamp.ikonli.Ikon icon, Runnable action) {
        HBox item = new HBox(12);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(10, 18, 10, 14));
        item.setStyle("-fx-background-color: transparent; -fx-background-radius: 6; -fx-cursor: hand;");

        FontIcon fontIcon = new FontIcon(icon);
        fontIcon.setIconSize(20);
        fontIcon.setIconColor(Color.web(MENU_TEXT));

        Label label = new Label(text);
        label.setStyle("-fx-text-fill: " + MENU_TEXT + "; -fx-font-size: 14px;");

        item.getChildren().addAll(fontIcon, label);

        item.setOnMouseEntered(_ -> item.setStyle(
                "-fx-background-color: " + MENU_HOVER + "; -fx-background-radius: 6; -fx-cursor: hand;"));
        item.setOnMouseExited(_ -> item.setStyle(
                "-fx-background-color: transparent; -fx-background-radius: 6; -fx-cursor: hand;"));

        item.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                if (popupStage != null) {
                    popupStage.hide();
                }
                action.run();
            }
        });

        return item;
    }

    private HBox createWorkflowItem(WorkflowDTO wf) {
        HBox item = new HBox(12);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(10, 18, 10, 14));
        item.setStyle("-fx-background-color: transparent; -fx-background-radius: 6; -fx-cursor: hand;");

        FontIcon playIcon = new FontIcon(MaterialDesignP.PLAY_CIRCLE_OUTLINE);
        playIcon.setIconSize(20);
        playIcon.setIconColor(Color.web(MENU_TEXT));

        Label label = new Label("  " + wf.name());
        label.setStyle("-fx-text-fill: " + MENU_TEXT + "; -fx-font-size: 14px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Status icon — hidden initially, shown after click
        FontIcon statusIcon = new FontIcon(MaterialDesignC.CHECK_CIRCLE);
        statusIcon.setIconSize(16);
        statusIcon.setVisible(false);

        item.getChildren().addAll(playIcon, label, spacer, statusIcon);

        item.setOnMouseEntered(_ -> item.setStyle(
                "-fx-background-color: " + MENU_HOVER + "; -fx-background-radius: 6; -fx-cursor: hand;"));
        item.setOnMouseExited(_ -> item.setStyle(
                "-fx-background-color: transparent; -fx-background-radius: 6; -fx-cursor: hand;"));

        item.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                // Show running indicator — don't close popup
                statusIcon.setIconColor(Color.web(MENU_RUNNING));
                statusIcon.setIconCode(MaterialDesignP.PROGRESS_CLOCK);
                statusIcon.setVisible(true);
                executeWorkflowWithStatus(wf, statusIcon);
            }
        });

        return item;
    }

    private HBox createRunAllItem(List<WorkflowDTO> workflows) {
        HBox item = new HBox(12);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(10, 18, 10, 14));
        item.setStyle("-fx-background-color: transparent; -fx-background-radius: 6; -fx-cursor: hand;");

        FontIcon icon = new FontIcon(MaterialDesignP.PLAY_BOX_MULTIPLE_OUTLINE);
        icon.setIconSize(20);
        icon.setIconColor(Color.web(MENU_ACCENT));

        Label label = new Label("  Run All (" + workflows.size() + ")");
        label.setStyle("-fx-text-fill: " + MENU_ACCENT + "; -fx-font-size: 14px; -fx-font-weight: bold;");

        item.getChildren().addAll(icon, label);

        item.setOnMouseEntered(_ -> item.setStyle(
                "-fx-background-color: " + MENU_HOVER + "; -fx-background-radius: 6; -fx-cursor: hand;"));
        item.setOnMouseExited(_ -> item.setStyle(
                "-fx-background-color: transparent; -fx-background-radius: 6; -fx-cursor: hand;"));

        item.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                for (WorkflowDTO wf : workflows) {
                    executeWorkflow(wf);
                }
                if (popupStage != null) {
                    popupStage.hide();
                }
            }
        });

        return item;
    }

    private Separator createSeparator() {
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: " + MENU_SEPARATOR + "; -fx-padding: 4 0;");
        return sep;
    }

    // ===== Window Management =====

    private void showWindow() {
        Platform.runLater(() -> {
            primaryStage.show();
            primaryStage.setIconified(false);
            primaryStage.toFront();
            primaryStage.requestFocus();
        });
    }

    private void hideToTray() {
        Platform.runLater(() -> {
            primaryStage.hide();
            if (trayIcon != null) {
                trayIcon.displayMessage("NerveMind",
                        "Running in background. Double-click tray icon to restore.",
                        TrayIcon.MessageType.INFO);
            }
        });
    }

    // ===== Workflow Execution =====

    private void executeWorkflow(WorkflowDTO workflow) {
        executeWorkflowWithStatus(workflow, null);
    }

    private void executeWorkflowWithStatus(WorkflowDTO workflow, FontIcon statusIcon) {
        LOGGER.info(() -> "Tray: executing workflow '" + workflow.name() + "' (id=" + workflow.id() + ")");

        if (trayIcon != null) {
            trayIcon.displayMessage("NerveMind",
                    "Running: " + workflow.name(),
                    TrayIcon.MessageType.INFO);
        }

        CompletableFuture<ExecutionDTO> future = executionService.executeAsync(workflow.id(), Map.of());
        future.thenAccept(result -> {
            String message;
            TrayIcon.MessageType messageType;
            if (result.status() == ExecutionStatus.SUCCESS) {
                message = "Completed: " + workflow.name();
                messageType = TrayIcon.MessageType.INFO;
                if (statusIcon != null) {
                    Platform.runLater(() -> {
                        statusIcon.setIconCode(MaterialDesignC.CHECK_CIRCLE);
                        statusIcon.setIconColor(Color.web(MENU_SUCCESS));
                    });
                }
            } else if (result.status() == ExecutionStatus.CANCELLED) {
                message = "Cancelled: " + workflow.name();
                messageType = TrayIcon.MessageType.WARNING;
                if (statusIcon != null) {
                    Platform.runLater(() -> {
                        statusIcon.setIconCode(MaterialDesignC.CANCEL);
                        statusIcon.setIconColor(Color.web(MENU_RUNNING));
                    });
                }
            } else {
                message = "Failed: " + workflow.name();
                messageType = TrayIcon.MessageType.ERROR;
                if (statusIcon != null) {
                    Platform.runLater(() -> {
                        statusIcon.setIconCode(MaterialDesignC.CLOSE_CIRCLE);
                        statusIcon.setIconColor(Color.web("#bf616a"));
                    });
                }
            }
            LOGGER.info(() -> "Tray: " + message);
            if (trayIcon != null) {
                trayIcon.displayMessage("NerveMind", message, messageType);
            }
        }).exceptionally(ex -> {
            LOGGER.log(Level.WARNING, "Tray: workflow execution failed", ex);
            if (statusIcon != null) {
                Platform.runLater(() -> {
                    statusIcon.setIconCode(MaterialDesignC.CLOSE_CIRCLE);
                    statusIcon.setIconColor(Color.web("#bf616a"));
                });
            }
            if (trayIcon != null) {
                trayIcon.displayMessage("NerveMind",
                        "Error: " + workflow.name() + " - " + ex.getMessage(),
                        TrayIcon.MessageType.ERROR);
            }
            return null;
        });
    }

    private void quit() {
        uninstall();
        Platform.runLater(onQuit);
    }

    private java.awt.Image loadTrayImage() throws IOException {
        try (InputStream is = Objects.requireNonNull(
                getClass().getResourceAsStream("/images/logo-trans.png"),
                "logo-trans.png not found on classpath")) {
            return ImageIO.read(is);
        }
    }
}
