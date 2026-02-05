/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nervemind.app.database.model.ExecutionEntity;
import ai.nervemind.app.database.repository.ExecutionRepository;
import ai.nervemind.common.enums.ExecutionStatus;
import ai.nervemind.common.enums.TriggerType;
import ai.nervemind.common.service.DevModeServiceInterface;
import ai.nervemind.common.service.ExecutionLogHandler;
import ai.nervemind.common.service.SettingsServiceInterface;

/**
 * Unit tests for ExecutionService cancellation functionality.
 *
 * <p>
 * Tests the execution cancellation features including:
 * <ul>
 * <li>Cancelling individual executions</li>
 * <li>Cancelling all executions for a workflow</li>
 * <li>ExecutionContext cancellation flag behavior</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExecutionService Cancellation Tests")
class ExecutionServiceCancellationTest {

    @Mock
    private ExecutionRepository executionRepository;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private CredentialService credentialService;

    @Mock
    private NodeExecutorRegistry nodeExecutorRegistry;

    @Mock
    private SettingsServiceInterface settingsService;

    @Mock
    private DevModeServiceInterface devModeService;

    private ObjectMapper objectMapper;
    private ExecutionLogger executionLogger;
    private ExecutionService executionService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        executionLogger = new ExecutionLogger();

        // Setup default settings - use lenient to avoid UnnecessaryStubbingException
        lenient().when(settingsService.getInt(any(String.class), any(Integer.class)))
                .thenAnswer(inv -> inv.getArgument(1));
        lenient().when(settingsService.getLong(any(String.class), any(Long.class)))
                .thenAnswer(inv -> inv.getArgument(1));
        lenient().when(settingsService.getValue(any(String.class), any(String.class)))
                .thenAnswer(inv -> inv.getArgument(1));
        lenient().when(settingsService.getBoolean(any(String.class), any(Boolean.class)))
                .thenAnswer(inv -> inv.getArgument(1));

        List<ExecutionLogHandler> logHandlers = Collections.emptyList();

        executionService = new ExecutionService(
                executionRepository,
                workflowService,
                credentialService,
                nodeExecutorRegistry,
                objectMapper,
                executionLogger,
                settingsService,
                devModeService,
                logHandlers);
    }

    // ========== ExecutionContext.isCancelled() Tests ==========

    @Nested
    @DisplayName("ExecutionContext.isCancelled()")
    class ExecutionContextIsCancelledTests {

        @Test
        @DisplayName("returns false when cancel flag is not set")
        void returnsFalseWhenNotCancelled() {
            AtomicBoolean cancelFlag = new AtomicBoolean(false);
            ExecutionService.ExecutionContext context = new ExecutionService.ExecutionContext(
                    1L, null, Map.of(), null, new ExecutionLogger(), cancelFlag);

            assertThat(context.isCancelled()).isFalse();
        }

        @Test
        @DisplayName("returns true when cancel flag is set")
        void returnsTrueWhenCancelled() {
            AtomicBoolean cancelFlag = new AtomicBoolean(false);
            ExecutionService.ExecutionContext context = new ExecutionService.ExecutionContext(
                    1L, null, Map.of(), null, new ExecutionLogger(), cancelFlag);

            // Simulate cancellation
            cancelFlag.set(true);

            assertThat(context.isCancelled()).isTrue();
        }

        @Test
        @DisplayName("returns false when cancel flag is null (backwards compatibility)")
        void returnsFalseWhenCancelFlagNull() {
            // Use the constructor without cancel flag
            ExecutionService.ExecutionContext context = new ExecutionService.ExecutionContext(
                    1L, null, Map.of(), null, new ExecutionLogger());

            // Should not throw, should return false
            assertThat(context.isCancelled()).isFalse();
        }
    }

    // ========== cancelExecution() Tests ==========

    @Nested
    @DisplayName("cancelExecution()")
    class CancelExecutionTests {

        @Test
        @DisplayName("returns false when execution not found")
        void returnsFalseWhenExecutionNotFound() {
            when(executionRepository.findById(999L)).thenReturn(Optional.empty());

            boolean result = executionService.cancelExecution(999L);

            assertThat(result).isFalse();
            verify(executionRepository, never()).save(any());
        }

        @Test
        @DisplayName("returns false when execution is not running")
        void returnsFalseWhenExecutionNotRunning() {
            ExecutionEntity execution = createExecution(1L, ExecutionStatus.SUCCESS);
            when(executionRepository.findById(1L)).thenReturn(Optional.of(execution));

            boolean result = executionService.cancelExecution(1L);

            assertThat(result).isFalse();
            // Execution should not be saved since status wasn't RUNNING
            verify(executionRepository, never()).save(any());
        }

        @Test
        @DisplayName("cancels running execution and updates database")
        void cancelsRunningExecution() {
            ExecutionEntity execution = createExecution(1L, ExecutionStatus.RUNNING);
            when(executionRepository.findById(1L)).thenReturn(Optional.of(execution));

            boolean result = executionService.cancelExecution(1L);

            assertThat(result).isTrue();

            // Verify the execution was saved with CANCELLED status
            ArgumentCaptor<ExecutionEntity> captor = ArgumentCaptor.forClass(ExecutionEntity.class);
            verify(executionRepository).save(captor.capture());

            ExecutionEntity savedExecution = captor.getValue();
            assertThat(savedExecution.getStatus()).isEqualTo(ExecutionStatus.CANCELLED);
            assertThat(savedExecution.getFinishedAt()).isNotNull();
            assertThat(savedExecution.getErrorMessage()).contains("cancelled");
        }

        @Test
        @DisplayName("calls devModeService.cancelStepExecution() when cancelling")
        void callsDevModeServiceOnCancel() {
            ExecutionEntity execution = createExecution(1L, ExecutionStatus.RUNNING);
            when(executionRepository.findById(1L)).thenReturn(Optional.of(execution));

            executionService.cancelExecution(1L);

            verify(devModeService).cancelStepExecution();
        }
    }

    // ========== cancelAllForWorkflow() Tests ==========

    @Nested
    @DisplayName("cancelAllForWorkflow()")
    class CancelAllForWorkflowTests {

        @Test
        @DisplayName("returns 0 when no running executions for workflow")
        void returnsZeroWhenNoRunningExecutions() {
            // Mock findRunningExecutions to return empty list
            when(executionRepository.findRunningExecutions())
                    .thenReturn(List.of());

            int result = executionService.cancelAllForWorkflow(100L);

            assertThat(result).isZero();
        }

        @Test
        @DisplayName("cancels all running executions for the specified workflow")
        void cancelsAllRunningExecutionsForWorkflow() {
            // Create running executions - 2 for workflow 100, 1 for workflow 200
            ExecutionEntity exec1 = createExecution(1L, 100L, ExecutionStatus.RUNNING);
            ExecutionEntity exec2 = createExecution(2L, 100L, ExecutionStatus.RUNNING);
            ExecutionEntity exec3 = createExecution(3L, 200L, ExecutionStatus.RUNNING);

            when(executionRepository.findRunningExecutions())
                    .thenReturn(List.of(exec1, exec2, exec3));
            when(executionRepository.findById(1L)).thenReturn(Optional.of(exec1));
            when(executionRepository.findById(2L)).thenReturn(Optional.of(exec2));

            int result = executionService.cancelAllForWorkflow(100L);

            // Should cancel 2 executions (the ones for workflow 100)
            assertThat(result).isEqualTo(2);
        }

        @Test
        @DisplayName("does not cancel executions from other workflows")
        void doesNotCancelExecutionsFromOtherWorkflows() {
            ExecutionEntity exec1 = createExecution(1L, 200L, ExecutionStatus.RUNNING);

            when(executionRepository.findRunningExecutions())
                    .thenReturn(List.of(exec1));

            int result = executionService.cancelAllForWorkflow(100L);

            assertThat(result).isZero();
            // findById should never be called because workflow ID doesn't match
            verify(executionRepository, never()).findById(1L);
        }
    }

    // ========== Helper Methods ==========

    private ExecutionEntity createExecution(Long id, ExecutionStatus status) {
        return createExecution(id, 1L, status);
    }

    private ExecutionEntity createExecution(Long id, Long workflowId, ExecutionStatus status) {
        ExecutionEntity entity = new ExecutionEntity(workflowId, TriggerType.MANUAL);
        entity.setId(id);
        entity.setStatus(status);
        entity.setStartedAt(Instant.now());
        return entity;
    }

}
