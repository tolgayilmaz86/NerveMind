/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.common.exception;

/**
 * Exception thrown when data serialization or parsing fails.
 * This includes JSON parsing errors, invalid data formats,
 * and serialization issues.
 */
public class DataParsingException extends NerveMindException {

    private static final long serialVersionUID = 1L;

    /** The type of data that failed to parse, or null if not specified. */
    private final String dataType;

    /**
     * Create a new DataParsingException.
     *
     * @param message the exception message
     */
    public DataParsingException(String message) {
        super(message);
        this.dataType = null;
    }

    /**
     * Create a new DataParsingException with cause.
     *
     * @param message the exception message
     * @param cause   the underlying cause
     */
    public DataParsingException(String message, Throwable cause) {
        super(message, cause);
        this.dataType = null;
    }

    /**
     * Create a new DataParsingException with data type info.
     *
     * @param message  the exception message
     * @param dataType the type of data being parsed (e.g., "nodes", "connections",
     *                 "JSON")
     */
    public DataParsingException(String message, String dataType) {
        super(message);
        this.dataType = dataType;
    }

    /**
     * Create a new DataParsingException with data type info and cause.
     *
     * @param message  the exception message
     * @param dataType the type of data being parsed
     * @param cause    the underlying cause
     */
    public DataParsingException(String message, String dataType, Throwable cause) {
        super(message, cause);
        this.dataType = dataType;
    }

    /**
     * Get the type of data that failed to parse.
     *
     * @return the data type, or null if not specified
     */
    public String getDataType() {
        return dataType;
    }
}
