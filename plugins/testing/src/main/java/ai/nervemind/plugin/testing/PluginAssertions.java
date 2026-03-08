package ai.nervemind.plugin.testing;

import java.time.*;
import java.util.*;
import java.util.function.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Custom assertion helpers for plugin testing.
 * 
 * <p>
 * Provides fluent assertions for common plugin testing scenarios.
 * </p>
 * 
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * PluginAssertions.assertThat(result)
 *         .containsKey("message")
 *         .containsEntry("status", "success")
 *         .hasNumber("count")
 *         .isNotEmpty();
 * }</pre>
 */
public class PluginAssertions {

    /**
     * Create assertions for a map result.
     */
    public static MapAssert assertThat(Map<String, Object> result) {
        return new MapAssert(result);
    }

    /**
     * Create assertions for a string result.
     */
    public static StringAssert assertThat(String result) {
        return new StringAssert(result);
    }

    /**
     * Create assertions for a number result.
     */
    public static NumberAssert assertThat(Number result) {
        return new NumberAssert(result);
    }

    /**
     * Create assertions for an execution context.
     */
    public static ExecutionAssert assertThat(MockExecutionContext context) {
        return new ExecutionAssert(context);
    }

    /**
     * Assertions for map results.
     */
    public static class MapAssert {
        private final Map<String, Object> actual;

        public MapAssert(Map<String, Object> actual) {
            this.actual = actual;
        }

        public MapAssert containsKey(String key) {
            assertThat(actual).containsKey(key);
            return this;
        }

        public MapAssert doesNotContainKey(String key) {
            assertThat(actual).doesNotContainKey(key);
            return this;
        }

        public MapAssert containsEntry(String key, Object value) {
            assertThat(actual).containsEntry(key, value);
            return this;
        }

        public MapAssert doesNotContainEntry(String key, Object value) {
            assertThat(actual).doesNotContainEntry(key, value);
            return this;
        }

        public MapAssert hasSize(int size) {
            assertThat(actual).hasSize(size);
            return this;
        }

        public MapAssert isEmpty() {
            assertThat(actual).isEmpty();
            return this;
        }

        public MapAssert isNotEmpty() {
            assertThat(actual).isNotEmpty();
            return this;
        }

        public ValueAssert value(String key) {
            assertHasKey(key);
            return new ValueAssert(actual.get(key));
        }

        public StringAssert string(String key) {
            Object value = actual.get(key);
            assertThat(value).isInstanceOf(String.class);
            return new StringAssert((String) value);
        }

        public NumberAssert number(String key) {
            Object value = actual.get(key);
            assertThat(value).isInstanceOf(Number.class);
            return new NumberAssert((Number) value);
        }

        public BooleanAssert bool(String key) {
            Object value = actual.get(key);
            assertThat(value).isInstanceOf(Boolean.class);
            return new BooleanAssert((Boolean) value);
        }

        public MapAssert map(String key) {
            Object value = actual.get(key);
            assertThat(value).isInstanceOf(Map.class);
            return new MapAssert((Map<String, Object>) value);
        }

        public ListAssert list(String key) {
            Object value = actual.get(key);
            assertThat(value).isInstanceOf(List.class);
            return new ListAssert((List<?>) value);
        }

        private void assertHasKey(String key) {
            if (!actual.containsKey(key)) {
                throw new AssertionError("Expected key '" + key + "' in map. " +
                        "Available keys: " + actual.keySet());
            }
        }
    }

    /**
     * Assertions for string values.
     */
    public static class StringAssert {
        private final String actual;

        public StringAssert(String actual) {
            this.actual = actual;
        }

        public StringAssert isEqualTo(String expected) {
            assertThat(actual).isEqualTo(expected);
            return this;
        }

        public StringAssert isNotEqualTo(String expected) {
            assertThat(actual).isNotEqualTo(expected);
            return this;
        }

        public StringAssert contains(String substring) {
            assertThat(actual).contains(substring);
            return this;
        }

        public StringAssert doesNotContain(String substring) {
            assertThat(actual).doesNotContain(substring);
            return this;
        }

        public StringAssert startsWith(String prefix) {
            assertThat(actual).startsWith(prefix);
            return this;
        }

        public StringAssert endsWith(String suffix) {
            assertThat(actual).endsWith(suffix);
            return this;
        }

        public StringAssert matches(String regex) {
            assertThat(actual).matches(regex);
            return this;
        }

        public StringAssert isEmpty() {
            assertThat(actual).isEmpty();
            return this;
        }

        public StringAssert isNotEmpty() {
            assertThat(actual).isNotEmpty();
            return this;
        }

        public StringAssert isBlank() {
            assertThat(actual).isBlank();
            return this;
        }

