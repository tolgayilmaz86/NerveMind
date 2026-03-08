/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.view.dialog;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ai.nervemind.common.dto.WorkflowDTO;
import ai.nervemind.common.enums.TriggerType;
import ai.nervemind.ui.integration.FxTestBase;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

@DisplayName("WorkflowListDialogController")
class WorkflowListDialogControllerTest extends FxTestBase {

    @Test
    @DisplayName("initialize should configure list cell factory")
    void initializeShouldConfigureListCellFactory() throws Exception {
        WorkflowListDialogController controller = runOnFxThreadAndWait(() -> {
            WorkflowListDialogController c = new WorkflowListDialogController();
            injectFields(c);
            c.initialize();
            return c;
        });

        runOnFxThread(() -> {
            ListView<WorkflowDTO> listView = getField(controller, "workflowListView", ListView.class);
            assertThat(listView.getCellFactory()).isNotNull();
        });
    }

    @Test
    @DisplayName("initWithWorkflows should bind list and reflect selection")
    void initWithWorkflowsShouldBindListAndReflectSelection() throws Exception {
        WorkflowListDialogController controller = runOnFxThreadAndWait(() -> {
            WorkflowListDialogController c = new WorkflowListDialogController();
            injectFields(c);
            c.initialize();
            c.initWithWorkflows(List.of(createWorkflow(1L, "Alpha"), createWorkflow(2L, "Beta")));
            return c;
        });

        runOnFxThread(() -> {
            ListView<WorkflowDTO> listView = getField(controller, "workflowListView", ListView.class);
            listView.getSelectionModel().select(1);

            WorkflowDTO selected = controller.getSelectedWorkflow();
            Label detailsLabel = getField(controller, "detailsLabel", Label.class);

            assertThat(selected).isNotNull();
            assertThat(selected.name()).isEqualTo("Beta");
            assertThat(detailsLabel.getText()).contains("Name: Beta");
            assertThat(controller.getViewModel()).isNotNull();
        });
    }

    @Test
    @DisplayName("setOnDeleteCallback should toggle delete visibility state")
    void setOnDeleteCallbackShouldToggleDeleteVisibilityState() throws Exception {
        WorkflowListDialogController controller = runOnFxThreadAndWait(() -> {
            WorkflowListDialogController c = new WorkflowListDialogController();
            injectFields(c);
            c.initialize();
            c.initWithWorkflows(List.of(createWorkflow(1L, "Only")));
            return c;
        });

        runOnFxThread(() -> {
            controller.setOnDeleteCallback(workflow -> {
            });
            assertThat(controller.isShowDeleteButton()).isTrue();
            assertThat(controller.showDeleteButtonProperty().get()).isTrue();
            assertThat(controller.getViewModel().isShowDeleteButton()).isTrue();

            controller.setOnDeleteCallback(null);
            assertThat(controller.isShowDeleteButton()).isFalse();
            assertThat(controller.getViewModel().isShowDeleteButton()).isFalse();
        });
    }

    @Test
    @DisplayName("initWithWorkflows should set placeholder when list is empty")
    void initWithWorkflowsShouldSetPlaceholderWhenListIsEmpty() throws Exception {
        WorkflowListDialogController controller = runOnFxThreadAndWait(() -> {
            WorkflowListDialogController c = new WorkflowListDialogController();
            injectFields(c);
            c.initialize();
            c.initWithWorkflows(List.of());
            return c;
        });

        runOnFxThread(() -> {
            ListView<WorkflowDTO> listView = getField(controller, "workflowListView", ListView.class);
            assertThat(listView.getPlaceholder()).isInstanceOf(Label.class);
            assertThat(((Label) listView.getPlaceholder()).getText()).contains("No workflows found");
        });
    }

    @Test
    @DisplayName("handleDelete should no-op when nothing is selected")
    void handleDeleteShouldNoOpWhenNothingIsSelected() throws Exception {
        WorkflowListDialogController controller = runOnFxThreadAndWait(() -> {
            WorkflowListDialogController c = new WorkflowListDialogController();
            injectFields(c);
            c.initialize();
            c.initWithWorkflows(List.of(createWorkflow(1L, "Single")));
            return c;
        });

        AtomicReference<Exception> errorRef = new AtomicReference<>();
        runOnFxThread(() -> {
            try {
                invokePrivate(controller, "handleDelete");
            } catch (Exception e) {
                errorRef.set(e);
            }
        });

        assertThat(errorRef.get()).isNull();
    }

    private static WorkflowDTO createWorkflow(Long id, String name) {
        Instant now = Instant.now();
        return new WorkflowDTO(
                id,
                name,
                "desc",
                List.of(),
                List.of(),
                Map.of(),
                true,
                TriggerType.MANUAL,
                null,
                now,
                now,
                null,
                1);
    }

    private static void injectFields(WorkflowListDialogController controller) {
        setField(controller, "workflowListView", new ListView<WorkflowDTO>());
        setField(controller, "detailsLabel", new Label());
        setField(controller, "deleteButton", new Button("Delete"));
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

    private static void invokePrivate(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(target);
    }
}
