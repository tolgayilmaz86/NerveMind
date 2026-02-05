package ai.nervemind.plugin.api;

/**
 * Defines the type of a property for generic UI rendering.
 */
public enum PropertyType {
    /** Single-line text input */
    STRING,
    /** Multi-line text area */
    TEXT,
    /** Numeric input */
    NUMBER,
    /** Boolean checkbox */
    BOOLEAN,
    /** File/directory path picker */
    PATH,
    /** Dropdown selection from predefined options */
    SELECT,
    /** Password/secret input (masked) */
    SECRET,
    /** JSON/Code editor */
    CODE
}
