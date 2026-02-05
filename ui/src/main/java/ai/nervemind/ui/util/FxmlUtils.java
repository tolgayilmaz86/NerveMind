package ai.nervemind.ui.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import org.springframework.context.ApplicationContext;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.util.Callback;

/**
 * Utility class for loading FXML files.
 * 
 * <p>
 * Provides helper methods for:
 * <ul>
 * <li>Loading FXML with Spring DI for controllers</li>
 * <li>Loading FXML resources by convention</li>
 * <li>Error handling for FXML loading</li>
 * </ul>
 */
public final class FxmlUtils {

    /**
     * Default base path for FXML resources. Can be overridden via system property
     * "nervemind.fxml.basePath".
     */
    private static final String DEFAULT_FXML_BASE_PATH = "/ai/nervemind/ui/view/";

    private static String fxmlBasePath = System.getProperty("nervemind.fxml.basePath", DEFAULT_FXML_BASE_PATH);

    private FxmlUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Sets the base path for FXML resources.
     * 
     * @param basePath the new base path (must start with /)
     */
    public static void setFxmlBasePath(String basePath) {
        fxmlBasePath = basePath;
    }

    /**
     * Gets the current FXML base path.
     * 
     * @return the current base path
     */
    public static String getFxmlBasePath() {
        return fxmlBasePath;
    }

    /**
     * Loads an FXML file using Spring for controller instantiation.
     * 
     * @param <T>      the type of the root element
     * @param fxmlPath the path to the FXML file (relative to fxml folder)
     * @param context  the Spring application context for controller creation
     * @return the loaded root element
     */
    public static <T extends Parent> T load(String fxmlPath, ApplicationContext context) {
        return load(fxmlPath, context::getBean);
    }

    /**
     * Loads an FXML file with a custom controller factory.
     * 
     * @param <T>               the type of the root element
     * @param fxmlPath          the path to the FXML file (relative to fxml folder)
     * @param controllerFactory factory for creating controllers
     * @return the loaded root element
     */
    @SuppressWarnings("unchecked")
    public static <T extends Parent> T load(String fxmlPath, Callback<Class<?>, Object> controllerFactory) {
        try {
            FXMLLoader loader = createLoader(fxmlPath);
            loader.setControllerFactory(controllerFactory);
            return (T) loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load FXML: " + fxmlPath, e);
        }
    }

    /**
     * Loads an FXML file and returns both the root element and controller.
     * 
     * @param <T>      the type of the root element
     * @param <C>      the type of the controller
     * @param fxmlPath the path to the FXML file (relative to fxml folder)
     * @param context  the Spring application context for controller creation
     * @return a LoadResult containing the root and controller
     */
    public static <T extends Parent, C> LoadResult<T, C> loadWithController(
            String fxmlPath,
            ApplicationContext context) {
        return loadWithController(fxmlPath, context::getBean);
    }

    /**
     * Loads an FXML file and returns both the root element and controller.
     * 
     * @param <T>               the type of the root element
     * @param <C>               the type of the controller
     * @param fxmlPath          the path to the FXML file (relative to fxml folder)
     * @param controllerFactory factory for creating controllers
     * @return a LoadResult containing the root and controller
     */
    @SuppressWarnings("unchecked")
    public static <T extends Parent, C> LoadResult<T, C> loadWithController(
            String fxmlPath,
            Callback<Class<?>, Object> controllerFactory) {
        try {
            FXMLLoader loader = createLoader(fxmlPath);
            loader.setControllerFactory(controllerFactory);
            T root = (T) loader.load();
            C controller = loader.getController();
            return new LoadResult<>(root, controller);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load FXML: " + fxmlPath, e);
        }
    }

    /**
     * Loads an FXML file for a custom component (where the component IS the root).
     * 
     * @param component the component instance (also the root and controller)
     * @param fxmlPath  the path to the FXML file
     */
    public static void loadComponent(Object component, String fxmlPath) {
        try {
            FXMLLoader loader = createLoader(fxmlPath);
            loader.setRoot(component);
            loader.setController(component);
            loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load component FXML: " + fxmlPath, e);
        }
    }

    /**
     * Loads an FXML file with a pre-set controller instance.
     * 
     * @param <T>        the type of the root element
     * @param fxmlPath   the path to the FXML file
     * @param controller the controller instance to use
     * @return the loaded root element
     */
    @SuppressWarnings("unchecked")
    public static <T extends Parent> T loadWithController(String fxmlPath, Object controller) {
        try {
            FXMLLoader loader = createLoader(fxmlPath);
            loader.setController(controller);
            return (T) loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load FXML: " + fxmlPath, e);
        }
    }

    /**
     * Creates a configured FXMLLoader for the given path.
     * 
     * @param fxmlPath the path to the FXML file
     * @return a configured FXMLLoader
     */
    private static FXMLLoader createLoader(String fxmlPath) {
        String fullPath = fxmlPath.startsWith("/") ? fxmlPath : fxmlBasePath + fxmlPath;
        URL resource = FxmlUtils.class.getResource(fullPath);
        if (resource == null) {
            throw new IllegalArgumentException("FXML resource not found: " + fullPath);
        }
        FXMLLoader loader = new FXMLLoader(resource);
        loader.setResources(getDefaultResourceBundle());
        return loader;
    }

    /**
     * Gets the default resource bundle for FXML files (if any).
     * <p>
     * Currently returns null as i18n is not implemented.
     * When i18n support is needed, this should return a ResourceBundle
     * loaded from a .properties file based on the current locale.
     * 
     * @return the default resource bundle or null
     */
    private static ResourceBundle getDefaultResourceBundle() {
        // i18n not implemented - return null to use default text from FXML
        return null;
    }

    /**
     * Gets the standard FXML path for a controller class.
     * 
     * <p>
     * Convention: {@code ai.nervemind.ui.view.dialog.SettingsDialogController}
     * maps to {@code view/dialog/SettingsDialog.fxml}
     * 
     * @param controllerClass the controller class
     * @return the conventional FXML path
     */
    public static String getFxmlPathForController(Class<?> controllerClass) {
        String className = controllerClass.getSimpleName();
        String fxmlName = className.replace("Controller", "") + ".fxml";

        // Extract package suffix (e.g., "dialog" from "ai.nervemind.ui.view.dialog")
        String packageName = controllerClass.getPackageName();
        String[] parts = packageName.split("\\.");
        String folder = parts[parts.length - 1];

        return folder + "/" + fxmlName;
    }

    /**
     * Result of loading FXML with both root and controller.
     * 
     * @param <T>        the type of the root element
     * @param <C>        the type of the controller
     * @param root       the loaded root Parent element
     * @param controller the instantiated controller
     */
    public record LoadResult<T extends Parent, C>(T root, C controller) {

        /**
         * Applies a consumer to the controller.
         * 
         * @param consumer the consumer to apply to the controller
         * @return this LoadResult
         */
        public LoadResult<T, C> withController(Consumer<C> consumer) {
            consumer.accept(controller);
            return this;
        }
    }
}
