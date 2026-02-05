package ai.nervemind.ui.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import javafx.scene.Scene;

/**
 * Default implementation of ThemeService.
 * 
 * <p>
 * Manages the NordDark theme (and future themes) with centralized
 * color definitions that match the CSS variables.
 */
@Service
public class DefaultThemeService implements ThemeService {

    /**
     * Default constructor.
     */
    public DefaultThemeService() {
        // Default constructor for Spring service
    }

    private static final String STYLES_BASE = "/ai/nervemind/ui/styles/";

    // Core stylesheets always loaded
    private static final List<String> CORE_STYLESHEETS = List.of(
            STYLES_BASE + "main.css",
            STYLES_BASE + "console.css");

    // Additional stylesheets for FXML components
    private final List<String> additionalStylesheets = new ArrayList<>();

    // Color definitions for NordDark theme (matching CSS variables)
    private static final Map<String, String> NORD_DARK_COLORS = createNordDarkColors();

    private Theme currentTheme = Theme.NORD_DARK;
    private Scene primaryScene;

    private static Map<String, String> createNordDarkColors() {
        Map<String, String> colors = new HashMap<>();

        // Background colors
        colors.put("bg-primary", "#2e3440");
        colors.put("bg-secondary", "#3b4252");
        colors.put("bg-tertiary", "#434c5e");
        colors.put("bg-card", "#3b4252");
        colors.put("bg-darker", "#242933");

        // Text colors
        colors.put("text-primary", "#eceff4");
        colors.put("text-secondary", "#d8dee9");
        colors.put("text-muted", "#8b949e");
        colors.put("text-disabled", "#616e88");

        // Accent colors (Nord palette)
        colors.put("accent-blue", "#5e81ac");
        colors.put("accent-blue-light", "#81a1c1");
        colors.put("accent-cyan", "#88c0d0");
        colors.put("accent-teal", "#8fbcbb");
        colors.put("accent-green", "#a3be8c");
        colors.put("accent-yellow", "#ebcb8b");
        colors.put("accent-orange", "#d08770");
        colors.put("accent-red", "#bf616a");
        colors.put("accent-purple", "#b48ead");

        // Semantic colors
        colors.put("success", "#a3be8c");
        colors.put("warning", "#ebcb8b");
        colors.put("danger", "#bf616a");
        colors.put("info", "#88c0d0");

        // UI element colors
        colors.put("border", "#4c566a");
        colors.put("border-light", "#616e88");
        colors.put("shadow", "rgba(0, 0, 0, 0.3)");
        colors.put("overlay", "rgba(46, 52, 64, 0.8)");

        // Node type colors (for canvas)
        colors.put("node-trigger", "#40c057");
        colors.put("node-action", "#4a9eff");
        colors.put("node-condition", "#fcc419");
        colors.put("node-ai", "#9775fa");
        colors.put("node-data", "#ff922b");

        return colors;
    }

    @Override
    /**
     * Gets the current active theme.
     * 
     * @return the current active theme
     */
    public Theme getCurrentTheme() {
        return currentTheme;
    }

    @Override
    /**
     * Sets the active theme.
     * 
     * @param theme the theme to apply
     */
    public void setTheme(Theme theme) {
        if (theme != null && theme != currentTheme) {
            this.currentTheme = theme;
            if (primaryScene != null) {
                applyTheme(primaryScene);
            }
        }
    }

    /**
     * Applies the theme to the given scene.
     * 
     * @param scene the scene to apply the theme to
     */
    @Override
    public void applyTheme(Scene scene) {
        Objects.requireNonNull(scene, "Scene cannot be null");
        this.primaryScene = scene;

        // Clear existing stylesheets
        scene.getStylesheets().clear();

        // Add all stylesheets
        for (String stylesheet : getStylesheets()) {
            String url = getClass().getResource(stylesheet).toExternalForm();
            scene.getStylesheets().add(url);
        }
    }

    /**
     * Gets the list of active stylesheets.
     * 
     * @return a list of active stylesheets
     */
    @Override
    public List<String> getStylesheets() {
        List<String> allStylesheets = new ArrayList<>(CORE_STYLESHEETS);
        allStylesheets.addAll(additionalStylesheets);
        return allStylesheets;
    }

    /**
     * Gets a color value by name.
     * 
     * @param colorName the name of the color to retrieve
     * @return the color hex value
     */
    @Override
    public String getColor(String colorName) {
        // Currently only supporting NordDark
        String color = NORD_DARK_COLORS.get(colorName);
        if (color == null) {
            // Return a fallback color if not found
            return "#ff00ff"; // Magenta - makes missing colors obvious
        }
        return color;
    }

    /**
     * Adds an additional stylesheet.
     * 
     * @param stylesheetPath the path to the stylesheet to add
     */
    @Override
    public void addStylesheet(String stylesheetPath) {
        if (stylesheetPath != null && !additionalStylesheets.contains(stylesheetPath)) {
            additionalStylesheets.add(stylesheetPath);
            if (primaryScene != null) {
                String url = getClass().getResource(stylesheetPath).toExternalForm();
                primaryScene.getStylesheets().add(url);
            }
        }
    }

    /**
     * Removes an additional stylesheet.
     * 
     * @param stylesheetPath the path to the stylesheet to remove
     */
    @Override
    public void removeStylesheet(String stylesheetPath) {
        if (stylesheetPath != null && additionalStylesheets.remove(stylesheetPath) && primaryScene != null) {
            String url = getClass().getResource(stylesheetPath).toExternalForm();
            primaryScene.getStylesheets().remove(url);
        }
    }

    /**
     * Reloads all active stylesheets on the primary scene.
     */
    @Override
    public void reloadStylesheets() {
        if (primaryScene != null) {
            applyTheme(primaryScene);
        }
    }

    /**
     * Gets all available colors for the current theme.
     * Useful for color pickers or theme preview.
     * 
     * @return map of color names to values
     */
    public Map<String, String> getAllColors() {
        // Currently only NordDark
        return new HashMap<>(NORD_DARK_COLORS);
    }

    /**
     * Converts a color name to a CSS variable reference.
     * 
     * @param colorName the color name
     * @return the CSS variable reference (e.g., "-nervemind-bg-primary")
     */
    public String toCssVariable(String colorName) {
        return "-nervemind-" + colorName;
    }
}
