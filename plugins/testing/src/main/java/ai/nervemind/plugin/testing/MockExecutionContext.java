package ai.nervemind.plugin.testing;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock execution context for testing plugin executions.
 * 
 * <p>
 * Provides controllable implementations of the execution context
 * including inputs, outputs, variables, and credentials.
 * </p>
 * 
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * MockExecutionContext context = new MockExecutionContext()
 *         .withInput("name", "World")
 *         .withVariable("apiKey", "secret")
 *         .withWorkflowId("test-workflow")
 *         .withExecutionId("test-execution");
 * 
 * Map<String, Object> result = plugin.execute(config, context.getInputs(), context);
 * 
 * assertThat(context.getOutputs()).containsEntry("message", "Hello, World!");
 * }</pre>
 */
public class MockExecutionContext {

    // Inputs
    private final Map<String, Object> inputs = new ConcurrentHashMap<>();

    // Outputs (captured during execution)
    private final Map<String, Object> outputs = new ConcurrentHashMap<>();

    // Variables
    private final Map<String, Object> variables = new ConcurrentHashMap<>();

    // Credentials
    private final Map<String, Object> credentials = new ConcurrentHashMap<>();

    // Execution metadata
    private String workflowId = "test-workflow";
    private String executionId = UUID.randomUUID().toString();
    private String nodeId = "test-node";
    private String nodeType = "test-plugin";
    private Instant executionStart = Instant.now();
    private volatile boolean cancelled = false;

    // State tracking
    private int executionCount = 0;
    private final List<String> stateChanges = Collections.synchronizedList(new ArrayList<>());

    // ========== Builder Methods ==========

    /**
     * Add an input value.
     */
    public MockExecutionContext withInput(String key, Object value) {
        inputs.put(key, value);
        return this;
    }

    /**
     * Add multiple inputs.
     */
    public MockExecutionContext withInputs(Map<String, Object> inputMap) {
        inputs.putAll(inputMap);
        return this;
    }

    /**
     * Add a variable.
     */
    public MockExecutionContext withVariable(String key, Object value) {
        variables.put(key, value);
        return this;
    }

    /**
     * Add multiple variables.
     */
    public MockExecutionContext withVariables(Map<String, Object> vars) {
        variables.putAll(vars);
        return this;
    }

    /**
     * Add a credential.
     */
    public MockExecutionContext withCredential(String key, Object value) {
        credentials.put(key, value);
        return this;
    }

    /**
     * Add multiple credentials.
     */
    public MockExecutionContext withCredentials(Map<String, Object> creds) {
        credentials.putAll(creds);
        return this;
    }

    /**
     * Set workflow ID.
     */
    public MockExecutionContext withWorkflowId(String workflowId) {
        this.workflowId = workflowId;
        return this;
    }

    /**
     * Set execution ID.
     */
    public MockExecutionContext withExecutionId(String executionId) {
        this.executionId = executionId;
        return this;
    }

    /**
     * Set node ID.
     */
    public MockExecutionContext withNodeId(String nodeId) {
        this.nodeId = nodeId;
        return this;
    }

    /**
     * Set node type.
     */
    public MockExecutionContext withNodeType(String nodeType) {
        this.nodeType = nodeType;
        return this;
    }

    // ========== Getters ==========

    /**
     * Get all inputs.
     */
    public Map<String, Object> getInputs() {
        return new HashMap<>(inputs);
    }

    /**
     * Get all outputs.
     */
    public Map<String, Object> getOutputs() {
        return new HashMap<>(outputs);
    }

    /**
     * Get all variables.
     */
    public Map<String, Object> getVariables() {
        return new HashMap<>(variables);
    }

    /**
     * Get all credentials.
     */
    public Map<String, Object> getCredentials() {
        return new HashMap<>(credentials);
    }

    /**
     * Get workflow ID.
     */
    public String getWorkflowId() {
        return workflowId;
    }

