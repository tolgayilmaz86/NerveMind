package ai.nervemind.common.enums;

/**
 * Status of a workflow execution.
 */
public enum ExecutionStatus {
    /** Execution is queued. */
    PENDING("Pending", "Execution is queued"),
    /** Execution is in progress. */
    RUNNING("Running", "Execution is in progress"),
    /** Execution completed successfully. */
    SUCCESS("Success", "Execution completed successfully"),
    /** Execution failed with an error. */
    FAILED("Failed", "Execution failed with an error"),
    /** Execution was cancelled by user. */
    CANCELLED("Cancelled", "Execution was cancelled by user"),
    /** Execution is waiting for external event. */
    WAITING("Waiting", "Execution is waiting for external event");

    private final String displayName;
    private final String description;

    /**
     * Constructs a new ExecutionStatus.
     * 
     * @param displayName the display name
     * @param description the description
     */
    ExecutionStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Gets the display name.
     * 
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the description.
     * 
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if the status is terminal (Success, Failed, or Cancelled).
     * 
     * @return true if terminal
     */
    public boolean isTerminal() {
        return this == SUCCESS || this == FAILED || this == CANCELLED;
    }

    /**
     * Checks if the execution is currently active.
     * 
     * @return true if running or waiting
     */
    public boolean isRunning() {
        return this == RUNNING || this == WAITING;
    }
}
