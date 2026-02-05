/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.canvas.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ai.nervemind.common.domain.Connection;
import ai.nervemind.common.domain.Node;
import ai.nervemind.common.dto.WorkflowDTO;
import ai.nervemind.common.enums.TriggerType;

/**
 * Unit tests for {@link WorkflowValidator}.
 * 
 * <p>
 * Tests workflow validation logic for errors and warnings.
 */
@DisplayName("WorkflowValidator")
class WorkflowValidatorTest {

    private WorkflowValidator validator;

    @BeforeEach
    void setUp() {
        validator = new WorkflowValidator();
    }

    @Nested
    @DisplayName("Basic Validation")
    class BasicValidationTests {

        @Test
        @DisplayName("should return error for null workflow")
        void shouldReturnErrorForNullWorkflow() {
            var result = validator.validate(null);

            assertTrue(result.hasErrors());
            assertEquals(1, result.errors().size());
            assertTrue(result.errors().get(0).contains("null"));
        }

        @Test
        @DisplayName("should return error for empty workflow")
        void shouldReturnErrorForEmptyWorkflow() {
            WorkflowDTO workflow = createWorkflow(List.of(), List.of());

            var result = validator.validate(workflow);

            assertTrue(result.hasErrors());
            assertTrue(result.errors().stream().anyMatch(e -> e.contains("no nodes")));
        }

        @Test
        @DisplayName("should pass validation for valid workflow with trigger")
        void shouldPassValidationForValidWorkflowWithTrigger() {
            Node trigger = createNode("1", "manualTrigger", "Start");
            Node action = createNode("2", "httpRequest", "API Call", Map.of("url", "https://api.example.com"));
            Connection conn = createConnection("1", "2");
            WorkflowDTO workflow = createWorkflow(List.of(trigger, action), List.of(conn));

            var result = validator.validate(workflow);

            assertTrue(result.isValid(), "Expected valid result but got errors: " + result.errors());
            assertTrue(result.warnings().isEmpty(),
                    "Expected no warnings but got: " + result.warnings());
        }
    }

    @Nested
    @DisplayName("Trigger Validation")
    class TriggerValidationTests {

        @Test
        @DisplayName("should warn when no trigger node present")
        void shouldWarnWhenNoTriggerNodePresent() {
            Node action = createNode("1", "httpRequest", "API Call", Map.of("url", "https://api.example.com"));
            WorkflowDTO workflow = createWorkflow(List.of(action), List.of());

            var result = validator.validate(workflow);

            assertTrue(result.isValid()); // Warnings don't make it invalid
            assertTrue(result.hasWarnings());
            assertTrue(result.warnings().stream().anyMatch(w -> w.contains("no trigger")));
        }

        @Test
        @DisplayName("should not warn when trigger node present")
        void shouldNotWarnWhenTriggerNodePresent() {
            Node trigger = createNode("1", "scheduleTrigger", "Daily Run");
            WorkflowDTO workflow = createWorkflow(List.of(trigger), List.of());

            var result = validator.validate(workflow);

            assertFalse(result.warnings().stream().anyMatch(w -> w.contains("no trigger")));
        }

        @Test
        @DisplayName("should detect trigger in different case")
        void shouldDetectTriggerInDifferentCase() {
            Node trigger = createNode("1", "WebhookTrigger", "Webhook");
            WorkflowDTO workflow = createWorkflow(List.of(trigger), List.of());

            var result = validator.validate(workflow);

            assertFalse(result.warnings().stream().anyMatch(w -> w.contains("no trigger")));
        }
    }

    @Nested
    @DisplayName("Connection Validation")
    class ConnectionValidationTests {

        @Test
        @DisplayName("should warn about disconnected nodes")
        void shouldWarnAboutDisconnectedNodes() {
            Node node1 = createNode("1", "manualTrigger", "Start");
            Node node2 = createNode("2", "code", "Process");
            Node node3 = createNode("3", "set", "Isolated"); // Not connected
            Connection conn = createConnection("1", "2");
            WorkflowDTO workflow = createWorkflow(List.of(node1, node2, node3), List.of(conn));

            var result = validator.validate(workflow);

            assertTrue(result.hasWarnings());
            assertTrue(result.warnings().stream().anyMatch(w -> w.contains("Isolated")));
        }

