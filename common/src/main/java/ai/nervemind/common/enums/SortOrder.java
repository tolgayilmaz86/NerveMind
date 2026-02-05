package ai.nervemind.common.enums;

/**
 * Sort order for data operations.
 * 
 * <p>
 * This enum provides type-safe sort order identification, replacing
 * hardcoded string values like "asc" and "desc".
 * </p>
 */
public enum SortOrder {
    /** Ascending order (A-Z, 0-9, earliest first) */
    ASC("asc", "Ascending", "Sort in ascending order"),

    /** Descending order (Z-A, 9-0, latest first) */
    DESC("desc", "Descending", "Sort in descending order");

    private final String value;
    private final String displayName;
    private final String description;

    SortOrder(String value, String displayName, String description) {
        this.value = value;
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Get the sort order value for serialization.
     * 
     * @return the order string (e.g., "asc")
     */
    public String getValue() {
        return value;
    }

    /**
     * Get the display name for this sort order.
     * 
     * @return the user-facing name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get the description of this sort order.
     * 
     * @return human-readable description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Check if this is ascending order.
     * 
     * @return true if ascending
     */
    public boolean isAscending() {
        return this == ASC;
    }

    /**
     * Parse a sort order from its string representation.
     * 
     * @param value the string value (case-insensitive)
     * @return the corresponding SortOrder, or ASC as default
     */
    public static SortOrder fromString(String value) {
        if (value == null) {
            return ASC;
        }
        return switch (value.toLowerCase()) {
            case "desc", "descending", "d" -> DESC;
            default -> ASC;
        };
    }

    @Override
    public String toString() {
        return value;
    }
}
