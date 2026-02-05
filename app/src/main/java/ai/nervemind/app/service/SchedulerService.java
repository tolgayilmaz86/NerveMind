package ai.nervemind.app.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import ai.nervemind.common.dto.WorkflowDTO;
import ai.nervemind.common.enums.TriggerType;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Service for scheduling workflow executions.
 * Uses virtual threads for efficient handling of many scheduled workflows.
 */
@Service
public class SchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerService.class);

    private final WorkflowService workflowService;
    private final ExecutionService executionService;

    // Virtual thread-based scheduler for lightweight concurrent scheduling
    private final ScheduledExecutorService scheduler;
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    /**
     * Creates a new SchedulerService with the given dependencies.
     *
     * @param workflowService the workflow service for managing workflows
     * @param executionService the execution service for running workflows
     */
    public SchedulerService(WorkflowService workflowService, ExecutionService executionService) {
        this.workflowService = workflowService;
        this.executionService = executionService;
        // Use virtual threads for scheduled tasks - efficient for I/O-bound workflow
        // executions
        this.scheduler = Executors.newScheduledThreadPool(0, Thread.ofVirtual().factory());
    }

    /**
     * Initialize the scheduler by loading and scheduling all active workflows.
     */
    @PostConstruct
    public void initialize() {
        // Load and schedule all active scheduled workflows on startup
        workflowService.findActiveScheduledWorkflows().forEach(this::scheduleWorkflow);
    }

    /**
     * Shutdown the scheduler and cancel all scheduled tasks.
     */
    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException _) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Schedule a workflow for execution based on its cron expression.
     *
     * @param workflow the workflow to schedule
     */
    public void scheduleWorkflow(WorkflowDTO workflow) {
        if (workflow.triggerType() != TriggerType.SCHEDULE || workflow.cronExpression() == null) {
            return;
        }

        // Cancel existing schedule if any
        cancelSchedule(workflow.id());

        try {
            CronExpression cron = CronExpression.parse(workflow.cronExpression());

            // Schedule recurring execution using AtomicReference for self-referencing task
            final AtomicReference<Runnable> taskRef = new AtomicReference<>();

            Runnable task = () -> {
                try {
                    executionService.execute(workflow.id(), Map.of(
                            "triggeredAt", Instant.now().toString(),
                            "triggerType", "schedule",
                            "cronExpression", workflow.cronExpression()));
                } catch (Exception e) {
                    // Log error but don't stop scheduling
                    logger.error("Scheduled workflow execution failed: {} - {}", workflow.name(), e.getMessage(), e);
                }

                // Reschedule for next execution
                scheduleNextExecution(workflow.id(), cron, taskRef.get());
            };

            taskRef.set(task);
            scheduleNextExecution(workflow.id(), cron, task);

        } catch (IllegalArgumentException e) {
            logger.error("Invalid cron expression for workflow {}: {}", workflow.name(), workflow.cronExpression());
        }
    }

    private void scheduleNextExecution(Long workflowId, CronExpression cron, Runnable task) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = cron.next(now);

        if (next != null) {
            long delayMs = ChronoUnit.MILLIS.between(now, next);
            ScheduledFuture<?> future = scheduler.schedule(task, delayMs, TimeUnit.MILLISECONDS);
            scheduledTasks.put(workflowId, future);
        }
    }

    /**
     * Cancel a scheduled workflow.
     *
     * @param workflowId the ID of the workflow to cancel
     */
    public void cancelSchedule(Long workflowId) {
        ScheduledFuture<?> future = scheduledTasks.remove(workflowId);
        if (future != null) {
            future.cancel(false);
        }
    }

    /**
     * Reschedule a workflow (call when workflow is updated).
     *
     * @param workflowId the ID of the workflow to reschedule
     */
    public void rescheduleWorkflow(Long workflowId) {
        workflowService.findById(workflowId).ifPresent(workflow -> {
            if (workflow.isActive() && workflow.triggerType() == TriggerType.SCHEDULE) {
                scheduleWorkflow(workflow);
            } else {
                cancelSchedule(workflowId);
            }
        });
    }

    /**
     * Get count of active scheduled workflows.
     *
     * @return the number of currently scheduled workflows
     */
    public int getActiveScheduleCount() {
        return scheduledTasks.size();
    }
}
