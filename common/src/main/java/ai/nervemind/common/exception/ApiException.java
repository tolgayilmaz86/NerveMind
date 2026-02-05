/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.common.exception;

/**
 * Exception thrown when an external API call fails.
 * This includes HTTP errors, timeout issues, and invalid responses
 * from external services like LLM providers, webhooks, etc.
 */
public class ApiException extends NerveMindException {

    private static final long serialVersionUID = 1L;

    /** The HTTP status code returned by the API, or -1 if not applicable. */
    private final int statusCode;
    /** The name of the API that failed, or null if not specified. */
    private final String apiName;

    /**
     * Create a new ApiException.
     *
     * @param message the exception message
     */
    public ApiException(String message) {
        super(message);
        this.statusCode = -1;
        this.apiName = null;
    }

    /**
     * Create a new ApiException with cause.
     *
     * @param message the exception message
     * @param cause   the underlying cause
     */
    public ApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.apiName = null;
    }

    /**
     * Create a new ApiException with API details.
     *
     * @param message    the exception message
     * @param apiName    the name of the API that failed
     * @param statusCode the HTTP status code returned
     */
    public ApiException(String message, String apiName, int statusCode) {
        super(message);
        this.apiName = apiName;
        this.statusCode = statusCode;
    }

    /**
     * Create a new ApiException with API details and cause.
     *
     * @param message    the exception message
     * @param apiName    the name of the API that failed
     * @param statusCode the HTTP status code returned
     * @param cause      the underlying cause
     */
    public ApiException(String message, String apiName, int statusCode, Throwable cause) {
        super(message, cause);
        this.apiName = apiName;
        this.statusCode = statusCode;
    }

    /**
     * Get the HTTP status code.
     *
     * @return the status code, or -1 if not applicable
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Get the name of the API that failed.
     *
     * @return the API name, or null if not specified
     */
    public String getApiName() {
        return apiName;
    }
}