        public StringAssert isNotBlank() {
            assertThat(actual).isNotBlank();
            return this;
        }

        public StringAssert hasSize(int size) {
            assertThat(actual).hasSize(size);
            return this;
        }
    }

    /**
     * Assertions for number values.
     */
    public static class NumberAssert {
        private final Number actual;

        public NumberAssert(Number actual) {
            this.actual = actual;
        }

        public NumberAssert isEqualTo(Number expected) {
            assertThat(actual).isEqualTo(expected);
            return this;
        }

        public NumberAssert isGreaterThan(Number expected) {
            assertThat(actual.doubleValue()).isGreaterThan(expected.doubleValue());
            return this;
        }

        public NumberAssert isLessThan(Number expected) {
            assertThat(actual.doubleValue()).isLessThan(expected.doubleValue());
            return this;
        }

        public NumberAssert isBetween(Number start, Number end) {
            double value = actual.doubleValue();
            assertThat(value).isBetween(start.doubleValue(), end.doubleValue());
            return this;
        }

        public NumberAssert isPositive() {
            assertThat(actual.longValue()).isPositive();
            return this;
        }

        public NumberAssert isNegative() {
            assertThat(actual.longValue()).isNegative();
            return this;
        }

        public NumberAssert isZero() {
            assertThat(actual.longValue()).isZero();
            return this;
        }

        public NumberAssert isInteger() {
            assertThat(actual.doubleValue() % 1).isEqualTo(0);
            return this;
        }
    }

    /**
     * Assertions for boolean values.
     */
    public static class BooleanAssert {
        private final Boolean actual;

        public BooleanAssert(Boolean actual) {
            this.actual = actual;
        }

        public BooleanAssert isTrue() {
            assertThat(actual).isTrue();
            return this;
        }

        public BooleanAssert isFalse() {
            assertThat(actual).isFalse();
            return this;
        }
    }

    /**
     * Assertions for list values.
     */
    public static class ListAssert {
        private final List<?> actual;

        public ListAssert(List<?> actual) {
            this.actual = actual;
        }

        public ListAssert hasSize(int size) {
            assertThat(actual).hasSize(size);
            return this;
        }

        public ListAssert isEmpty() {
            assertThat(actual).isEmpty();
            return this;
        }

        public ListAssert isNotEmpty() {
            assertThat(actual).isNotEmpty();
            return this;
        }

        public ListAssert contains(Object... elements) {
            assertThat(actual).contains(elements);
            return this;
        }

        public ListAssert containsExactly(Object... elements) {
            assertThat(actual).containsExactly(elements);
            return this;
        }

        public ListAssert doesNotContain(Object element) {
            assertThat(actual).doesNotContain(element);
            return this;
        }

        public ListAssert allMatch(Predicate<? super Object> predicate) {
            assertThat(actual).allMatch(predicate);
            return this;
        }

        public ListAssert anyMatch(Predicate<? super Object> predicate) {
            assertThat(actual).anyMatch(predicate);
            return this;
        }
    }

    /**
     * Assertions for any value.
     */
    public static class ValueAssert {
        private final Object actual;

        public ValueAssert(Object actual) {
            this.actual = actual;
        }

        public ValueAssert isNull() {
            assertThat(actual).isNull();
            return this;
        }

        public ValueAssert isNotNull() {
            assertThat(actual).isNotNull();
            return this;
        }

        public ValueAssert isInstanceOf(Class<?> type) {
            assertThat(actual).isInstanceOf(type);
            return this;
        }

        public ValueAssert isEqualTo(Object expected) {
            assertThat(actual).isEqualTo(expected);
            return this;
        }

        @SuppressWarnings("unchecked")
        public <T> T as(Class<T> type) {
            return (T) actual;
        }
    }

    /**
     * Assertions for execution context.
     */
    public static class ExecutionAssert {
        private final MockExecutionContext context;

        public ExecutionAssert(MockExecutionContext context) {
            this.context = context;
        }

        public ExecutionAssert hasOutput(String key) {
            assertThat(context.getOutputs()).containsKey(key);
            return this;
        }

        public ExecutionAssert outputEquals(String key, Object value) {
            assertThat(context.getOutputs()).containsEntry(key, value);
            return this;
        }

        public ExecutionAssert wasExecuted(int times) {
            assertThat(context.getExecutionCount()).isEqualTo(times);
            return this;
        }

        public ExecutionAssert wasNotCancelled() {
            assertThat(context.isCancelled()).isFalse();
            return this;
        }

        public ExecutionAssert wasCancelled() {
            assertThat(context.isCancelled()).isTrue();
            return this;
        }
    }
}
