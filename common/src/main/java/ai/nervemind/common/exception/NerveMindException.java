/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.common.exception;

/**
 * Base exception for all NerveMind exceptions.
 * Provides a common parent for all custom exceptions in the application.
 */
public class NerveMindException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Create a new NerveMindException with a message.
     *
     * @param message the exception message
     */
    public NerveMindException(String message) {
        super(message);
    }

    /**
     * Create a new NerveMindException with a message and cause.
     *
     * @param message the exception message
     * @param cause   the underlying cause
     */
    public NerveMindException(String message, Throwable cause) {
        super(message, cause);
    }
}
