package ai.nervemind.plugin.api;

import java.util.List;

/**
 * Defines a property that a node accepts.
 * Used by the UI to dynamically generate the properties panel.
 *
 * @param name         Internal name of the property (used as key in settings
 *                     map)
 * @param displayName  Human-readable label for the UI
 * @param type         The type of input control to render
 * @param required     Whether this property must be set
 * @param defaultValue Default value if not specified
 * @param description  Tooltip/help text for the property
 * @param options      For SELECT type: list of valid options
 */
public record PropertyDefinition(
        String name,
        String displayName,
        PropertyType type,
        boolean required,
        Object defaultValue,
        String description,
        List<String> options) {
    /**
     * Creates a required string property.
     * 
     * @param name        the property name
     * @param displayName the human-readable label
     * @param description the help text
     * @return a new PropertyDefinition
     */
    public static PropertyDefinition requiredString(String name, String displayName, String description) {
        return new PropertyDefinition(name, displayName, PropertyType.STRING, true, null, description, List.of());
    }

    /**
     * Creates an optional string property with a default value.
     * 
     * @param name         the property name
     * @param displayName  the human-readable label
     * @param defaultValue the default value
     * @param description  the help text
     * @return a new PropertyDefinition
     */
    public static PropertyDefinition optionalString(String name, String displayName, String defaultValue,
            String description) {
        return new PropertyDefinition(name, displayName, PropertyType.STRING, false, defaultValue, description,
                List.of());
    }

    /**
     * Creates a required path property (file/directory picker).
     * 
     * @param name        the property name
     * @param displayName the human-readable label
     * @param description the help text
     * @return a new PropertyDefinition
     */
    public static PropertyDefinition requiredPath(String name, String displayName, String description) {
        return new PropertyDefinition(name, displayName, PropertyType.PATH, true, null, description, List.of());
    }

    /**
     * Creates an optional boolean property.
     * 
     * @param name         the property name
     * @param displayName  the human-readable label
     * @param defaultValue the default value
     * @param description  the help text
     * @return a new PropertyDefinition
     */
    public static PropertyDefinition optionalBoolean(String name, String displayName, boolean defaultValue,
            String description) {
        return new PropertyDefinition(name, displayName, PropertyType.BOOLEAN, false, defaultValue, description,
                List.of());
    }

    /**
     * Creates a select dropdown property.
     * 
     * @param name         the property name
     * @param displayName  the human-readable label
     * @param options      the available options
     * @param defaultValue the default option
     * @param description  the help text
     * @return a new PropertyDefinition
     */
    public static PropertyDefinition select(String name, String displayName, List<String> options, String defaultValue,
            String description) {
        return new PropertyDefinition(name, displayName, PropertyType.SELECT, false, defaultValue, description,
                options);
    }
}