    /**
     * Get execution ID.
     */
    public String getExecutionId() {
        return executionId;
    }

    /**
     * Get node ID.
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * Get node type.
     */
    public String getNodeType() {
        return nodeType;
    }

    /**
     * Get execution start time.
     */
    public Instant getExecutionStart() {
        return executionStart;
    }

    /**
     * Get execution count.
     */
    public int getExecutionCount() {
        return executionCount;
    }

    /**
     * Get state changes.
     */
    public List<String> getStateChanges() {
        return new ArrayList<>(stateChanges);
    }

    // ========== Execution Context Methods ==========

    /**
     * Capture an output value.
     */
    public void setOutput(String key, Object value) {
        outputs.put(key, value);
    }

    /**
     * Get an input value.
     */
    public Object getInput(String key) {
        return inputs.get(key);
    }

    /**
     * Get an input value with default.
     */
    public Object getInput(String key, Object defaultValue) {
        return inputs.getOrDefault(key, defaultValue);
    }

    /**
     * Get a variable.
     */
    public Object getVariable(String key) {
        return variables.get(key);
    }

    /**
     * Set a variable.
     */
    public void setVariable(String key, Object value) {
        variables.put(key, value);
        stateChanges.add("Variable " + key + " = " + value);
    }

    /**
     * Get a credential.
     */
    public Object getCredential(String key) {
        return credentials.get(key);
    }

    /**
     * Check if cancelled.
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Cancel execution.
     */
    public void cancel() {
        this.cancelled = true;
        stateChanges.add("Execution cancelled");
    }

    /**
     * Increment execution count.
     */
    public void incrementExecutionCount() {
        this.executionCount++;
    }

    // ========== Assertion Helpers ==========

    /**
     * Assert that an output exists.
     */
    public MockExecutionContext assertHasOutput(String key) {
        if (!outputs.containsKey(key)) {
            throw new AssertionError("Expected output key '" + key + "' but not found. " +
                    "Available keys: " + outputs.keySet());
        }
        return this;
    }

    /**
     * Assert that an output has a specific value.
     */
    public MockExecutionContext assertOutputEquals(String key, Object expected) {
        assertHasOutput(key);
        if (!Objects.equals(outputs.get(key), expected)) {
            throw new AssertionError("Expected output '" + key + "' to equal '" + expected +
                    "' but was '" + outputs.get(key) + "'");
        }
        return this;
    }

    /**
     * Assert that an input exists.
     */
    public MockExecutionContext assertHasInput(String key) {
        if (!inputs.containsKey(key)) {
            throw new AssertionError("Expected input key '" + key + "' but not found");
        }
        return this;
    }

    /**
     * Assert that an input has a specific value.
     */
    public MockExecutionContext assertInputEquals(String key, Object expected) {
        assertHasInput(key);
        if (!Objects.equals(inputs.get(key), expected)) {
            throw new AssertionError("Expected input '" + key + "' to equal '" + expected +
                    "' but was '" + inputs.get(key) + "'");
        }
        return this;
    }

    /**
     * Assert execution count.
     */
    public MockExecutionContext assertExecutionCount(int expected) {
        if (executionCount != expected) {
            throw new AssertionError("Expected execution count " + expected +
                    " but was " + executionCount);
        }
        return this;
    }

    // ========== Reset ==========

    /**
     * Reset for reuse.
     */
    public MockExecutionContext reset() {
        inputs.clear();
        outputs.clear();
        variables.clear();
        credentials.clear();
        executionCount = 0;
        stateChanges.clear();
        cancelled = false;
        executionStart = Instant.now();
        return this;
    }

    /**
     * Create a copy.
     */
    public MockExecutionContext copy() {
        return new MockExecutionContext()
                .withInputs(inputs)
                .withVariables(variables)
                .withCredentials(credentials)
                .withWorkflowId(workflowId)
                .withExecutionId(executionId)
                .withNodeId(nodeId)
                .withNodeType(nodeType);
    }
}
