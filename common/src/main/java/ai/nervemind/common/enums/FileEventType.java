package ai.nervemind.common.enums;

/**
 * File system event types for file watching triggers.
 * 
 * <p>
 * This enum provides type-safe file event identification, replacing
 * hardcoded string values like "CREATE", "MODIFY", "DELETE".
 * </p>
 */
public enum FileEventType {
    /** File was created */
    CREATE("CREATE", "File Created", "A new file was created"),

    /** File was modified */
    MODIFY("MODIFY", "File Modified", "An existing file was modified"),

    /** File was deleted */
    DELETE("DELETE", "File Deleted", "A file was deleted"),

    /** File was renamed (may be reported as DELETE + CREATE) */
    RENAME("RENAME", "File Renamed", "A file was renamed"),

    /** Directory overflow - too many events */
    OVERFLOW("OVERFLOW", "Event Overflow", "Too many events occurred to track");

    private final String value;
    private final String displayName;
    private final String description;

    FileEventType(String value, String displayName, String description) {
        this.value = value;
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Get the event type value for serialization.
     * 
     * @return the event type string (e.g., "CREATE")
     */
    public String getValue() {
        return value;
    }

    /**
     * Get the display name for this event type.
     * 
     * @return the user-facing name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get the description of this event type.
     * 
     * @return human-readable description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Parse a file event type from its string representation.
     * 
     * @param value the string value (case-insensitive)
     * @return the corresponding FileEventType, or MODIFY as default
     */
    public static FileEventType fromString(String value) {
        if (value == null) {
            return MODIFY;
        }
        return switch (value.toUpperCase()) {
            case "CREATE", "ENTRY_CREATE" -> CREATE;
            case "DELETE", "ENTRY_DELETE" -> DELETE;
            case "RENAME" -> RENAME;
            case "OVERFLOW", "ENTRY_OVERFLOW" -> OVERFLOW;
            default -> MODIFY;
        };
    }

    /**
     * Get common file event types for UI configuration.
     * 
     * @return array of commonly watched event types
     */
    public static FileEventType[] commonTypes() {
        return new FileEventType[] { CREATE, MODIFY, DELETE };
    }

    /**
     * Parse a comma-separated string of event types.
     * 
     * @param eventTypesStr comma-separated event types (e.g., "CREATE,MODIFY")
     * @return array of FileEventType
     */
    public static FileEventType[] parseMultiple(String eventTypesStr) {
        if (eventTypesStr == null || eventTypesStr.isBlank()) {
            return commonTypes();
        }
        String[] parts = eventTypesStr.split(",");
        FileEventType[] types = new FileEventType[parts.length];
        for (int i = 0; i < parts.length; i++) {
            types[i] = fromString(parts[i].trim());
        }
        return types;
    }

    /**
     * Convert an array of event types to a comma-separated string.
     * 
     * @param types the event types
     * @return comma-separated string (e.g., "CREATE,MODIFY,DELETE")
     */
    public static String toCommaSeparated(FileEventType[] types) {
        if (types == null || types.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < types.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(types[i].getValue());
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return value;
    }
}
