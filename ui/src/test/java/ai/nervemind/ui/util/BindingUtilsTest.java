/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.StringConverter;

@DisplayName("BindingUtils")
class BindingUtilsTest {

    @Test
    @DisplayName("ifThenElse should switch value based on condition")
    void ifThenElseShouldSwitchValueBasedOnCondition() {
        BooleanProperty condition = new SimpleBooleanProperty(false);

        var value = BindingUtils.ifThenElse(condition, "yes", "no");
        assertThat(value.getValue()).isEqualTo("no");

        condition.set(true);
        assertThat(value.getValue()).isEqualTo("yes");
    }

    @Test
    @DisplayName("format should update when observed values change")
    void formatShouldUpdateWhenObservedValuesChange() {
        SimpleStringProperty name = new SimpleStringProperty("Alice");
        SimpleIntegerProperty count = new SimpleIntegerProperty(2);

        StringBinding binding = BindingUtils.format("%s (%d)", name, count.asObject());
        assertThat(binding.get()).isEqualTo("Alice (2)");

        name.set("Bob");
        count.set(7);
        assertThat(binding.get()).isEqualTo("Bob (7)");
    }

    @Test
    @DisplayName("isNotEmpty and isEmpty should react to blank values")
    void isNotEmptyAndIsEmptyShouldReactToBlankValues() {
        StringProperty text = new SimpleStringProperty(null);

        BooleanBinding notEmpty = BindingUtils.isNotEmpty(text);
        BooleanBinding empty = BindingUtils.isEmpty(text);

        assertThat(notEmpty.get()).isFalse();
        assertThat(empty.get()).isTrue();

        text.set("  ");
        assertThat(notEmpty.get()).isFalse();
        assertThat(empty.get()).isTrue();

        text.set("value");
        assertThat(notEmpty.get()).isTrue();
        assertThat(empty.get()).isFalse();
    }

    @Test
    @DisplayName("list helpers should reflect list content and size labels")
    void listHelpersShouldReflectListContentAndSizeLabels() {
        ObservableList<String> list = FXCollections.observableArrayList();

        BooleanBinding notEmpty = BindingUtils.isNotEmpty(list);
        StringBinding size = BindingUtils.sizeLabel(list, "item", "items");

        assertThat(notEmpty.get()).isFalse();
        assertThat(size.get()).isEqualTo("0 items");

        list.add("a");
        assertThat(notEmpty.get()).isTrue();
        assertThat(size.get()).isEqualTo("1 item");

        list.add("b");
        assertThat(size.get()).isEqualTo("2 items");
    }

    @Test
    @DisplayName("bindBidirectional should sync source and target with conversion")
    void bindBidirectionalShouldSyncSourceAndTargetWithConversion() {
        SimpleObjectProperty<Integer> source = new SimpleObjectProperty<>(3);
        SimpleObjectProperty<String> target = new SimpleObjectProperty<>("ignored");

        BindingUtils.bindBidirectional(source, target, i -> "v" + i, s -> Integer.parseInt(s.substring(1)));

        assertThat(target.get()).isEqualTo("v3");

        source.set(9);
        assertThat(target.get()).isEqualTo("v9");

        target.set("v12");
        assertThat(source.get()).isEqualTo(12);
    }

    @Test
    @DisplayName("stringConverter should handle null/blank and valid values")
    void stringConverterShouldHandleNullBlankAndValidValues() {
        StringConverter<Integer> converter = BindingUtils.stringConverter(Object::toString, Integer::parseInt);

        assertThat(converter.toString(null)).isEqualTo("");
        assertThat(converter.toString(42)).isEqualTo("42");
        assertThat(converter.fromString(null)).isNull();
        assertThat(converter.fromString("   ")).isNull();
        assertThat(converter.fromString("17")).isEqualTo(17);
    }

    @Test
    @DisplayName("allTrue and anyTrue should evaluate aggregate boolean state")
    void allTrueAndAnyTrueShouldEvaluateAggregateBooleanState() {
        BooleanProperty a = new SimpleBooleanProperty(false);
        BooleanProperty b = new SimpleBooleanProperty(true);
        BooleanProperty c = new SimpleBooleanProperty(false);

        BooleanBinding all = BindingUtils.allTrue(a, b, c);
        BooleanBinding any = BindingUtils.anyTrue(a, b, c);

        assertThat(all.get()).isFalse();
        assertThat(any.get()).isTrue();

        a.set(true);
        c.set(true);
        assertThat(all.get()).isTrue();
        assertThat(any.get()).isTrue();
    }

    @Test
    @DisplayName("not should invert boolean property")
    void notShouldInvertBooleanProperty() {
        BooleanProperty value = new SimpleBooleanProperty(false);
        BooleanBinding inverted = BindingUtils.not(value);

        assertThat(inverted.get()).isTrue();

        value.set(true);
        assertThat(inverted.get()).isFalse();
    }
}
