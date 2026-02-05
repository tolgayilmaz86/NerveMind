package ai.nervemind.ui.util;

import java.util.function.Function;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.util.StringConverter;

/**
 * Utility class for common JavaFX binding patterns.
 * 
 * <p>
 * Provides helper methods for:
 * <ul>
 * <li>Conditional bindings</li>
 * <li>String formatting bindings</li>
 * <li>List-based bindings</li>
 * <li>Bidirectional binding with conversion</li>
 * </ul>
 */
public final class BindingUtils {

    private BindingUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Creates a binding that returns one value when a condition is true, another
     * when false.
     * 
     * @param <T>        the type of the result
     * @param condition  the boolean property to check
     * @param trueValue  the value when condition is true
     * @param falseValue the value when condition is false
     * @return a binding that switches based on the condition
     */
    public static <T> ObservableValue<T> ifThenElse(
            ObservableValue<Boolean> condition,
            T trueValue,
            T falseValue) {
        return Bindings.when(BooleanExpression.booleanExpression(condition))
                .then(trueValue)
                .otherwise(falseValue);
    }

    /**
     * Creates a string binding that formats a value using a pattern.
     * 
     * @param pattern the format pattern (uses String.format)
     * @param values  the observable values to format
     * @return a string binding with the formatted result
     */
    public static StringBinding format(String pattern, ObservableValue<?>... values) {
        return Bindings.createStringBinding(
                () -> {
                    Object[] args = new Object[values.length];
                    for (int i = 0; i < values.length; i++) {
                        args[i] = values[i].getValue();
                    }
                    return String.format(pattern, args);
                },
                values);
    }

    /**
     * Creates a binding that checks if a string property is not empty.
     * 
     * @param property the string property to check
     * @return a boolean binding that is true when the string is not null/empty
     */
    public static BooleanBinding isNotEmpty(StringProperty property) {
        return Bindings.createBooleanBinding(
                () -> property.get() != null && !property.get().isBlank(),
                property);
    }

    /**
     * Creates a binding that checks if a string property is empty.
     * 
     * @param property the string property to check
     * @return a boolean binding that is true when the string is null/empty
     */
    public static BooleanBinding isEmpty(StringProperty property) {
        return isNotEmpty(property).not();
    }

    /**
     * Creates a binding that checks if a list is not empty.
     * 
     * @param <T>  the type of list items
     * @param list the observable list
     * @return a boolean binding that is true when the list has items
     */
    public static <T> BooleanBinding isNotEmpty(ObservableList<T> list) {
        return Bindings.isNotEmpty(list);
    }

    /**
     * Creates a binding for the size of a list formatted as a string.
     * 
     * @param <T>      the type of list items
     * @param list     the observable list
     * @param singular the singular label (e.g., "item")
     * @param plural   the plural label (e.g., "items")
     * @return a string binding like "5 items" or "1 item"
     */
    public static <T> StringBinding sizeLabel(ObservableList<T> list, String singular, String plural) {
        return Bindings.createStringBinding(
                () -> {
                    int size = list.size();
                    return size + " " + (size == 1 ? singular : plural);
                },
                list);
    }

    /**
     * Binds a property bidirectionally with value conversion.
     * 
     * @param <S>      the source type
     * @param <T>      the target type
     * @param source   the source property
     * @param target   the target property
     * @param toTarget converter from source to target
     * @param toSource converter from target to source
     */
    public static <S, T> void bindBidirectional(
            Property<S> source,
            Property<T> target,
            Function<S, T> toTarget,
            Function<T, S> toSource) {

        target.setValue(toTarget.apply(source.getValue()));

        source.addListener((obs, oldVal, newVal) -> {
            T converted = toTarget.apply(newVal);
            if (!converted.equals(target.getValue())) {
                target.setValue(converted);
            }
        });

        target.addListener((obs, oldVal, newVal) -> {
            S converted = toSource.apply(newVal);
            if (!converted.equals(source.getValue())) {
                source.setValue(converted);
            }
        });
    }

    /**
     * Creates a StringConverter for use with bidirectional bindings.
     * 
     * @param <T>        the type to convert
     * @param toString   converter to string
     * @param fromString converter from string
     * @return a StringConverter instance
     */
    public static <T> StringConverter<T> stringConverter(
            Function<T, String> toString,
            Function<String, T> fromString) {
        return new StringConverter<>() {
            @Override
            public String toString(T object) {
                return object == null ? "" : toString.apply(object);
            }

            @Override
            public T fromString(String string) {
                return string == null || string.isBlank() ? null : fromString.apply(string);
            }
        };
    }

    /**
     * Creates a binding that combines multiple boolean properties with AND.
     * 
     * @param properties the boolean properties to combine
     * @return a binding that is true only when all properties are true
     */
    public static BooleanBinding allTrue(BooleanProperty... properties) {
        return Bindings.createBooleanBinding(
                () -> {
                    for (BooleanProperty prop : properties) {
                        if (!prop.get()) {
                            return false;
                        }
                    }
                    return true;
                },
                properties);
    }

    /**
     * Creates a binding that combines multiple boolean properties with OR.
     * 
     * @param properties the boolean properties to combine
     * @return a binding that is true when any property is true
     */
    public static BooleanBinding anyTrue(BooleanProperty... properties) {
        return Bindings.createBooleanBinding(
                () -> {
                    for (BooleanProperty prop : properties) {
                        if (prop.get()) {
                            return true;
                        }
                    }
                    return false;
                },
                properties);
    }

    /**
     * Creates a binding that negates a boolean property.
     * 
     * @param property the property to negate
     * @return a binding that is the opposite of the property
     */
    public static BooleanBinding not(BooleanProperty property) {
        return property.not();
    }
}
