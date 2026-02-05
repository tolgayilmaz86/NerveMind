package ai.nervemind.common.enums;

/**
 * Data types for application setting values.
 * 
 * <p>
 * Each setting has a type that determines how its value is stored,
 * validated, and rendered in the UI. All values are stored as strings
 * in the database but converted to their appropriate type on retrieval.
 * </p>
 * 
 * <h2>Type Handling</h2>
 * <ul>
 * <li><strong>Serialization</strong> - All types serialize to String for
 * storage</li>
 * <li><strong>Validation</strong> - Types are validated on input (e.g., INTEGER
 * must be numeric)</li>
 * <li><strong>UI Rendering</strong> - Types determine the input control (text
 * field, checkbox, file picker)</li>
 * </ul>
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see ai.nervemind.common.dto.SettingDTO#asBoolean() Type-safe value accessors
 * @see ai.nervemind.common.service.SettingsServiceInterface Type-safe value
 *      methods
 */
public enum SettingType {
    /** Plain text string value */
    STRING,

    /** Integer numeric value (32-bit) */
    INTEGER,

    /** Long integer value (64-bit) */
    LONG,

    /** Floating-point numeric value */
    DOUBLE,

    /** Boolean true/false value (renders as checkbox) */
    BOOLEAN,

    /** Encrypted string for sensitive values (renders as password field) */
    PASSWORD,

    /** File system path (renders with file picker) */
    PATH,

    /** Complex nested object stored as JSON string */
    JSON,

    /** Enumeration value stored as string (renders as dropdown) */
    ENUM
}
