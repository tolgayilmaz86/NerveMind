package ai.nervemind.plugin.api;

import java.util.Collections;
import java.util.List;

/**
 * Result of node configuration validation.
 * 
 * @param isValid true if the configuration is valid
 * @param errors  list of error messages if invalid
 */
public record ValidationResult(boolean isValid, List<String> errors) {

    /**
     * Creates a valid validation result.
     * 
     * @return a successful validation result
     */
    public static ValidationResult valid() {
        return new ValidationResult(true, Collections.emptyList());
    }

    /**
     * Creates an invalid validation result with a single error.
     * 
     * @param error the error message
     * @return an invalid validation result with a single error
     */
    public static ValidationResult invalid(String error) {
        return new ValidationResult(false, List.of(error));
    }

    /**
     * Creates an invalid validation result with multiple errors.
     * 
     * @param errors the list of error messages
     * @return an invalid validation result with multiple errors
     */
    public static ValidationResult invalid(List<String> errors) {
        return new ValidationResult(false, errors);
    }
}
