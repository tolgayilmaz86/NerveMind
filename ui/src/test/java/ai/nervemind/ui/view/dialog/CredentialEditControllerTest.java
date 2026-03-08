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
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ai.nervemind.common.dto.CredentialDTO;
import ai.nervemind.common.enums.CredentialType;
import ai.nervemind.ui.integration.FxTestBase;
import ai.nervemind.ui.viewmodel.dialog.CredentialEditViewModel;
import ai.nervemind.ui.viewmodel.dialog.CredentialEditViewModel.CredentialEditResult;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

@ExtendWith(MockitoExtension.class)
@DisplayName("CredentialEditController")
class CredentialEditControllerTest extends FxTestBase {

    @Mock
    private Stage stage;

    @Mock
    private CredentialEditViewModel mockViewModel;

    @Test
    @DisplayName("initialize should setup controls and save new credential")
    void initializeShouldSetupControlsAndSaveNewCredential() throws Exception {
        CredentialEditController controller = runOnFxThreadAndWait(() -> {
            CredentialEditController c = new CredentialEditController();
            injectFields(c);
            c.initialize();
            return c;
        });

        CredentialEditViewModel realVm = new CredentialEditViewModel(null);
        runOnFxThread(() -> controller.initialize(realVm, stage));

        runOnFxThread(() -> {
            getField(controller, "nameField", TextField.class).setText("OpenAI Key");
            getField(controller, "typeCombo", ComboBox.class).setValue(CredentialType.API_KEY);
            getField(controller, "dataField", PasswordField.class).setText("secret-token");
            getField(controller, "showDataCheckbox", CheckBox.class).setSelected(true);
            invokePrivate(controller, "handleSave");
        });

        assertThat(controller.wasApplied()).isTrue();
        CredentialEditResult result = controller.getResult();
        assertThat(result.dto().name()).isEqualTo("OpenAI Key");
        assertThat(result.dto().type()).isEqualTo(CredentialType.API_KEY);
        assertThat(result.data()).isEqualTo("secret-token");
        verify(stage).close();
    }

    @Test
    @DisplayName("save should not apply when viewModel validation fails")
    void saveShouldNotApplyWhenViewModelValidationFails() throws Exception {
        CredentialEditController controller = runOnFxThreadAndWait(() -> {
            CredentialEditController c = new CredentialEditController();
            injectFields(c);
            c.initialize();
            return c;
        });

        when(mockViewModel.isEditMode()).thenReturn(true);
        when(mockViewModel.nameProperty()).thenReturn(new SimpleStringProperty("Existing"));
        when(mockViewModel.typeProperty()).thenReturn(new SimpleObjectProperty<>(CredentialType.HTTP_BASIC));
        when(mockViewModel.dataProperty()).thenReturn(new SimpleStringProperty(""));
        when(mockViewModel.showDataProperty()).thenReturn(new SimpleBooleanProperty(false));
        when(mockViewModel.formValidProperty()).thenReturn(new SimpleBooleanProperty(false));
        when(mockViewModel.validate()).thenReturn(false);

        runOnFxThread(() -> controller.initialize(mockViewModel, stage));
        runOnFxThread(() -> invokePrivate(controller, "handleSave"));

        assertThat(controller.wasApplied()).isFalse();
        verify(mockViewModel, never()).confirm();
        verify(stage, never()).close();
    }

    @Test
    @DisplayName("cancel and getResult should delegate properly")
    void cancelAndGetResultShouldDelegateProperly() throws Exception {
        CredentialEditController controller = runOnFxThreadAndWait(() -> {
            CredentialEditController c = new CredentialEditController();
            injectFields(c);
            c.initialize();
            setField(c, "viewModel", mockViewModel);
            setField(c, "dialogStage", stage);
            return c;
        });

        CredentialEditResult expected = new CredentialEditResult(
                new CredentialDTO(1L, "Stored", CredentialType.API_KEY, Instant.now(), Instant.now()),
                "value");
        when(mockViewModel.getResult()).thenReturn(expected);

        runOnFxThread(() -> invokePrivate(controller, "handleCancel"));

        assertThat(controller.wasApplied()).isFalse();
        assertThat(controller.getResult()).isEqualTo(expected);
        verify(stage).close();
    }

    private static void injectFields(CredentialEditController controller) {
        setField(controller, "titleLabel", new Label());
        setField(controller, "nameField", new TextField());
        setField(controller, "typeCombo", new ComboBox<CredentialType>());
        setField(controller, "dataField", new PasswordField());
        setField(controller, "dataVisibleField", new TextField());
        setField(controller, "showDataCheckbox", new CheckBox());
        setField(controller, "dataLabel", new Label());
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
