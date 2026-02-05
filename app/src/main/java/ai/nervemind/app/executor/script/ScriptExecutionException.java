package ai.nervemind.app.executor.script;

/**
 * Exception thrown when script execution fails.
 * 
 * <p>
 * This exception wraps errors from various script engines (GraalJS, GraalPy,
 * external Python) with additional context about the failure.
 * </p>
 * /**
 * Exception thrown when script execution fails.
 */
public class ScriptExecutionException extends RuntimeException {

    /** The script language (e.g., "javascript", "python"). */
    private final String language;

    /** The script code that failed. */
    private final String code;

    /** The line number where the error occurred. */
    private final Integer lineNumber;

    /**
     * Create a new script execution exception.
     *
     * @param message  error message
     * @param language the script language (e.g., "javascript", "python")
     */
    public ScriptExecutionException(String message, String language) {
        super(message);
        this.language = language;
        this.code = null;
        this.lineNumber = null;
    }

    /**
     * Create a new script execution exception with cause.
     *
     * @param message  error message
     * @param cause    the underlying exception
     * @param language the script language
     */
    public ScriptExecutionException(String message, Throwable cause, String language) {
        super(message, cause);
        this.language = language;
        this.code = null;
        this.lineNumber = null;
    }

    /**
     * Create a new script execution exception with full context.
     *
     * @param message    error message
     * @param cause      the underlying exception
     * @param language   the script language
     * @param code       the script code that failed
     * @param lineNumber the line number where the error occurred (may be null)
     */
    public ScriptExecutionException(String message, Throwable cause,
            String language, String code, Integer lineNumber) {
        super(message, cause);
        this.language = language;
        this.code = code;
        this.lineNumber = lineNumber;
    }

    /**
     * Get the script language.
     *
     * @return language identifier
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Get the script code that failed.
     *
     * @return the code, or null if not available
     */
    public String getCode() {
        return code;
    }

    /**
     * Get the line number where the error occurred.
     *
     * @return line number, or null if not available
     */
    public Integer getLineNumber() {
        return lineNumber;
    }

    /**
     * Get a formatted error message with context.
     *
     * @return detailed error message
     */
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(language.toUpperCase()).append(" execution error: ").append(getMessage());

        if (lineNumber != null) {
            sb.append(" (line ").append(lineNumber).append(")");
        }

        if (getCause() != null && getCause().getMessage() != null
                && !getCause().getMessage().equals(getMessage())) {
            sb.append("\nCaused by: ").append(getCause().getMessage());
        }

        return sb.toString();
    }
}
