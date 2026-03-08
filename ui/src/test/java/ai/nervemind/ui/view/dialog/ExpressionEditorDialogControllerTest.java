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
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ai.nervemind.ui.component.ExpressionEditorComponent;
import ai.nervemind.ui.integration.FxTestBase;
import ai.nervemind.ui.viewmodel.dialog.ExpressionEditorDialogViewModel;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpressionEditorDialogController")
class ExpressionEditorDialogControllerTest extends FxTestBase {

    @Mock
    private Stage stage;

    @Mock
    private ExpressionEditorDialogViewModel viewModel;

    @Mock
    private ExpressionEditorComponent editor;

    @Test
    @DisplayName("initialize with view model should bind and wire editor")
    void initializeWithViewModelShouldBindAndWireEditor() throws Exception {
        ExpressionEditorDialogController controller = runOnFxThreadAndWait(() -> {
            ExpressionEditorDialogController c = new ExpressionEditorDialogController();
            injectFields(c);
            setField(c, "editor", editor);
            return c;
        });

        var editorExpression = new SimpleStringProperty("a");
        var vmExpression = new SimpleStringProperty("b");
        var vmPreview = new SimpleStringProperty("preview");

        when(editor.expressionProperty()).thenReturn(editorExpression);
        when(viewModel.expressionProperty()).thenReturn(vmExpression);
        when(viewModel.previewTextProperty()).thenReturn(vmPreview);
        when(viewModel.getAvailableVariables())
                .thenReturn(javafx.collections.FXCollections.observableArrayList(List.of("x")));

        runOnFxThread(() -> controller.initialize(viewModel, stage));

        verify(editor).setAvailableVariables(viewModel.getAvailableVariables());
        verify(editor).setOnExpressionChange(org.mockito.ArgumentMatchers.any());
        runOnFxThread(() -> {
            TextArea previewArea = getField(controller, "previewArea", TextArea.class);
            assertThat(previewArea.getText()).isEqualTo("preview");
        });
    }

    @Test
    @DisplayName("insert should confirm and close when valid")
    void insertShouldConfirmAndCloseWhenValid() throws Exception {
        ExpressionEditorDialogController controller = runOnFxThreadAndWait(() -> {
            ExpressionEditorDialogController c = new ExpressionEditorDialogController();
            injectFields(c);
            setField(c, "editor", editor);
            return c;
        });

        when(editor.expressionProperty()).thenReturn(new SimpleStringProperty("expr"));
        when(viewModel.expressionProperty()).thenReturn(new SimpleStringProperty("expr"));
        when(viewModel.previewTextProperty()).thenReturn(new SimpleStringProperty("preview"));
        when(viewModel.getAvailableVariables()).thenReturn(javafx.collections.FXCollections.observableArrayList());
        when(viewModel.validate()).thenReturn(true);

        runOnFxThread(() -> controller.initialize(viewModel, stage));
        runOnFxThread(() -> invokePrivate(controller, "handleInsert"));

        assertThat(controller.wasApplied()).isTrue();
        verify(viewModel).confirm();
        verify(stage).close();
    }

    @Test
    @DisplayName("insert should not close when invalid")
    void insertShouldNotCloseWhenInvalid() throws Exception {
        ExpressionEditorDialogController controller = runOnFxThreadAndWait(() -> {
            ExpressionEditorDialogController c = new ExpressionEditorDialogController();
            injectFields(c);
            setField(c, "editor", editor);
            return c;
        });

        when(editor.expressionProperty()).thenReturn(new SimpleStringProperty("expr"));
        when(viewModel.expressionProperty()).thenReturn(new SimpleStringProperty("expr"));
        when(viewModel.previewTextProperty()).thenReturn(new SimpleStringProperty("preview"));
        when(viewModel.getAvailableVariables()).thenReturn(javafx.collections.FXCollections.observableArrayList());
        when(viewModel.validate()).thenReturn(false);

        runOnFxThread(() -> controller.initialize(viewModel, stage));
        runOnFxThread(() -> invokePrivate(controller, "handleInsert"));

        assertThat(controller.wasApplied()).isFalse();
        verify(viewModel, never()).confirm();
        verify(stage, never()).close();
    }

    @Test
    @DisplayName("copy, cancel, setExpression and result forwarding should delegate to viewModel")
    void copyCancelSetExpressionAndResultForwardingShouldDelegateToViewModel() throws Exception {
        ExpressionEditorDialogController controller = runOnFxThreadAndWait(() -> {
            ExpressionEditorDialogController c = new ExpressionEditorDialogController();
            injectFields(c);
            setField(c, "viewModel", viewModel);
            setField(c, "dialogStage", stage);
            return c;
        });

        when(viewModel.getResult()).thenReturn("result-expr");

        runOnFxThread(() -> {
            invokePrivate(controller, "handleCopy");
            invokePrivate(controller, "handleCancel");
            controller.setExpression("abc");
        });

        verify(viewModel).copyToClipboard();
        verify(stage).close();
        verify(viewModel).setExpression("abc");
        assertThat(controller.getResult()).isEqualTo("result-expr");
    }

    private static void injectFields(ExpressionEditorDialogController controller) {
        setField(controller, "tabPane", new TabPane());
        setField(controller, "editorTab", new Tab("Editor"));
        setField(controller, "helpTab", new Tab("Help"));
        setField(controller, "editorContent", new VBox());
        setField(controller, "helpScrollPane", new ScrollPane());
        setField(controller, "previewArea", new TextArea());
        setField(controller, "insertButton", new Button());
        setField(controller, "copyButton", new Button());
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
