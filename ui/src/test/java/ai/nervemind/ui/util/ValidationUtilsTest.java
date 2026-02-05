/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import ai.nervemind.ui.util.ValidationUtils.ValidationResult;
import ai.nervemind.ui.util.ValidationUtils.Validator;

/**
 * Unit tests for ValidationUtils.
 * 
 * <p>
 * Tests validation rules used across ViewModels.
 */
class ValidationUtilsTest {

    @Nested
    @DisplayName("isRequired - String")
    class IsRequiredString {

        @Test
        @DisplayName("Should return true for valid string")
        void shouldReturnTrueForValidString() {
            assertTrue(ValidationUtils.isRequired("test"));
            assertTrue(ValidationUtils.isRequired("a"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { "  ", "\t", "\n", "   \n\t  " })
        @DisplayName("Should return false for null, empty, or blank strings")
        void shouldReturnFalseForInvalidStrings(String value) {
            assertFalse(ValidationUtils.isRequired(value));
        }
    }

    @Nested
    @DisplayName("Email Validation")
    class EmailValidation {

        @Test
        @DisplayName("Should validate correct email formats")
        void shouldValidateCorrectEmailFormats() {
            assertTrue(ValidationUtils.isValidEmail("test@example.com"));
            assertTrue(ValidationUtils.isValidEmail("user.name@domain.org"));
            assertTrue(ValidationUtils.isValidEmail("user+tag@example.co.uk"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { "notanemail", "missing@", "@nodomain.com", "spaces in@email.com" })
        @DisplayName("Should reject invalid email formats")
        void shouldRejectInvalidEmailFormats(String email) {
            assertFalse(ValidationUtils.isValidEmail(email));
        }
    }

    @Nested
    @DisplayName("URL Validation")
    class UrlValidation {

        @Test
        @DisplayName("Should validate correct URL formats")
        void shouldValidateCorrectUrlFormats() {
            assertTrue(ValidationUtils.isValidUrl("http://example.com"));
            assertTrue(ValidationUtils.isValidUrl("https://example.com/path"));
            assertTrue(ValidationUtils.isValidUrl("https://example.com:8080/path?query=1"));
            assertTrue(ValidationUtils.isValidUrl("ftp://files.example.com"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { "notaurl", "example.com", "htp://wrong.com", "file:///local" })
        @DisplayName("Should reject invalid URL formats")
        void shouldRejectInvalidUrlFormats(String url) {
            assertFalse(ValidationUtils.isValidUrl(url));
        }
    }

    @Nested
    @DisplayName("Variable Name Validation")
    class VariableNameValidation {

        @Test
        @DisplayName("Should validate correct variable names")
        void shouldValidateCorrectVariableNames() {
            assertTrue(ValidationUtils.isValidVariableName("validName"));
            assertTrue(ValidationUtils.isValidVariableName("_private"));
            assertTrue(ValidationUtils.isValidVariableName("name123"));
            assertTrue(ValidationUtils.isValidVariableName("CONSTANT_VALUE"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { "123start", "has-dash", "has space", "special@char" })
        @DisplayName("Should reject invalid variable names")
        void shouldRejectInvalidVariableNames(String name) {
            assertFalse(ValidationUtils.isValidVariableName(name));
        }
    }

    @Nested
    @DisplayName("Integer Validation")
    class IntegerValidation {

        @Test
        @DisplayName("Should validate valid integers")
        void shouldValidateValidIntegers() {
            assertTrue(ValidationUtils.isValidInteger("123"));
            assertTrue(ValidationUtils.isValidInteger("-456"));
            assertTrue(ValidationUtils.isValidInteger("0"));
            assertTrue(ValidationUtils.isValidInteger("  42  ")); // Trimmed
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { "12.34", "abc", "12abc", "1.0" })
        @DisplayName("Should reject invalid integers")
        void shouldRejectInvalidIntegers(String value) {
            assertFalse(ValidationUtils.isValidInteger(value));
        }
    }

    @Nested
    @DisplayName("Double Validation")
    class DoubleValidation {

        @Test
        @DisplayName("Should validate valid doubles")
        void shouldValidateValidDoubles() {
            assertTrue(ValidationUtils.isValidDouble("123"));
            assertTrue(ValidationUtils.isValidDouble("12.34"));
            assertTrue(ValidationUtils.isValidDouble("-45.67"));
            assertTrue(ValidationUtils.isValidDouble("0.0"));
            assertTrue(ValidationUtils.isValidDouble("  3.14  ")); // Trimmed
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { "abc", "12.34.56", "1,234" })
        @DisplayName("Should reject invalid doubles")
        void shouldRejectInvalidDoubles(String value) {
            assertFalse(ValidationUtils.isValidDouble(value));
        }
    }

    @Nested
    @DisplayName("Range Validation - Integer")
    class RangeValidationInteger {

        @Test
        @DisplayName("Should return true for values in range")
        void shouldReturnTrueForValuesInRange() {
            assertTrue(ValidationUtils.isInRange(5, 1, 10));
            assertTrue(ValidationUtils.isInRange(1, 1, 10)); // Lower bound
            assertTrue(ValidationUtils.isInRange(10, 1, 10)); // Upper bound
        }

        @Test
        @DisplayName("Should return false for values outside range")
        void shouldReturnFalseForValuesOutsideRange() {
            assertFalse(ValidationUtils.isInRange(0, 1, 10));
            assertFalse(ValidationUtils.isInRange(11, 1, 10));
            assertFalse(ValidationUtils.isInRange(-5, 1, 10));
        }
    }

    @Nested
    @DisplayName("Range Validation - Double")
    class RangeValidationDouble {

        @Test
        @DisplayName("Should return true for values in range")
        void shouldReturnTrueForValuesInRange() {
            assertTrue(ValidationUtils.isInRange(5.5, 1.0, 10.0));
            assertTrue(ValidationUtils.isInRange(1.0, 1.0, 10.0));
            assertTrue(ValidationUtils.isInRange(10.0, 1.0, 10.0));
        }

        @Test
        @DisplayName("Should return false for values outside range")
        void shouldReturnFalseForValuesOutsideRange() {
            assertFalse(ValidationUtils.isInRange(0.5, 1.0, 10.0));
            assertFalse(ValidationUtils.isInRange(10.5, 1.0, 10.0));
        }
    }

    @Nested
    @DisplayName("Length Validation")
    class LengthValidation {

        @Test
        @DisplayName("Should validate strings within length bounds")
        void shouldValidateStringsWithinLengthBounds() {
            assertTrue(ValidationUtils.hasLengthBetween("test", 1, 10));
            assertTrue(ValidationUtils.hasLengthBetween("ab", 2, 2)); // Exact length
            assertTrue(ValidationUtils.hasLengthBetween("", 0, 5)); // Empty allowed
        }

        @Test
        @DisplayName("Should reject strings outside length bounds")
        void shouldRejectStringsOutsideLengthBounds() {
            assertFalse(ValidationUtils.hasLengthBetween("a", 2, 10)); // Too short
            assertFalse(ValidationUtils.hasLengthBetween("this is too long", 1, 5)); // Too long
        }

        @Test
        @DisplayName("Should handle null values")
        void shouldHandleNullValues() {
            assertTrue(ValidationUtils.hasLengthBetween(null, 0, 10));
            assertFalse(ValidationUtils.hasLengthBetween(null, 1, 10));
        }
    }

    @Nested
    @DisplayName("Pattern Matching")
    class PatternMatching {

        @Test
        @DisplayName("Should match valid patterns with Pattern object")
        void shouldMatchValidPatternsWithPatternObject() {
            Pattern hexPattern = Pattern.compile("^#[0-9A-Fa-f]{6}$");
            assertTrue(ValidationUtils.matchesPattern("#FF00FF", hexPattern));
            assertFalse(ValidationUtils.matchesPattern("notahex", hexPattern));
        }

        @Test
        @DisplayName("Should match valid patterns with regex string")
        void shouldMatchValidPatternsWithRegexString() {
            assertTrue(ValidationUtils.matchesPattern("ABC123", "^[A-Z]{3}\\d{3}$"));
            assertFalse(ValidationUtils.matchesPattern("abc123", "^[A-Z]{3}\\d{3}$"));
        }

        @Test
        @DisplayName("Should return false for null value")
        void shouldReturnFalseForNullValue() {
            assertFalse(ValidationUtils.matchesPattern(null, Pattern.compile(".*")));
        }
    }

    @Nested
    @DisplayName("ValidationResult")
    class ValidationResultTests {

        @Test
        @DisplayName("Should create valid result")
        void shouldCreateValidResult() {
            ValidationResult result = ValidationResult.valid();

            assertTrue(result.isValid());
            assertNull(result.errorMessage());
        }

        @Test
        @DisplayName("Should create invalid result with message")
        void shouldCreateInvalidResultWithMessage() {
            ValidationResult result = ValidationResult.invalid("Error occurred");

            assertFalse(result.isValid());
            assertEquals("Error occurred", result.errorMessage());
        }

        @Test
        @DisplayName("Should throw exception when invalid")
        void shouldThrowExceptionWhenInvalid() {
            ValidationResult result = ValidationResult.invalid("Test error");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, result::throwIfInvalid);
            assertEquals("Test error", ex.getMessage());
        }

        @Test
        @DisplayName("Should not throw exception when valid")
        void shouldNotThrowExceptionWhenValid() {
            ValidationResult result = ValidationResult.valid();

            assertDoesNotThrow(result::throwIfInvalid);
        }
    }

    @Nested
    @DisplayName("validate Helper")
    class ValidateHelper {

        @Test
        @DisplayName("Should return valid result when condition is true")
        void shouldReturnValidResultWhenConditionIsTrue() {
            ValidationResult result = ValidationUtils.validate(true, "Error");

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("Should return invalid result when condition is false")
        void shouldReturnInvalidResultWhenConditionIsFalse() {
            ValidationResult result = ValidationUtils.validate(false, "Error message");

            assertFalse(result.isValid());
            assertEquals("Error message", result.errorMessage());
        }
    }

    @Nested
    @DisplayName("Combining Validations")
    class CombiningValidations {

        @Test
        @DisplayName("Should return valid when all validations pass")
        void shouldReturnValidWhenAllValidationsPass() {
            ValidationResult result = ValidationUtils.combine(
                    ValidationResult.valid(),
                    ValidationResult.valid(),
                    ValidationResult.valid());

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("Should return first error when any validation fails")
        void shouldReturnFirstErrorWhenAnyValidationFails() {
            ValidationResult result = ValidationUtils.combine(
                    ValidationResult.valid(),
                    ValidationResult.invalid("First error"),
                    ValidationResult.invalid("Second error"));

            assertFalse(result.isValid());
            assertEquals("First error", result.errorMessage());
        }

        @Test
        @DisplayName("Should combine list of validations")
        void shouldCombineListOfValidations() {
            List<ValidationResult> results = List.of(
                    ValidationResult.valid(),
                    ValidationResult.invalid("List error"));

            ValidationResult result = ValidationUtils.combine(results);

            assertFalse(result.isValid());
            assertEquals("List error", result.errorMessage());
        }
    }

    @Nested
    @DisplayName("Validator Interface")
    class ValidatorInterfaceTests {

        @Test
        @DisplayName("Should create validator from predicate")
        void shouldCreateValidatorFromPredicate() {
            Validator<String> notEmpty = ValidationUtils.validator(
                    s -> s != null && !s.isEmpty(),
                    "Value cannot be empty");

            assertTrue(notEmpty.validate("test").isValid());
            assertFalse(notEmpty.validate("").isValid());
            assertEquals("Value cannot be empty", notEmpty.validate("").errorMessage());
        }

        @Test
        @DisplayName("Should chain validators with and")
        void shouldChainValidatorsWithAnd() {
            Validator<String> notEmpty = ValidationUtils.validator(
                    s -> s != null && !s.isEmpty(),
                    "Cannot be empty");
            Validator<String> minLength = ValidationUtils.validator(
                    s -> s.length() >= 3,
                    "Must be at least 3 characters");

            Validator<String> combined = notEmpty.and(minLength);

            assertTrue(combined.validate("test").isValid());
            assertFalse(combined.validate("").isValid());
            assertEquals("Cannot be empty", combined.validate("").errorMessage());
            assertFalse(combined.validate("ab").isValid());
            assertEquals("Must be at least 3 characters", combined.validate("ab").errorMessage());
        }
    }
}
