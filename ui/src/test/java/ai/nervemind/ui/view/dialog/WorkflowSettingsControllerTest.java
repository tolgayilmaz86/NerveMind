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

import ai.nervemind.ui.integration.FxTestBase;
import ai.nervemind.ui.viewmodel.dialog.WorkflowSettingsViewModel;
import ai.nervemind.ui.viewmodel.dialog.WorkflowSettingsViewModel.WorkflowSettings;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowSettingsController")
class WorkflowSettingsControllerTest extends FxTestBase {

    @Mock
    private Stage stage;

    @Mock
    private WorkflowSettingsViewModel mockViewModel;

    @Test
    @DisplayName("initialize with view model should bind values and allow save")
    void initializeWithViewModelShouldBindValuesAndAllowSave() throws Exception {
        WorkflowSettingsController controller = runOnFxThreadAndWait(() -> {
            WorkflowSettingsController c = new WorkflowSettingsController();
            injectFields(c);
            return c;
        });

        WorkflowSettingsViewModel realVm = new WorkflowSettingsViewModel("Initial", "Desc", true);

        runOnFxThread(() -> controller.initialize(realVm, stage));

        runOnFxThread(() -> {
            getField(controller, "nameField", TextField.class).setText("Updated Name");
            getField(controller, "descriptionArea", TextArea.class).setText("Updated Desc");
            getField(controller, "activeCheckbox", CheckBox.class).setSelected(false);
        });

        runOnFxThread(() -> invokePrivate(controller, "handleSave"));

        assertThat(controller.wasApplied()).isTrue();
        assertThat(controller.getResult()).isEqualTo(new WorkflowSettings("Updated Name", "Updated Desc", false));
        verify(stage).close();
    }

    @Test
    @DisplayName("save should not close dialog when validation fails")
    void saveShouldNotCloseDialogWhenValidationFails() throws Exception {
        WorkflowSettingsController controller = runOnFxThreadAndWait(() -> {
            WorkflowSettingsController c = new WorkflowSettingsController();
            injectFields(c);
            return c;
        });

        when(mockViewModel.nameProperty()).thenReturn(new javafx.beans.property.SimpleStringProperty("n"));
        when(mockViewModel.descriptionProperty()).thenReturn(new javafx.beans.property.SimpleStringProperty("d"));
        when(mockViewModel.activeProperty()).thenReturn(new javafx.beans.property.SimpleBooleanProperty(true));
        when(mockViewModel.validate()).thenReturn(false);

        runOnFxThread(() -> controller.initialize(mockViewModel, stage));
        runOnFxThread(() -> invokePrivate(controller, "handleSave"));

        assertThat(controller.wasApplied()).isFalse();
        verify(mockViewModel, never()).confirm();
        verify(stage, never()).close();
    }

    @Test
    @DisplayName("cancel should close dialog and keep applied false")
    void cancelShouldCloseDialogAndKeepAppliedFalse() throws Exception {
        WorkflowSettingsController controller = runOnFxThreadAndWait(() -> {
            WorkflowSettingsController c = new WorkflowSettingsController();
            injectFields(c);
            return c;
        });

        WorkflowSettingsViewModel realVm = new WorkflowSettingsViewModel("A", "B", true);
        runOnFxThread(() -> controller.initialize(realVm, stage));
        runOnFxThread(() -> invokePrivate(controller, "handleCancel"));

        assertThat(controller.wasApplied()).isFalse();
        verify(stage).close();
    }

    private static void injectFields(WorkflowSettingsController controller) {
        setField(controller, "nameField", new TextField());
        setField(controller, "descriptionArea", new TextArea());
        setField(controller, "activeCheckbox", new CheckBox());
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
