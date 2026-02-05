/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.common.exception;

/**
 * Exception thrown when UI component initialization fails.
 * This includes FXML loading errors, resource loading failures,
 * and component setup issues.
 */
public class UiInitializationException extends NerveMindException {

    private static final long serialVersionUID = 1L;

    /**
     * The name of the UI component that failed to initialize, or null if not
     * specified.
     */
    private final String componentName;

    /**
     * Create a new UiInitializationException.
     *
     * @param message the exception message
     */
    public UiInitializationException(String message) {
        super(message);
        this.componentName = null;
    }

    /**
     * Create a new UiInitializationException with cause.
     *
     * @param message the exception message
     * @param cause   the underlying cause
     */
    public UiInitializationException(String message, Throwable cause) {
        super(message, cause);
        this.componentName = null;
    }

    /**
     * Create a new UiInitializationException with component info.
     *
     * @param message       the exception message
     * @param componentName the name of the component that failed to initialize
     */
    public UiInitializationException(String message, String componentName) {
        super(message);
        this.componentName = componentName;
    }

    /**
     * Create a new UiInitializationException with component info and cause.
     *
     * @param message       the exception message
     * @param componentName the name of the component that failed to initialize
     * @param cause         the underlying cause
     */
    public UiInitializationException(String message, String componentName, Throwable cause) {
        super(message, cause);
        this.componentName = componentName;
    }

    /**
     * Get the name of the component that failed to initialize.
     *
     * @return the component name, or null if not specified
     */
    public String getComponentName() {
        return componentName;
    }
}
