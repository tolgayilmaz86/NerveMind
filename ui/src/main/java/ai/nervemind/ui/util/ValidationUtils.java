package ai.nervemind.ui.util;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import javafx.beans.property.StringProperty;

/**
 * Utility class for common UI validation patterns.
 * 
 * <p>
 * Provides validators for:
 * <ul>
 * <li>Required fields</li>
 * <li>String patterns (email, URL, etc.)</li>
 * <li>Numeric ranges</li>
 * <li>Custom predicates</li>
 * </ul>
 */
public final class ValidationUtils {

    // Common regex patterns - possessive quantifiers used to prevent backtracking
    // issues
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[\\w+&*-]++(?:\\.[\\w+&*-]++)*+@(?>[\\w-]++\\.)+[a-zA-Z]{2,7}$");

    private static final Pattern URL_PATTERN = Pattern.compile(
            "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$", Pattern.CASE_INSENSITIVE);

    private static final Pattern VARIABLE_NAME_PATTERN = Pattern.compile(
            "^[a-zA-Z_]\\w*$");

    private ValidationUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Validates that a string is not null or blank.
     * 
     * @param value the value to check
     * @return true if the value is not null/blank
     */
    public static boolean isRequired(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Validates that a StringProperty is not null or blank.
     * 
     * @param property the property to check
     * @return true if the property value is not null/blank
     */
    public static boolean isRequired(StringProperty property) {
        return property != null && isRequired(property.get());
    }

    /**
     * Validates that a string matches the email pattern.
     * 
     * @param value the value to check
     * @return true if the value is a valid email format
     */
    public static boolean isValidEmail(String value) {
        return value != null && EMAIL_PATTERN.matcher(value).matches();
    }

    /**
     * Validates that a string matches the URL pattern.
     * 
     * @param value the value to check
     * @return true if the value is a valid URL format
     */
    public static boolean isValidUrl(String value) {
        return value != null && URL_PATTERN.matcher(value).matches();
    }

    /**
     * Validates that a string is a valid variable name.
     * Must start with letter or underscore, contain only alphanumeric and
     * underscore.
     * 
     * @param value the value to check
     * @return true if the value is a valid variable name
     */
    public static boolean isValidVariableName(String value) {
        return value != null && VARIABLE_NAME_PATTERN.matcher(value).matches();
    }

    /**
     * Validates that a string can be parsed as an integer.
     * 
     * @param value the value to check
     * @return true if the value is a valid integer
     */
    public static boolean isValidInteger(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            Integer.parseInt(value.trim());
            return true;
        } catch (NumberFormatException _) {
            return false;
        }
    }

    /**
     * Validates that a string can be parsed as a double.
     * 
     * @param value the value to check
     * @return true if the value is a valid double
     */
    public static boolean isValidDouble(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            Double.parseDouble(value.trim());
            return true;
        } catch (NumberFormatException _) {
            return false;
        }
    }

    /**
     * Validates that an integer value is within a range.
     * 
     * @param value the value to check
     * @param min   the minimum value (inclusive)
     * @param max   the maximum value (inclusive)
     * @return true if the value is within range
     */
    public static boolean isInRange(int value, int min, int max) {
        return value >= min && value <= max;
    }

    /**
     * Validates that a double value is within a range.
     * 
     * @param value the value to check
     * @param min   the minimum value (inclusive)
     * @param max   the maximum value (inclusive)
     * @return true if the value is within range
     */
    public static boolean isInRange(double value, double min, double max) {
        return value >= min && value <= max;
    }

    /**
     * Validates that a string length is within a range.
     * 
     * @param value     the value to check
     * @param minLength the minimum length (inclusive)
     * @param maxLength the maximum length (inclusive)
     * @return true if the length is within range
     */
    public static boolean hasLengthBetween(String value, int minLength, int maxLength) {
        if (value == null) {
            return minLength <= 0;
        }
        int length = value.length();
        return length >= minLength && length <= maxLength;
    }

    /**
     * Validates that a string matches a custom pattern.
     * 
     * @param value   the value to check
     * @param pattern the regex pattern
     * @return true if the value matches the pattern
     */
    public static boolean matchesPattern(String value, Pattern pattern) {
        return value != null && pattern.matcher(value).matches();
    }

    /**
     * Validates that a string matches a custom pattern.
     * 
     * @param value the value to check
     * @param regex the regex pattern string
     * @return true if the value matches the pattern
     */
    public static boolean matchesPattern(String value, String regex) {
        return matchesPattern(value, Pattern.compile(regex));
    }

    /**
     * Creates a validation result with an error message.
     * 
     * @param valid        whether the validation passed
     * @param errorMessage the error message if validation failed
     * @return the validation result
     */
    public static ValidationResult validate(boolean valid, String errorMessage) {
        return valid ? ValidationResult.valid() : ValidationResult.invalid(errorMessage);
    }

    /**
     * Combines multiple validation results.
     * Returns the first error found, or valid if all pass.
     * 
     * @param results the validation results to combine
     * @return the combined result
     */
    public static ValidationResult combine(ValidationResult... results) {
        for (ValidationResult result : results) {
            if (!result.isValid()) {
                return result;
            }
        }
        return ValidationResult.valid();
    }

    /**
     * Combines multiple validation results from a list.
     * 
     * @param results the validation results to combine
     * @return the combined result
     */
    public static ValidationResult combine(List<ValidationResult> results) {
        return combine(results.toArray(ValidationResult[]::new));
    }

    /**
     * Creates a validator function from a predicate and error message.
     * 
     * @param predicate    the validation predicate
     * @param errorMessage the error message if validation fails
     * @return a function that returns a ValidationResult
     */
    public static Validator<String> validator(Predicate<String> predicate, String errorMessage) {
        return value -> predicate.test(value)
                ? ValidationResult.valid()
                : ValidationResult.invalid(errorMessage);
    }

    /**
     * Functional interface for validators.
     * 
     * @param <T> the type being validated
     */
    @FunctionalInterface
    public interface Validator<T> {
        ValidationResult validate(T value);

        /**
         * Chains validators - runs this validator, then the other if this passes.
         */
        default Validator<T> and(Validator<T> other) {
            return value -> {
                ValidationResult result = this.validate(value);
                return result.isValid() ? other.validate(value) : result;
            };
        }
    }

    /**
     * Result of a validation check.
     *
     * @param isValid      whether the validation passed
     * @param errorMessage the error message if validation failed, or null if passed
     */
    public record ValidationResult(boolean isValid, String errorMessage) {

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        /**
         * Throws an IllegalArgumentException if invalid.
         */
        public void throwIfInvalid() {
            if (!isValid) {
                throw new IllegalArgumentException(errorMessage);
            }
        }
    }
}
