package ai.nervemind.app.database.model;

import java.time.Instant;

import ai.nervemind.common.enums.ExecutionStatus;
import ai.nervemind.common.enums.TriggerType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * JPA Entity for Workflow Execution.
 */
@Entity
@Table(name = "executions", indexes = {
        @Index(name = "idx_executions_workflow", columnList = "workflow_id"),
        @Index(name = "idx_executions_status", columnList = "status"),
        @Index(name = "idx_executions_started", columnList = "started_at")
})
public class ExecutionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workflow_id", nullable = false)
    private Long workflowId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status = ExecutionStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type")
    private TriggerType triggerType;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "input_data_json", columnDefinition = "CLOB")
    private String inputDataJson;

    @Column(name = "output_data_json", columnDefinition = "CLOB")
    private String outputDataJson;

    @Column(name = "error_message", length = 4000)
    private String errorMessage;

    @Column(name = "execution_log", columnDefinition = "CLOB")
    private String executionLog;

    /**
     * Default constructor for JPA.
     */
    protected ExecutionEntity() {
    }

    /**
     * Creates a new ExecutionEntity.
     * 
     * @param workflowId  the workflow ID
     * @param triggerType the trigger type
     */
    public ExecutionEntity(Long workflowId, TriggerType triggerType) {
        this.workflowId = workflowId;
        this.triggerType = triggerType;
        this.status = ExecutionStatus.PENDING;
    }

    // Getters and Setters

    /**
     * Gets the execution ID.
     * 
     * @return the execution ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the execution ID.
     * 
     * @param id the execution ID to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the workflow ID.
     * 
     * @return the workflow ID
     */
    public Long getWorkflowId() {
        return workflowId;
    }

    /**
     * Sets the workflow ID.
     * 
     * @param workflowId the workflow ID to set
     */
    public void setWorkflowId(Long workflowId) {
        this.workflowId = workflowId;
    }

    /**
     * Gets the execution status.
     * 
     * @return the execution status
     */
    public ExecutionStatus getStatus() {
        return status;
    }

    /**
     * Sets the execution status.
     * 
     * @param status the execution status to set
     */
    public void setStatus(ExecutionStatus status) {
        this.status = status;
    }

    /**
     * Gets the trigger type.
     * 
     * @return the trigger type
     */
    public TriggerType getTriggerType() {
        return triggerType;
    }

    /**
     * Sets the trigger type.
     * 
     * @param triggerType the trigger type to set
     */
    public void setTriggerType(TriggerType triggerType) {
        this.triggerType = triggerType;
    }

    /**
     * Gets the start timestamp.
     * 
     * @return the start timestamp
     */
    public Instant getStartedAt() {
        return startedAt;
    }

    /**
     * Sets the start timestamp.
     * 
     * @param startedAt the start timestamp to set
     */
    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    /**
     * Gets the finish timestamp.
     * 
     * @return the finish timestamp
     */
    public Instant getFinishedAt() {
        return finishedAt;
    }

    /**
     * Sets the finish timestamp.
     * 
     * @param finishedAt the finish timestamp to set
     */
    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    /**
     * Gets the input data JSON.
     * 
     * @return the input data JSON string
     */
    public String getInputDataJson() {
        return inputDataJson;
    }

    /**
     * Sets the input data JSON.
     * 
     * @param inputDataJson the input data JSON string to set
     */
    public void setInputDataJson(String inputDataJson) {
        this.inputDataJson = inputDataJson;
    }

    /**
     * Gets the output data JSON.
     * 
     * @return the output data JSON string
     */
    public String getOutputDataJson() {
        return outputDataJson;
    }

    /**
     * Sets the output data JSON.
     * 
     * @param outputDataJson the output data JSON string to set
     */
    public void setOutputDataJson(String outputDataJson) {
        this.outputDataJson = outputDataJson;
    }

    /**
     * Gets the error message.
     * 
     * @return the error message string
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the error message.
     * 
     * @param errorMessage the error message string to set
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Gets the execution log.
     * 
     * @return the execution log string
     */
    public String getExecutionLog() {
        return executionLog;
    }

    /**
     * Sets the execution log.
     * 
     * @param executionLog the execution log string to set
     */
    public void setExecutionLog(String executionLog) {
        this.executionLog = executionLog;
    }
}
