/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.common.enums;

/**
 * Categories for icon libraries available in the icon picker.
 * 
 * <p>
 * Each category represents a different icon font library that can be
 * used throughout the application. New icon libraries can be added
 * by extending this enum.
 * </p>
 */
public enum IconCategory {

    /**
     * Material Design Icons (MDI) - comprehensive icon set.
     */
    MATERIAL_DESIGN("Material Design"),

    /**
     * FontAwesome Solid and Regular icons.
     */
    FONTAWESOME("FontAwesome"),

    /**
     * FontAwesome Brand icons (logos, social media, etc.).
     */
    BRANDS("Brands");

    private final String displayName;

    IconCategory(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Get the human-readable display name for UI.
     * 
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Find an IconCategory by its display name.
     * 
     * @param displayName the display name to search for
     * @return the matching IconCategory, or null if not found
     */
    public static IconCategory fromDisplayName(String displayName) {
        for (IconCategory category : values()) {
            if (category.displayName.equals(displayName)) {
                return category;
            }
        }
        return null;
    }
}
