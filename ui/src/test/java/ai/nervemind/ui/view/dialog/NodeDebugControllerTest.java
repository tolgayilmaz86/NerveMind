/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.view.dialog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kordamp.ikonli.javafx.FontIcon;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ai.nervemind.common.domain.Node;
import ai.nervemind.ui.console.ExecutionConsoleService.NodeExecutionState;
import ai.nervemind.ui.console.ExecutionConsoleService.NodeState;
import ai.nervemind.ui.integration.FxTestBase;
import ai.nervemind.ui.viewmodel.dialog.NodeDebugViewModel;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

@ExtendWith(MockitoExtension.class)
@DisplayName("NodeDebugController")
class NodeDebugControllerTest extends FxTestBase {

    @Mock
    private Stage stage;

    @Test
    @DisplayName("initialize should bind fields and show error section for failed state")
    void initializeShouldBindFieldsAndShowErrorSectionForFailedState() throws Exception {
        NodeDebugController controller = runOnFxThreadAndWait(() -> {
            NodeDebugController c = new NodeDebugController();
            injectFields(c);
            return c;
        });

        Node node = new Node("n1", "code", "Transform", new Node.Position(10, 20), java.util.Map.of(), null, false,
                null);
        NodeExecutionState state = new NodeExecutionState(
                "n1",
                "Transform",
                NodeState.FAILED,
                System.currentTimeMillis() - 100,
                System.currentTimeMillis(),
                "boom",
                java.util.Map.of("a", 1),
                java.util.Map.of("b", 2));
        NodeDebugViewModel vm = new NodeDebugViewModel(node, state);

        runOnFxThread(() -> controller.initialize(vm, stage));

        runOnFxThread(() -> {
            assertThat(getField(controller, "nodeNameLabel", Label.class).getText()).isEqualTo("Transform");
            assertThat(getField(controller, "nodeTypeLabel", Label.class).getText()).isEqualTo("code");
            assertThat(getField(controller, "statusLabel", Label.class).getText()).isEqualTo("Failed");
            assertThat(getField(controller, "errorSection", VBox.class).isVisible()).isTrue();
            assertThat(getField(controller, "errorArea", TextArea.class).getText()).isEqualTo("boom");
            assertThat(getField(controller, "statusIcon", FontIcon.class).getIconLiteral())
                    .isEqualTo(vm.getStatusIconCode());
        });

        verify(stage).setTitle("Debug View: Transform");
    }

    @Test
    @DisplayName("copy handlers and close should work")
    void copyHandlersAndCloseShouldWork() throws Exception {
        NodeDebugController controller = runOnFxThreadAndWait(() -> {
            NodeDebugController c = new NodeDebugController();
            injectFields(c);
            return c;
        });

        Node node = new Node("n2", "httpRequest", "Fetch", new Node.Position(0, 0), java.util.Map.of(), null,
                false,
                null);
        NodeExecutionState state = new NodeExecutionState(
                "n2",
                "Fetch",
                NodeState.SUCCESS,
                System.currentTimeMillis() - 50,
                System.currentTimeMillis(),
                null,
                "input",
                "output");
        NodeDebugViewModel vm = new NodeDebugViewModel(node, state);

        runOnFxThread(() -> controller.initialize(vm, stage));
        runOnFxThread(() -> invokePrivate(controller, "handleCopyInput"));
        runOnFxThread(() -> invokePrivate(controller, "handleCopyOutput"));
        runOnFxThread(() -> invokePrivate(controller, "handleCopyError"));
        runOnFxThread(() -> invokePrivate(controller, "handleClose"));

        runOnFxThread(() -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            assertThat(clipboard.getString()).isEqualTo(vm.getErrorText());
            assertThat(getField(controller, "copyInputButton", Button.class).getText()).isEqualTo("Copied!");
        });

        verify(stage).close();
    }

    private static void injectFields(NodeDebugController controller) {
        setField(controller, "nodeNameLabel", new Label());
        setField(controller, "nodeTypeLabel", new Label());
        setField(controller, "statusLabel", new Label());
        setField(controller, "statusIcon", new FontIcon());
        setField(controller, "timestampLabel", new Label());
        setField(controller, "durationLabel", new Label());
        setField(controller, "inputDataArea", new TextArea());
        setField(controller, "outputDataArea", new TextArea());
        setField(controller, "errorSection", new VBox());
        setField(controller, "errorArea", new TextArea());
        setField(controller, "copyInputButton", new Button("Copy"));
        setField(controller, "copyOutputButton", new Button("Copy"));
        setField(controller, "copyErrorButton", new Button("Copy"));
        setField(controller, "closeButton", new Button("Close"));
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
