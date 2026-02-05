/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.common.exception;

/**
 * Exception thrown when workflow node execution fails.
 * This includes errors during node processing, invalid parameters,
 * or runtime failures within a node.
 */
public class NodeExecutionException extends NerveMindException {

    private static final long serialVersionUID = 1L;

    /** The ID of the node that failed, or null if not specified. */
    private final String nodeId;
    /** The type of the node that failed, or null if not specified. */
    private final String nodeType;

    /**
     * Create a new NodeExecutionException.
     *
     * @param message the exception message
     */
    public NodeExecutionException(String message) {
        super(message);
        this.nodeId = null;
        this.nodeType = null;
    }

    /**
     * Create a new NodeExecutionException with cause.
     *
     * @param message the exception message
     * @param cause   the underlying cause
     */
    public NodeExecutionException(String message, Throwable cause) {
        super(message, cause);
        this.nodeId = null;
        this.nodeType = null;
    }

    /**
     * Create a new NodeExecutionException with node details.
     *
     * @param message  the exception message
     * @param nodeId   the ID of the failing node
     * @param nodeType the type of the failing node
     */
    public NodeExecutionException(String message, String nodeId, String nodeType) {
        super(message);
        this.nodeId = nodeId;
        this.nodeType = nodeType;
    }

    /**
     * Create a new NodeExecutionException with node details and cause.
     *
     * @param message  the exception message
     * @param nodeId   the ID of the failing node
     * @param nodeType the type of the failing node
     * @param cause    the underlying cause
     */
    public NodeExecutionException(String message, String nodeId, String nodeType, Throwable cause) {
        super(message, cause);
        this.nodeId = nodeId;
        this.nodeType = nodeType;
    }

    /**
     * Get the ID of the node that failed.
     *
     * @return the node ID, or null if not specified
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * Get the type of the node that failed.
     *
     * @return the node type, or null if not specified
     */
    public String getNodeType() {
        return nodeType;
    }
}
