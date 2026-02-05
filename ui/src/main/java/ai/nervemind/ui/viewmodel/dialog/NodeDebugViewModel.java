/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.dialog;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import ai.nervemind.common.domain.Node;
import ai.nervemind.ui.console.ExecutionConsoleService.NodeExecutionState;
import ai.nervemind.ui.console.ExecutionConsoleService.NodeState;
import ai.nervemind.ui.viewmodel.BaseViewModel;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * ViewModel for the Node Debug dialog.
 * 
 * <p>
 * Manages the display of execution data for debugging a node.
 */
public class NodeDebugViewModel extends BaseViewModel {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final ObjectMapper jsonMapper;
    private final Node node;
    private final NodeExecutionState executionState;

    // Display properties
    private final StringProperty nodeName = new SimpleStringProperty();
    private final StringProperty nodeType = new SimpleStringProperty();
    private final StringProperty statusText = new SimpleStringProperty();
    private final StringProperty statusStyleClass = new SimpleStringProperty();
    private final ReadOnlyStringWrapper timestampText = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper durationText = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper inputDataText = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper outputDataText = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper errorText = new ReadOnlyStringWrapper();
    private final StringProperty statusIconCode = new SimpleStringProperty();

    /**
     * Creates a new NodeDebugViewModel.
     * 
     * @param node           the node being inspected
     * @param executionState the last execution state data
     */
    public NodeDebugViewModel(Node node, NodeExecutionState executionState) {
        this.jsonMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        this.node = node;
        this.executionState = executionState;

        nodeName.set(node.name());
        nodeType.set(node.type());
        updateStatus();
        updateTimestamp();
        updateDuration();
        updateInputData();
        updateOutputData();
        updateError();
    }

    // ===== Properties =====

    /**
     * The node name.
     *
     * @return the node name property
     */
    public StringProperty nodeNameProperty() {
        return nodeName;
    }

    public String getNodeName() {
        return nodeName.get();
    }

    /**
     * The node type.
     *
     * @return the node type property
     */
    public StringProperty nodeTypeProperty() {
        return nodeType;
    }

    public String getNodeType() {
        return nodeType.get();
    }

    /**
     * The status text.
     *
     * @return the status text property
     */
    public StringProperty statusTextProperty() {
        return statusText;
    }

    public String getStatusText() {
        return statusText.get();
    }

    /**
     * The status style class.
     *
     * @return the status style class property
     */
    public StringProperty statusStyleClassProperty() {
        return statusStyleClass;
    }

    public String getStatusStyleClass() {
        return statusStyleClass.get();
    }

    /**
     * The status icon code.
     *
     * @return the status icon code property
     */
    public StringProperty statusIconCodeProperty() {
        return statusIconCode;
    }

    public String getStatusIconCode() {
        return statusIconCode.get();
    }

    /**
     * The formatted timestamp text.
     *
     * @return the timestamp text property
     */
    public ReadOnlyStringProperty timestampTextProperty() {
        return timestampText.getReadOnlyProperty();
    }

    public String getTimestampText() {
        return timestampText.get();
    }

    /**
     * The formatted duration text.
     *
     * @return the duration text property
     */
    public ReadOnlyStringProperty durationTextProperty() {
        return durationText.getReadOnlyProperty();
    }

    public String getDurationText() {
        return durationText.get();
    }

    /**
     * The formatted input data text.
     *
     * @return the input data text property
     */
    public ReadOnlyStringProperty inputDataTextProperty() {
        return inputDataText.getReadOnlyProperty();
    }

    public String getInputDataText() {
        return inputDataText.get();
    }

    /**
     * The formatted output data text.
     *
     * @return the output data text property
     */
    public ReadOnlyStringProperty outputDataTextProperty() {
        return outputDataText.getReadOnlyProperty();
    }

    public String getOutputDataText() {
        return outputDataText.get();
    }

    /**
     * The error message text.
     *
     * @return the error text property
     */
    public ReadOnlyStringProperty errorTextProperty() {
        return errorText.getReadOnlyProperty();
    }

    public String getErrorText() {
        return errorText.get();
    }

    @Override
    public boolean hasError() {
        return executionState.state() == NodeState.FAILED && executionState.error() != null;
    }

    /**
     * Gets the node.
     * 
     * @return the node
     */
    public Node getNode() {
        return node;
    }

    /**
     * Gets the node execution state.
     *
     * @return the execution state
     */
    public NodeExecutionState getExecutionState() {
        return executionState;
    }

    // ===== Private Methods =====

    private void updateStatus() {
        switch (executionState.state()) {
            case SUCCESS -> {
                statusText.set("Success");
                statusStyleClass.set("node-debug-dialog__status-success");
                statusIconCode.set("mdi2c-check-circle");
            }
            case FAILED -> {
                statusText.set("Failed");
                statusStyleClass.set("node-debug-dialog__status-failed");
                statusIconCode.set("mdi2a-alert-circle");
            }
            case RUNNING -> {
                statusText.set("Running");
                statusStyleClass.set("node-debug-dialog__status-running");
                statusIconCode.set("mdi2c-clock-outline");
            }
            case SKIPPED -> {
                statusText.set("Skipped");
                statusStyleClass.set("node-debug-dialog__status-skipped");
                statusIconCode.set("mdi2c-cancel");
            }
            default -> {
                statusText.set("Idle");
                statusStyleClass.set("node-debug-dialog__status-skipped");
                statusIconCode.set("mdi2c-clock-outline");
            }
        }
    }

    private void updateTimestamp() {
        String timestamp = "N/A";
        if (executionState.startTime() > 0) {
            timestamp = TIME_FORMATTER.format(Instant.ofEpochMilli(executionState.startTime()));
        }
        timestampText.set("Last Executed: " + timestamp);
    }

    private void updateDuration() {
        if (executionState.durationMs() > 0) {
            durationText.set("(" + executionState.durationMs() + " ms)");
        } else {
            durationText.set("");
        }
    }

    private void updateInputData() {
        inputDataText.set(formatData(executionState.inputData()));
    }

    private void updateOutputData() {
        outputDataText.set(formatData(executionState.outputData()));
    }

    private void updateError() {
        if (hasError()) {
            errorText.set(executionState.error());
        } else {
            errorText.set("");
        }
    }

    private String formatData(Object data) {
        if (data == null) {
            return "(no data)";
        }
        try {
            String json = jsonMapper.writeValueAsString(data);
            return unescapeNewlinesInStrings(json);
        } catch (Exception _) {
            return data.toString();
        }
    }

    /**
     * Process JSON to unescape newlines within string values for display.
     */
    private String unescapeNewlinesInStrings(String json) {
        StringBuilder result = new StringBuilder();
        boolean inString = false;
        boolean escape = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (escape) {
                if (c == 'n' && inString) {
                    result.append('\n');
                } else if (c == 't' && inString) {
                    result.append('\t');
                } else {
                    result.append('\\').append(c);
                }
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else {
                if (c == '"') {
                    inString = !inString;
                }
                result.append(c);
            }
        }

        return result.toString();
    }
}
