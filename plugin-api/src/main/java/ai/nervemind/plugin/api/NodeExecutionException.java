package ai.nervemind.plugin.api;

/**
 * Exception thrown when a node execution fails.
 */
public class NodeExecutionException extends Exception {

    /** The type of node that threw the exception. */
    private final String nodeType;
    /** Whether the execution can be retried. */
    private final boolean retryable;

    /**
     * Constructs a new NodeExecutionException with the specified message.
     * 
     * @param message the error message
     */
    public NodeExecutionException(String message) {
        super(message);
        this.nodeType = null;
        this.retryable = false;
    }

    /**
     * Constructs a new NodeExecutionException with the specified message and cause.
     * 
     * @param message the error message
     * @param cause   the cause of the error
     */
    public NodeExecutionException(String message, Throwable cause) {
        super(message, cause);
        this.nodeType = null;
        this.retryable = false;
    }

    /**
     * Constructs a new NodeExecutionException with full context.
     * 
     * @param nodeType  the node type
     * @param message   the error message
     * @param cause     the cause of the error
     * @param retryable whether the error is retryable
     */
    public NodeExecutionException(String nodeType, String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.nodeType = nodeType;
        this.retryable = retryable;
    }

    /**
     * Gets the node type.
     * 
     * @return the node type
     */
    public String getNodeType() {
        return nodeType;
    }

    /**
     * Gets whether the execution can be retried.
     * 
     * @return true if retryable
     */
    public boolean isRetryable() {
        return retryable;
    }
}
