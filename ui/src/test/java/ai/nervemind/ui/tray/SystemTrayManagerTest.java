/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.tray;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.TrayIcon;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ai.nervemind.common.dto.ExecutionDTO;
import ai.nervemind.common.dto.WorkflowDTO;
import ai.nervemind.common.enums.ExecutionStatus;
import ai.nervemind.common.enums.TriggerType;
import ai.nervemind.common.service.ExecutionServiceInterface;
import ai.nervemind.common.service.WorkflowServiceInterface;
import javafx.stage.Stage;

@ExtendWith(MockitoExtension.class)
@DisplayName("SystemTrayManager")
class SystemTrayManagerTest {

    @Mock
    private WorkflowServiceInterface workflowService;

    @Mock
    private ExecutionServiceInterface executionService;

    @Mock
    private Stage primaryStage;

    @Mock
    private TrayIcon trayIcon;

    @Mock
    private Runnable onQuit;

    private SystemTrayManager manager;

    @BeforeEach
    void setUp() {
        manager = new SystemTrayManager(primaryStage, workflowService, executionService, onQuit);
    }

    @Test
    @DisplayName("install should return false when SystemTray is unsupported")
    void installShouldReturnFalseWhenSystemTrayUnsupported() {
        assertFalse(manager.install());
    }

    @Nested
    @DisplayName("Workflow Execution Notifications")
    class WorkflowExecutionNotificationsTests {

        @Test
        @DisplayName("should show running and completed notifications for successful execution")
        void shouldShowRunningAndCompletedNotificationsForSuccess() throws Exception {
            WorkflowDTO workflow = createTestWorkflow(10L, "Daily Sync");
            when(executionService.executeAsync(eq(workflow.id()), anyMap()))
                    .thenReturn(CompletableFuture
                            .completedFuture(createExecutionResult(workflow, ExecutionStatus.SUCCESS)));
            setPrivateField(manager, "trayIcon", trayIcon);

            invokeExecuteWithStatus(workflow);

            verify(executionService).executeAsync(eq(workflow.id()), eq(Map.of()));
            verify(trayIcon).displayMessage("NerveMind", "Running: Daily Sync", TrayIcon.MessageType.INFO);
            verify(trayIcon).displayMessage("NerveMind", "Completed: Daily Sync", TrayIcon.MessageType.INFO);
        }

        @Test
        @DisplayName("should show cancelled notification for cancelled execution")
        void shouldShowCancelledNotificationForCancelledExecution() throws Exception {
            WorkflowDTO workflow = createTestWorkflow(11L, "Cleanup");
            when(executionService.executeAsync(eq(workflow.id()), anyMap()))
                    .thenReturn(CompletableFuture
                            .completedFuture(createExecutionResult(workflow, ExecutionStatus.CANCELLED)));
            setPrivateField(manager, "trayIcon", trayIcon);

            invokeExecuteWithStatus(workflow);

            verify(trayIcon).displayMessage("NerveMind", "Running: Cleanup", TrayIcon.MessageType.INFO);
            verify(trayIcon).displayMessage("NerveMind", "Cancelled: Cleanup", TrayIcon.MessageType.WARNING);
        }

        @Test
        @DisplayName("should show failed notification for non-terminal success statuses")
        void shouldShowFailedNotificationForFailedExecution() throws Exception {
            WorkflowDTO workflow = createTestWorkflow(12L, "Import");
            when(executionService.executeAsync(eq(workflow.id()), anyMap()))
                    .thenReturn(
                            CompletableFuture.completedFuture(createExecutionResult(workflow, ExecutionStatus.FAILED)));
            setPrivateField(manager, "trayIcon", trayIcon);

            invokeExecuteWithStatus(workflow);

            verify(trayIcon).displayMessage("NerveMind", "Running: Import", TrayIcon.MessageType.INFO);
            verify(trayIcon).displayMessage("NerveMind", "Failed: Import", TrayIcon.MessageType.ERROR);
        }

        @Test
        @DisplayName("should show error notification when async execution completes exceptionally")
        void shouldShowErrorNotificationWhenAsyncExecutionFails() throws Exception {
            WorkflowDTO workflow = createTestWorkflow(13L, "Export");
            when(executionService.executeAsync(eq(workflow.id()), anyMap()))
                    .thenReturn(CompletableFuture.failedFuture(new RuntimeException("network unavailable")));
            setPrivateField(manager, "trayIcon", trayIcon);

            invokeExecuteWithStatus(workflow);

            verify(trayIcon).displayMessage("NerveMind", "Running: Export", TrayIcon.MessageType.INFO);
            verify(trayIcon).displayMessage(
                    "NerveMind",
                    "Error: Export - java.lang.RuntimeException: network unavailable",
                    TrayIcon.MessageType.ERROR);
        }

        @Test
        @DisplayName("should execute workflow even when trayIcon is null")
        void shouldExecuteWorkflowWhenTrayIconIsNull() throws Exception {
            WorkflowDTO workflow = createTestWorkflow(14L, "No Tray");
            when(executionService.executeAsync(eq(workflow.id()), anyMap()))
                    .thenReturn(CompletableFuture
                            .completedFuture(createExecutionResult(workflow, ExecutionStatus.SUCCESS)));

            invokeExecuteWithStatus(workflow);

            verify(executionService, times(1)).executeAsync(eq(workflow.id()), eq(Map.of()));
        }
    }

    private void invokeExecuteWithStatus(WorkflowDTO workflow) throws Exception {
        Method method = SystemTrayManager.class.getDeclaredMethod(
                "executeWorkflowWithStatus",
                WorkflowDTO.class,
                org.kordamp.ikonli.javafx.FontIcon.class);
        method.setAccessible(true);
        method.invoke(manager, workflow, null);
    }

    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private WorkflowDTO createTestWorkflow(Long id, String name) {
        return new WorkflowDTO(
                id,
                name,
                "Tray workflow",
                List.of(),
                List.of(),
                Map.of(),
                false,
                TriggerType.MANUAL,
                null,
                Instant.now(),
                Instant.now(),
                null,
                1);
    }

    private ExecutionDTO createExecutionResult(WorkflowDTO workflow, ExecutionStatus status) {
        return new ExecutionDTO(
                100L,
                workflow.id(),
                workflow.name(),
                status,
                TriggerType.MANUAL,
                Instant.now().minusSeconds(1),
                Instant.now(),
                100L,
                Map.of(),
                Map.of(),
                status == ExecutionStatus.FAILED ? "failure" : null,
                List.of());
    }
}
