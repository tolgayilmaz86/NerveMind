/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.canvas.validation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ai.nervemind.common.dto.WorkflowDTO;

/**
 * Validates workflows for errors and warnings.
 * 
 * <p>
 * This class provides stateless validation of workflow structure and
 * configuration. It checks for common issues like:
 * <ul>
 * <li>Empty workflows</li>
 * <li>Missing trigger nodes</li>
 * <li>Disconnected nodes</li>
 * <li>Missing required parameters</li>
 * </ul>
 * 
 * <p>
 * Usage:
 * 
 * <pre>
 * WorkflowValidator validator = new WorkflowValidator();
 * ValidationResult result = validator.validate(workflow);
 * if (result.hasErrors()) {
 *     // Handle errors
 * }
 * </pre>
 */
public class WorkflowValidator {

    /**
     * Validates a workflow and returns the validation result.
     *
     * @param workflow the workflow to validate
     * @return the validation result containing errors and warnings
     */
    public ValidationResult validate(WorkflowDTO workflow) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (workflow == null) {
            errors.add("Workflow is null");
            return new ValidationResult(errors, warnings);
        }

        // Check if workflow has nodes
        if (workflow.nodes() == null || workflow.nodes().isEmpty()) {
            errors.add("Workflow has no nodes");
            return new ValidationResult(errors, warnings);
        }

        // Check for trigger nodes
        boolean hasTrigger = workflow.nodes().stream()
                .anyMatch(n -> n.type() != null && n.type().toLowerCase().contains("trigger"));
        if (!hasTrigger) {
            warnings.add("Workflow has no trigger node - it can only be run manually");
        }

        // Check for disconnected nodes
        validateConnectedNodes(workflow, warnings);

        // Check for missing required parameters
        validateNodeParameters(workflow, errors, warnings);

        return new ValidationResult(errors, warnings);
    }

    private void validateConnectedNodes(WorkflowDTO workflow, List<String> warnings) {
        if (workflow.connections() == null || workflow.nodes().size() <= 1) {
            return;
        }

        Set<String> connectedNodes = new HashSet<>();
        for (var conn : workflow.connections()) {
            connectedNodes.add(conn.sourceNodeId());
            connectedNodes.add(conn.targetNodeId());
        }

        for (var node : workflow.nodes()) {
            if (!connectedNodes.contains(node.id())) {
                warnings.add("Node '" + node.name() + "' is not connected to any other node");
            }
        }
    }

    private void validateNodeParameters(WorkflowDTO workflow, List<String> errors, List<String> warnings) {
        for (var node : workflow.nodes()) {
            if (node.type() == null) {
                continue;
            }

            String nodeType = node.type().toLowerCase();
            validateHttpRequestNode(nodeType, node.name(), node.parameters(), errors);
            validateLlmChatNode(nodeType, node.name(), node.parameters(), warnings);
        }
    }

    private void validateHttpRequestNode(String nodeType, String nodeName, java.util.Map<String, Object> parameters,
            List<String> errors) {
        if (!nodeType.equals("httprequest") && !nodeType.equals("http_request")) {
            return;
        }
        if (isParameterMissing(parameters, "url")) {
            errors.add("HTTP Request node '" + nodeName + "' is missing URL");
        }
    }

    private void validateLlmChatNode(String nodeType, String nodeName, java.util.Map<String, Object> parameters,
            List<String> warnings) {
        if (!nodeType.equals("llmchat") && !nodeType.equals("llm_chat")) {
            return;
        }
        if (isParameterMissing(parameters, "prompt")) {
            warnings.add("LLM Chat node '" + nodeName + "' has no prompt configured");
        }
    }

    private boolean isParameterMissing(java.util.Map<String, Object> params, String paramName) {
        return params == null || !params.containsKey(paramName) ||
                params.get(paramName) == null || params.get(paramName).toString().isBlank();
    }

    /**
     * Result of workflow validation containing errors and warnings.
     *
     * @param errors   List of validation errors that prevent execution
     * @param warnings List of validation warnings that don't block execution but
     *                 suggest improvements
     */
    public record ValidationResult(List<String> errors, List<String> warnings) {

        /**
         * @return true if validation passed with no errors (warnings are allowed)
         */
        public boolean isValid() {
            return errors.isEmpty();
        }

        /**
         * @return true if there are any errors
         */
        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        /**
         * @return true if there are any warnings
         */
        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        /**
         * @return total count of issues (errors + warnings)
         */
        public int totalIssues() {
            return errors.size() + warnings.size();
        }
    }
}
