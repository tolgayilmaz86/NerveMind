package ai.nervemind.common.enums;

/**
 * HTTP methods supported for HTTP requests and webhooks.
 * 
 * <p>
 * This enum provides type-safe HTTP method constants, replacing
 * hardcoded string values throughout the codebase.
 * </p>
 */
public enum HttpMethod {
    /** HTTP GET - Retrieve data */
    GET("GET", "Retrieve data from a resource"),

    /** HTTP POST - Create new data */
    POST("POST", "Create new data on a resource"),

    /** HTTP PUT - Update/replace data */
    PUT("PUT", "Update or replace data on a resource"),

    /** HTTP DELETE - Remove data */
    DELETE("DELETE", "Remove data from a resource"),

    /** HTTP PATCH - Partial update */
    PATCH("PATCH", "Partially update data on a resource"),

    /** HTTP HEAD - Headers only */
    HEAD("HEAD", "Retrieve headers only"),

    /** HTTP OPTIONS - Available methods */
    OPTIONS("OPTIONS", "Retrieve available methods for a resource");

    private final String value;
    private final String description;

    HttpMethod(String value, String description) {
        this.value = value;
        this.description = description;
    }

    /**
     * Get the HTTP method string value.
     * 
     * @return the method name (e.g., "GET", "POST")
     */
    public String getValue() {
        return value;
    }

    /**
     * Get the description of this HTTP method.
     * 
     * @return human-readable description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Parse an HTTP method from its string representation.
     * 
     * @param value the string value (case-insensitive)
     * @return the corresponding HttpMethod, or GET as default
     */
    public static HttpMethod fromString(String value) {
        if (value == null) {
            return GET;
        }
        return switch (value.toUpperCase()) {
            case "POST" -> POST;
            case "PUT" -> PUT;
            case "DELETE" -> DELETE;
            case "PATCH" -> PATCH;
            case "HEAD" -> HEAD;
            case "OPTIONS" -> OPTIONS;
            default -> GET;
        };
    }

    /**
     * Get all common HTTP methods for UI dropdowns.
     * 
     * @return array of common HTTP methods
     */
    public static HttpMethod[] commonMethods() {
        return new HttpMethod[] { GET, POST, PUT, DELETE, PATCH };
    }

    /**
     * Get all webhook-compatible HTTP methods.
     * 
     * @return array of webhook HTTP methods
     */
    public static HttpMethod[] webhookMethods() {
        return new HttpMethod[] { GET, POST, PUT, DELETE };
    }

    /**
     * Get HTTP method values as a string array for UI dropdowns.
     * 
     * @param methods the methods to convert
     * @return array of method value strings
     */
    public static String[] getValuesArray(HttpMethod[] methods) {
        String[] values = new String[methods.length];
        for (int i = 0; i < methods.length; i++) {
            values[i] = methods[i].getValue();
        }
        return values;
    }

    @Override
    public String toString() {
        return value;
    }
}
