/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.view.dialog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ai.nervemind.common.dto.VariableDTO;
import ai.nervemind.common.dto.VariableDTO.VariableScope;
import ai.nervemind.common.dto.VariableDTO.VariableType;
import ai.nervemind.ui.integration.FxTestBase;
import ai.nervemind.ui.viewmodel.dialog.VariableEditViewModel;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

@ExtendWith(MockitoExtension.class)
@DisplayName("VariableEditController")
class VariableEditControllerTest extends FxTestBase {

    @Mock
    private Stage stage;

    @Mock
    private VariableEditViewModel mockViewModel;

    @Test
    @DisplayName("initialize should setup combos and save new variable")
    void initializeShouldSetupCombosAndSaveNewVariable() throws Exception {
        VariableEditController controller = runOnFxThreadAndWait(() -> {
            VariableEditController c = new VariableEditController();
            injectFields(c);
            c.initialize();
            return c;
        });

        VariableEditViewModel realVm = new VariableEditViewModel(null, 99L);
        runOnFxThread(() -> controller.initialize(realVm, stage));

        runOnFxThread(() -> {
            getField(controller, "nameField", TextField.class).setText("customer_id");
            getField(controller, "typeCombo", ComboBox.class).setValue(VariableType.STRING);
            getField(controller, "scopeCombo", ComboBox.class).setValue(VariableScope.WORKFLOW);
            getField(controller, "valueArea", TextArea.class).setText("C-123");
            getField(controller, "descriptionField", TextField.class).setText("Customer identifier");
            invokePrivate(controller, "handleSave");
        });

        assertThat(controller.wasApplied()).isTrue();
        VariableDTO result = controller.getResult();
        assertThat(result.name()).isEqualTo("customer_id");
        assertThat(result.type()).isEqualTo(VariableType.STRING);
        assertThat(result.scope()).isEqualTo(VariableScope.WORKFLOW);
        assertThat(result.workflowId()).isEqualTo(99L);
        assertThat(result.value()).isEqualTo("C-123");
        verify(stage).close();
    }

    @Test
    @DisplayName("save should not apply when validation fails")
    void saveShouldNotApplyWhenValidationFails() throws Exception {
        VariableEditController controller = runOnFxThreadAndWait(() -> {
            VariableEditController c = new VariableEditController();
            injectFields(c);
            c.initialize();
            return c;
        });

        when(mockViewModel.isEditMode()).thenReturn(false);
        when(mockViewModel.nameProperty()).thenReturn(new SimpleStringProperty(""));
        when(mockViewModel.typeProperty()).thenReturn(new SimpleObjectProperty<>(VariableType.STRING));
        when(mockViewModel.scopeProperty()).thenReturn(new SimpleObjectProperty<>(VariableScope.GLOBAL));
        when(mockViewModel.valueProperty()).thenReturn(new SimpleStringProperty(""));
        when(mockViewModel.descriptionProperty()).thenReturn(new SimpleStringProperty(""));
        when(mockViewModel.scopeEditableProperty()).thenReturn(new SimpleBooleanProperty(true));
        when(mockViewModel.formValidProperty()).thenReturn(new SimpleBooleanProperty(false));
        when(mockViewModel.validate()).thenReturn(false);

        runOnFxThread(() -> controller.initialize(mockViewModel, stage));
        runOnFxThread(() -> invokePrivate(controller, "handleSave"));

        assertThat(controller.wasApplied()).isFalse();
        verify(mockViewModel, never()).confirm();
        verify(stage, never()).close();
    }

    @Test
    @DisplayName("cancel should close dialog and leave applied false")
    void cancelShouldCloseDialogAndLeaveAppliedFalse() throws Exception {
        VariableEditController controller = runOnFxThreadAndWait(() -> {
            VariableEditController c = new VariableEditController();
            injectFields(c);
            c.initialize();
            return c;
        });

        VariableEditViewModel realVm = new VariableEditViewModel(null, null);
        runOnFxThread(() -> controller.initialize(realVm, stage));
        runOnFxThread(() -> invokePrivate(controller, "handleCancel"));

        assertThat(controller.wasApplied()).isFalse();
        verify(stage).close();
    }

    private static void injectFields(VariableEditController controller) {
        setField(controller, "titleLabel", new Label());
        setField(controller, "nameField", new TextField());
        setField(controller, "typeCombo", new ComboBox<VariableType>());
        setField(controller, "scopeCombo", new ComboBox<VariableScope>());
        setField(controller, "valueArea", new TextArea());
        setField(controller, "valueLabel", new Label());
        setField(controller, "descriptionField", new TextField());
        setField(controller, "saveButton", new Button());
        setField(controller, "cancelButton", new Button());
    }

    private static void invokePrivate(Object target, String methodName) {
        try {
            Method m = target.getClass().getDeclaredMethod(methodName);
            m.setAccessible(true);
            m.invoke(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> T getField(Object target, String fieldName, Class<T> type) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return type.cast(f.get(target));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
