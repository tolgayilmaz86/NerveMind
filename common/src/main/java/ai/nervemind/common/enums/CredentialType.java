package ai.nervemind.common.enums;

/**
 * Types of credentials supported by the application.
 */
public enum CredentialType {
    /** Simple API key authentication. */
    API_KEY("API Key", "Simple API key authentication"),
    /** Username and password authentication. */
    HTTP_BASIC("HTTP Basic", "Username and password"),
    /** Bearer token authentication. */
    HTTP_BEARER("Bearer Token", "Bearer token authentication"),
    /** OAuth 2.0 authentication flow. */
    OAUTH2("OAuth 2.0", "OAuth 2.0 authentication flow"),
    /** Custom HTTP header authentication. */
    CUSTOM_HEADER("Custom Header", "Custom HTTP header authentication"),
    /** Database connection credentials. */
    DATABASE("Database", "Database connection credentials"),
    /** SMTP email credentials. */
    EMAIL_SMTP("Email (SMTP)", "SMTP server credentials for sending email");

    private final String displayName;
    private final String description;

    /**
     * @param displayName the human-readable name
     * @param description the description of use
     */
    CredentialType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Gets the display name.
     * 
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the description.
     * 
     * @return the description
     */
    public String getDescription() {
        return description;
    }
}
