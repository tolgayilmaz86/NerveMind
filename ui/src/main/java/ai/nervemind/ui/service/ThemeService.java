package ai.nervemind.ui.service;

import java.util.List;

import javafx.scene.Scene;

/**
 * Service interface for managing application themes and CSS.
 * 
 * <p>
 * Provides functionality for:
 * <ul>
 * <li>Loading and switching themes</li>
 * <li>Managing CSS stylesheets</li>
 * <li>Providing theme color values for programmatic use</li>
 * </ul>
 */
public interface ThemeService {

    /**
     * The available theme names.
     */
    enum Theme {
        NORD_DARK("NordDark", "Nord Dark theme (default)"),
        NORD_LIGHT("NordLight", "Nord Light theme");

        private final String displayName;
        private final String description;

        Theme(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Gets the current active theme.
     * 
     * @return the current theme
     */
    Theme getCurrentTheme();

    /**
     * Sets the active theme.
     * 
     * @param theme the theme to apply
     */
    void setTheme(Theme theme);

    /**
     * Applies the current theme to a scene.
     * 
     * @param scene the scene to style
     */
    void applyTheme(Scene scene);

    /**
     * Gets the list of application stylesheets to apply.
     * 
     * @return list of stylesheet URLs
     */
    List<String> getStylesheets();

    /**
     * Gets a color value by name from the current theme.
     * 
     * @param colorName the color variable name (e.g., "bg-primary", "accent-blue")
     * @return the color value as a CSS color string (e.g., "#2e3440")
     */
    String getColor(String colorName);

    /**
     * Adds a stylesheet to the application.
     * 
     * @param stylesheetPath the path to the stylesheet resource
     */
    void addStylesheet(String stylesheetPath);

    /**
     * Removes a stylesheet from the application.
     * 
     * @param stylesheetPath the path to the stylesheet resource
     */
    void removeStylesheet(String stylesheetPath);

    /**
     * Reloads all stylesheets (useful for development/hot-reload).
     */
    void reloadStylesheets();
}