        @Test
        @DisplayName("should not warn about disconnected single node")
        void shouldNotWarnAboutDisconnectedSingleNode() {
            Node node = createNode("1", "manualTrigger", "Start");
            WorkflowDTO workflow = createWorkflow(List.of(node), List.of());

            var result = validator.validate(workflow);

            assertFalse(result.warnings().stream().anyMatch(w -> w.contains("not connected")));
        }

        @Test
        @DisplayName("should handle null connections list")
        void shouldHandleNullConnectionsList() {
            Node node1 = createNode("1", "manualTrigger", "Start");
            Node node2 = createNode("2", "code", "Process");
            WorkflowDTO workflow = new WorkflowDTO(
                    1L, "Test", "Test workflow",
                    List.of(node1, node2),
                    null, // null connections
                    Map.of(), false, TriggerType.MANUAL, null,
                    Instant.now(), Instant.now(), null, 1);

            var result = validator.validate(workflow);

            // Should not throw, but may have warnings about connections
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("HTTP Request Validation")
    class HttpRequestValidationTests {

        @Test
        @DisplayName("should error when HTTP Request missing URL")
        void shouldErrorWhenHttpRequestMissingUrl() {
            Node trigger = createNode("1", "manualTrigger", "Start");
            Node http = createNode("2", "httpRequest", "API Call", Map.of()); // No URL
            Connection conn = createConnection("1", "2");
            WorkflowDTO workflow = createWorkflow(List.of(trigger, http), List.of(conn));

            var result = validator.validate(workflow);

            assertTrue(result.hasErrors());
            assertTrue(result.errors().stream().anyMatch(e -> e.contains("missing URL")));
        }

        @Test
        @DisplayName("should error when HTTP Request has blank URL")
        void shouldErrorWhenHttpRequestHasBlankUrl() {
            Node trigger = createNode("1", "manualTrigger", "Start");
            Node http = createNode("2", "httpRequest", "API Call", Map.of("url", "  "));
            Connection conn = createConnection("1", "2");
            WorkflowDTO workflow = createWorkflow(List.of(trigger, http), List.of(conn));

            var result = validator.validate(workflow);

            assertTrue(result.hasErrors());
            assertTrue(result.errors().stream().anyMatch(e -> e.contains("missing URL")));
        }

        @Test
        @DisplayName("should pass when HTTP Request has valid URL")
        void shouldPassWhenHttpRequestHasValidUrl() {
            Node trigger = createNode("1", "manualTrigger", "Start");
            Node http = createNode("2", "httpRequest", "API Call", Map.of("url", "https://example.com"));
            Connection conn = createConnection("1", "2");
            WorkflowDTO workflow = createWorkflow(List.of(trigger, http), List.of(conn));

            var result = validator.validate(workflow);

            assertFalse(result.errors().stream().anyMatch(e -> e.contains("missing URL")));
        }

        @Test
        @DisplayName("should validate http_request snake case type")
        void shouldValidateHttpRequestSnakeCaseType() {
            Node trigger = createNode("1", "manualTrigger", "Start");
            Node http = createNode("2", "http_request", "API Call", Map.of()); // Snake case
            Connection conn = createConnection("1", "2");
            WorkflowDTO workflow = createWorkflow(List.of(trigger, http), List.of(conn));

            var result = validator.validate(workflow);

            assertTrue(result.hasErrors());
            assertTrue(result.errors().stream().anyMatch(e -> e.contains("missing URL")));
        }
    }

    @Nested
    @DisplayName("LLM Chat Validation")
    class LlmChatValidationTests {

        @Test
        @DisplayName("should warn when LLM Chat missing prompt")
        void shouldWarnWhenLlmChatMissingPrompt() {
            Node trigger = createNode("1", "manualTrigger", "Start");
            Node llm = createNode("2", "llmChat", "AI", Map.of()); // No prompt
            Connection conn = createConnection("1", "2");
            WorkflowDTO workflow = createWorkflow(List.of(trigger, llm), List.of(conn));

            var result = validator.validate(workflow);

            assertTrue(result.hasWarnings());
            assertTrue(result.warnings().stream().anyMatch(w -> w.contains("no prompt")));
        }

        @Test
        @DisplayName("should pass when LLM Chat has prompt")
        void shouldPassWhenLlmChatHasPrompt() {
            Node trigger = createNode("1", "manualTrigger", "Start");
            Node llm = createNode("2", "llmChat", "AI", Map.of("prompt", "Hello"));
            Connection conn = createConnection("1", "2");
            WorkflowDTO workflow = createWorkflow(List.of(trigger, llm), List.of(conn));

            var result = validator.validate(workflow);

            assertFalse(result.warnings().stream().anyMatch(w -> w.contains("no prompt")));
        }

        @Test
        @DisplayName("should validate llm_chat snake case type")
        void shouldValidateLlmChatSnakeCaseType() {
            Node trigger = createNode("1", "manualTrigger", "Start");
            Node llm = createNode("2", "llm_chat", "AI", Map.of()); // Snake case
            Connection conn = createConnection("1", "2");
            WorkflowDTO workflow = createWorkflow(List.of(trigger, llm), List.of(conn));

            var result = validator.validate(workflow);

            assertTrue(result.hasWarnings());
            assertTrue(result.warnings().stream().anyMatch(w -> w.contains("no prompt")));
        }
    }

    @Nested
    @DisplayName("ValidationResult")
    class ValidationResultTests {

        @Test
        @DisplayName("should correctly report valid result")
        void shouldCorrectlyReportValidResult() {
            var result = new WorkflowValidator.ValidationResult(List.of(), List.of());

            assertTrue(result.isValid());
            assertFalse(result.hasErrors());
            assertFalse(result.hasWarnings());
            assertEquals(0, result.totalIssues());
        }

        @Test
        @DisplayName("should correctly report result with only warnings")
        void shouldCorrectlyReportResultWithOnlyWarnings() {
            var result = new WorkflowValidator.ValidationResult(
                    List.of(),
                    List.of("Warning 1", "Warning 2"));

            assertTrue(result.isValid()); // Warnings don't invalidate
            assertFalse(result.hasErrors());
            assertTrue(result.hasWarnings());
            assertEquals(2, result.totalIssues());
        }

        @Test
        @DisplayName("should correctly report result with errors")
        void shouldCorrectlyReportResultWithErrors() {
            var result = new WorkflowValidator.ValidationResult(
                    List.of("Error 1"),
                    List.of("Warning 1"));

            assertFalse(result.isValid());
            assertTrue(result.hasErrors());
            assertTrue(result.hasWarnings());
            assertEquals(2, result.totalIssues());
        }
    }

    // ========== Helper Methods ==========

    private Node createNode(String id, String type, String name) {
        return createNode(id, type, name, Map.of());
    }

    private Node createNode(String id, String type, String name, Map<String, Object> params) {
        return new Node(id, type, name, new Node.Position(100, 100), params, null, false, "");
    }

    private Connection createConnection(String sourceId, String targetId) {
        return new Connection(
                sourceId + "-" + targetId, // id
                sourceId, // sourceNodeId
                "main", // sourceOutput
                targetId, // targetNodeId
                "main"); // targetInput
    }

    private WorkflowDTO createWorkflow(List<Node> nodes, List<Connection> connections) {
        return new WorkflowDTO(
                1L,
                "Test Workflow",
                "Test workflow description",
                nodes,
                connections,
                Map.of(),
                false,
                TriggerType.MANUAL,
                null,
                Instant.now(),
                Instant.now(),
                null,
                1);
    }
}
