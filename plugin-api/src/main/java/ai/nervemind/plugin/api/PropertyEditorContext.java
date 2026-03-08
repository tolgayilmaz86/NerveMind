package ai.nervemind.plugin.api;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Context provided to plugins when creating custom property editors.
 * 
 * <p>
 * This record provides everything needed to create a custom editor control
 * for a specific property. It includes the property definition, current value,
 * and a callback to notify the system when the value changes.
 * </p>
 * 
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * // In your plugin, create custom editors for specific properties
 * public javafx.scene.Node createCustomEditor(PropertyEditorContext context) {
 *     if ("colorPicker".equals(context.propertyName())) {
 *         var colorPicker = new ColorPicker();
 *         String currentValue = (String) context.currentValue();
 *         if (currentValue != null) {
 *             colorPicker.setValue(Color.web(currentValue));
 *         }
 *         colorPicker.setOnAction(e -> {
 *             context.onValueChanged().accept(
 *                     colorPicker.getValue().toString());
 *         });
 *         return colorPicker;
 *     }
 *     return null; // Use default editor
 * }
 * }</pre>
 * 
 * @param propertyDefinition the definition of the property being edited
 * @param currentValue       the current value of the property (may be null)
 * @param allSettings        all settings for the node (for dependent
 *                           properties)
 * @param onValueChanged     callback to invoke when the property value changes
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see PropertyDefinition
 */
public record PropertyEditorContext(
        PropertyDefinition propertyDefinition,
        Object currentValue,
        Map<String, Object> allSettings,
        Consumer<Object> onValueChanged) {

    /**
     * Gets the name of the property being edited.
     * 
     * @return the property name
     */
    public String propertyName() {
        return propertyDefinition.name();
    }

    /**
     * Gets the display name of the property.
     * 
     * @return the human-readable property label
     */
    public String displayName() {
        return propertyDefinition.displayName();
    }

    /**
     * Gets the property type.
     * 
     * @return the PropertyType enum value
     */
    public PropertyType propertyType() {
        return propertyDefinition.type();
    }

    /**
     * Checks if this property is required.
     * 
     * @return true if the property must have a value
     */
    public boolean isRequired() {
        return propertyDefinition.required();
    }

    /**
     * Gets another setting value from the node.
     * 
     * <p>
     * Useful for creating dependent properties that change behavior
     * based on other settings.
     * </p>
     * 
     * @param settingName the name of the setting
     * @return the setting value, or null if not set
     */
    public Object getSetting(String settingName) {
        return allSettings.get(settingName);
    }

    /**
     * Notifies the system that the property value has changed.
     * 
     * @param newValue the new value
     */
    public void updateValue(Object newValue) {
        onValueChanged.accept(newValue);
    }
}
