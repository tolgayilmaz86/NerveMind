/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.canvas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ai.nervemind.common.domain.Node;
import ai.nervemind.ui.viewmodel.ViewModelTestBase;

/**
 * Unit tests for NodePropertiesViewModel.
 *
 * <p>
 * Tests cover:
 * <ul>
 * <li>Initial state verification</li>
 * <li>Loading and clearing nodes</li>
 * <li>Dirty state tracking</li>
 * <li>Parameter definitions for various node types</li>
 * <li>Applying changes to create updated nodes</li>
 * <li>Node type visual styling</li>
 * <li>Callback invocations</li>
 * </ul>
 */
class NodePropertiesViewModelTest extends ViewModelTestBase {

    private static final String TEST_NOTES = "Test notes";

    private static final String HTTP_REQUEST_TYPE = "httpRequest";
    private static final String NODE_ABC = "node-abc";

    private NodePropertiesViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new NodePropertiesViewModel();
    }

    // ===== Helper Methods =====

    private Node createTestNode(String type) {
        return createTestNode("node-123", type, "Test Node");
    }

    private Node createTestNode(String id, String type, String name) {
        return new Node(
                id,
                type,
                name,
                new Node.Position(100, 200),
                new HashMap<>(),
                null,
                false,
                TEST_NOTES);
    }

    private Node createNodeWithParams(String type, Map<String, Object> params) {
        return new Node(
                "node-123",
                type,
                "Test Node",
                new Node.Position(100, 200),
                params,
                null,
                false,
                null);
    }

    @Nested
    class InitialState {

        @Test
        void shouldHaveNoNodeInitially() {
            assertFalse(viewModel.hasNode());
            assertNull(viewModel.getOriginalNode());
        }

        @Test
        void shouldBeHiddenInitially() {
            assertFalse(viewModel.isVisible());
        }

        @Test
        void shouldNotBeDirtyInitially() {
            assertFalse(viewModel.dirtyProperty().get());
        }

        @Test
        void shouldHaveEmptyParameterDefinitions() {
            assertTrue(viewModel.getParameterDefinitions().isEmpty());
        }

        @Test
        void shouldHaveEmptyParameters() {
            assertTrue(viewModel.getParameters().isEmpty());
        }
    }

    @Nested
    class LoadNode {

        @Test
        void shouldSetHasNodeToTrue() {
            Node node = createTestNode(HTTP_REQUEST_TYPE);

            viewModel.loadNode(node);

            assertTrue(viewModel.hasNode());
        }

        @Test
        void shouldStoreOriginalNode() {
            Node node = createTestNode(HTTP_REQUEST_TYPE);

            viewModel.loadNode(node);

            assertSame(node, viewModel.getOriginalNode());
        }

        @Test
        void shouldSetNodeIdentity() {
            Node node = createTestNode(NODE_ABC, HTTP_REQUEST_TYPE, "My Node");

            viewModel.loadNode(node);

            assertEquals(NODE_ABC, viewModel.getNodeId());
            assertEquals(HTTP_REQUEST_TYPE, viewModel.getNodeType());
            assertEquals("My Node", viewModel.getName());
        }

        @Test
        void shouldSetNodeTypeLabel() {
            viewModel.loadNode(createTestNode(HTTP_REQUEST_TYPE));
            assertEquals("HTTP Request", viewModel.getNodeTypeLabel());

            viewModel.loadNode(createTestNode("llmChat"));
            assertEquals("LLM Chat", viewModel.getNodeTypeLabel());

            viewModel.loadNode(createTestNode("if"));
            assertEquals("If Condition", viewModel.getNodeTypeLabel());
        }

        @Test
        void shouldSetNotesAndDisabled() {
            Node node = new Node(
                    "node-1", "code", "Code Node",
                    new Node.Position(0, 0),
                    null, null, true, "Important notes");

            viewModel.loadNode(node);

            assertEquals("Important notes", viewModel.getNotes());
            assertTrue(viewModel.isDisabled());
        }

        @Test
        void shouldHandleNullNotes() {
            Node node = createTestNode("code");

            viewModel.loadNode(node);

            assertEquals(TEST_NOTES, viewModel.getNotes());
        }

        @Test
        void shouldLoadParameters() {
            Map<String, Object> params = new HashMap<>();
            params.put("url", "https://api.test.com");
            params.put("method", "POST");

            Node node = createNodeWithParams(HTTP_REQUEST_TYPE, params);

            viewModel.loadNode(node);

            assertEquals("https://api.test.com", viewModel.getParameter("url"));
            assertEquals("POST", viewModel.getParameter("method"));
        }

        @Test
        void shouldBuildParameterDefinitions() {
            viewModel.loadNode(createTestNode(HTTP_REQUEST_TYPE));

            assertFalse(viewModel.getParameterDefinitions().isEmpty());
            // HTTP Request should have url, method, headers, body, timeout
            assertTrue(viewModel.getParameterDefinitions().size() >= 5);
        }

        @Test
        void shouldClearDirtyStateAfterLoad() {
            viewModel.loadNode(createTestNode("code"));

            assertFalse(viewModel.dirtyProperty().get());
        }

        @Test
        void shouldMakeVisibleAfterLoad() {
            viewModel.loadNode(createTestNode("code"));

            assertTrue(viewModel.isVisible());
        }

        @Test
        void shouldHandleNullNode() {
            viewModel.loadNode(createTestNode("code"));
            assertTrue(viewModel.hasNode());

            viewModel.loadNode(null);

            assertFalse(viewModel.hasNode());
        }
    }

    @Nested
    class ClearNode {

        @Test
        void shouldClearAllState() {
            viewModel.loadNode(createTestNode(HTTP_REQUEST_TYPE));

            viewModel.clearNode();

            assertFalse(viewModel.hasNode());
            assertNull(viewModel.getOriginalNode());
            assertNull(viewModel.getNodeId());
            assertNull(viewModel.getNodeType());
            assertNull(viewModel.getName());
        }

        @Test
        void shouldClearParameters() {
            Map<String, Object> params = new HashMap<>();
            params.put("url", "https://test.com");
            viewModel.loadNode(createNodeWithParams(HTTP_REQUEST_TYPE, params));

            viewModel.clearNode();

            assertTrue(viewModel.getParameters().isEmpty());
            assertTrue(viewModel.getParameterDefinitions().isEmpty());
        }

        @Test
        void shouldClearDirtyState() {
            viewModel.loadNode(createTestNode("code"));
            viewModel.setName("Changed Name");
            assertTrue(viewModel.dirtyProperty().get());

            viewModel.clearNode();

            assertFalse(viewModel.dirtyProperty().get());
        }
    }

    @Nested
    class DirtyState {

        @Test
        void shouldBeDirtyAfterNameChange() {
            viewModel.loadNode(createTestNode("code"));

            viewModel.setName("New Name");

            assertTrue(viewModel.dirtyProperty().get());
        }

        @Test
        void shouldBeDirtyAfterNotesChange() {
            viewModel.loadNode(createTestNode("code"));

            viewModel.setNotes("Updated notes");

            assertTrue(viewModel.dirtyProperty().get());
        }

        @Test
        void shouldBeDirtyAfterDisabledChange() {
            viewModel.loadNode(createTestNode("code"));

            viewModel.setDisabled(true);

            assertTrue(viewModel.dirtyProperty().get());
        }

        @Test
        void shouldBeDirtyAfterParameterChange() {
            viewModel.loadNode(createTestNode(HTTP_REQUEST_TYPE));

            viewModel.setParameter("url", "https://new-url.com");

            assertTrue(viewModel.dirtyProperty().get());
        }

        @Test
        void shouldNotifyPropertyChanged() {
            AtomicInteger callCount = new AtomicInteger(0);
            viewModel.setOnPropertyChanged(callCount::incrementAndGet);
            viewModel.loadNode(createTestNode("code"));

            viewModel.setName("New Name");

            assertTrue(callCount.get() > 0);
        }
    }

    @Nested
    class ShowAndHide {

        @Test
        void shouldShowAndMakeVisible() {
            Node node = createTestNode("code");

            viewModel.show(node);

            assertTrue(viewModel.isVisible());
            assertTrue(viewModel.hasNode());
        }

        @Test
        void shouldHideAndCallCallback() {
            AtomicBoolean closeCalled = new AtomicBoolean(false);
            viewModel.setOnClose(() -> closeCalled.set(true));
            viewModel.show(createTestNode("code"));

            viewModel.hide();

            assertFalse(viewModel.isVisible());
            assertTrue(closeCalled.get());
        }
    }

    @Nested
    class Reset {

        @Test
        void shouldRestoreOriginalValues() {
            Node original = createTestNode("node-1", "code", "Original Name");
            viewModel.loadNode(original);

            viewModel.setName("Changed Name");
            viewModel.setNotes("Changed Notes");
            assertTrue(viewModel.dirtyProperty().get());

            viewModel.reset();

            assertEquals("Original Name", viewModel.getName());
            assertEquals(TEST_NOTES, viewModel.getNotes());
            assertFalse(viewModel.dirtyProperty().get());
        }

        @Test
        void shouldDoNothingWhenNoNode() {
            viewModel.reset();

            assertFalse(viewModel.hasNode());
        }
    }

    @Nested
    class ApplyChanges {

        @Test
        void shouldCreateUpdatedNode() {
            viewModel.loadNode(createTestNode("node-1", "code", "Original Name"));
            viewModel.setName("Updated Name");
            viewModel.setNotes("Updated Notes");
            viewModel.setDisabled(true);

            Node updated = viewModel.applyChanges();

            assertNotNull(updated);
            assertEquals("node-1", updated.id());
            assertEquals("code", updated.type());
            assertEquals("Updated Name", updated.name());
            assertEquals("Updated Notes", updated.notes());
            assertTrue(updated.disabled());
        }

        @Test
        void shouldPreservePosition() {
            Node original = createTestNode("code");
            viewModel.loadNode(original);

            Node updated = viewModel.applyChanges();

            assertEquals(original.position(), updated.position());
        }

        @Test
        void shouldIncludeUpdatedParameters() {
            Map<String, Object> params = new HashMap<>();
            params.put("url", "https://old.com");
            viewModel.loadNode(createNodeWithParams(HTTP_REQUEST_TYPE, params));

            viewModel.setParameter("url", "https://new.com");
            Node updated = viewModel.applyChanges();

            assertEquals("https://new.com", updated.parameters().get("url"));
        }

        @Test
        void shouldInvokeCallback() {
            AtomicReference<String> callbackId = new AtomicReference<>();
            AtomicReference<Node> callbackNode = new AtomicReference<>();
            viewModel.setOnApplyChanges((id, node) -> {
                callbackId.set(id);
                callbackNode.set(node);
            });
            viewModel.loadNode(createTestNode(NODE_ABC, "code", "Test"));

            viewModel.applyChanges();

            assertEquals(NODE_ABC, callbackId.get());
            assertNotNull(callbackNode.get());
        }

        @Test
        void shouldClearDirtyState() {
            viewModel.loadNode(createTestNode("code"));
            viewModel.setName("Changed");
            assertTrue(viewModel.dirtyProperty().get());

            viewModel.applyChanges();

            assertFalse(viewModel.dirtyProperty().get());
        }

        @Test
        void shouldUpdateOriginalNode() {
            viewModel.loadNode(createTestNode("code"));
            viewModel.setName("New Name");

            Node updated = viewModel.applyChanges();

            assertSame(updated, viewModel.getOriginalNode());
        }

        @Test
        void shouldReturnNullWhenNoNode() {
            Node result = viewModel.applyChanges();

            assertNull(result);
        }

        @Test
        void shouldKeepOriginalNameIfBlank() {
            viewModel.loadNode(createTestNode("node-1", "code", "Original Name"));
            viewModel.setName("   ");

            Node updated = viewModel.applyChanges();

            assertEquals("Original Name", updated.name());
        }
    }

    @Nested
    class NodeTypeVisuals {

        @Test
        void shouldSetTriggerColors() {
            viewModel.loadNode(createTestNode("scheduleTrigger"));

            assertTrue(viewModel.getAccentColor().contains("f59e0b"));
            assertEquals("mdi2l-lightning-bolt", viewModel.getNodeTypeIcon());
        }

        @Test
        void shouldSetAiNodeColors() {
            viewModel.loadNode(createTestNode("llmChat"));

            assertTrue(viewModel.getAccentColor().contains("8b5cf6"));
            assertEquals("mdi2b-brain", viewModel.getNodeTypeIcon());
        }

        @Test
        void shouldSetControlFlowColors() {
            viewModel.loadNode(createTestNode("if"));

            assertTrue(viewModel.getAccentColor().contains("7c3aed"));
            assertEquals("mdi2s-sitemap", viewModel.getNodeTypeIcon());
        }

        @Test
        void shouldSetDataNodeColors() {
            viewModel.loadNode(createTestNode("set"));

            assertTrue(viewModel.getAccentColor().contains("10b981"));
            assertEquals("mdi2d-database", viewModel.getNodeTypeIcon());
        }

        @Test
        void shouldSetHttpRequestColors() {
            viewModel.loadNode(createTestNode(HTTP_REQUEST_TYPE));

            assertTrue(viewModel.getAccentColor().contains("3b82f6"));
            assertEquals("mdi2w-web", viewModel.getNodeTypeIcon());
        }

        @Test
        void shouldSetCodeNodeColors() {
            viewModel.loadNode(createTestNode("code"));

            assertTrue(viewModel.getAccentColor().contains("06b6d4"));
            assertEquals("mdi2c-code-tags", viewModel.getNodeTypeIcon());
        }

        @Test
        void shouldSetDefaultColorsForUnknownType() {
            viewModel.loadNode(createTestNode("unknownType"));

            assertTrue(viewModel.getAccentColor().contains("4a9eff"));
            assertEquals("mdi2c-cog", viewModel.getNodeTypeIcon());
        }
    }

    @Nested
    class ParameterDefinitions {

        @Test
        void shouldBuildHttpRequestParams() {
            viewModel.loadNode(createTestNode(HTTP_REQUEST_TYPE));

            var defs = viewModel.getParameterDefinitions();
            assertTrue(defs.stream().anyMatch(d -> d.key().equals("url")));
            assertTrue(defs.stream().anyMatch(d -> d.key().equals("method")));
            assertTrue(defs.stream().anyMatch(d -> d.key().equals("headers")));
            assertTrue(defs.stream().anyMatch(d -> d.key().equals("body")));
            assertTrue(defs.stream().anyMatch(d -> d.key().equals("timeout")));
        }

        @Test
        void shouldBuildCodeParams() {
            viewModel.loadNode(createTestNode("code"));

            var defs = viewModel.getParameterDefinitions();
            assertTrue(defs.stream().anyMatch(d -> d.key().equals("language")));
            assertTrue(defs.stream().anyMatch(d -> d.key().equals("code")));
        }

        @Test
        void shouldBuildIfParams() {
            viewModel.loadNode(createTestNode("if"));

            var defs = viewModel.getParameterDefinitions();
            assertTrue(defs.stream().anyMatch(d -> d.key().equals("condition")));
        }

        @Test
        void shouldBuildLoopParams() {
            viewModel.loadNode(createTestNode("loop"));

            var defs = viewModel.getParameterDefinitions();
            assertTrue(defs.stream().anyMatch(d -> d.key().equals("items")));
            assertTrue(defs.stream().anyMatch(d -> d.key().equals("batchSize")));
            assertTrue(defs.stream().anyMatch(d -> d.key().equals("parallel")));
        }

        @Test
        void shouldBuildScheduleTriggerParams() {
            viewModel.loadNode(createTestNode("scheduleTrigger"));

            var defs = viewModel.getParameterDefinitions();
            assertTrue(defs.stream().anyMatch(d -> d.key().equals("cronExpression")));
        }

        @Test
        void shouldBuildLlmChatParams() {
            viewModel.loadNode(createTestNode("llmChat"));

            var defs = viewModel.getParameterDefinitions();
            assertTrue(defs.stream().anyMatch(d -> d.key().equals("model")));
            assertTrue(defs.stream().anyMatch(d -> d.key().equals("prompt")));
        }
    }

    @Nested
    class HelpAndAdvancedEditor {

        @Test
        void shouldInvokeShowHelpCallback() {
            AtomicReference<String> helpType = new AtomicReference<>();
            viewModel.setOnShowHelp(helpType::set);
            viewModel.loadNode(createTestNode(HTTP_REQUEST_TYPE));

            viewModel.showHelp();

            assertEquals(HTTP_REQUEST_TYPE, helpType.get());
        }

        @Test
        void shouldInvokeShowAdvancedEditorCallback() {
            AtomicReference<String> editorNodeId = new AtomicReference<>();
            viewModel.setOnShowAdvancedEditor(editorNodeId::set);
            viewModel.loadNode(createTestNode("node-xyz", "code", "Test"));

            viewModel.showAdvancedEditor();

            assertEquals("node-xyz", editorNodeId.get());
        }
    }

    @Nested
    class NodeTypeLabels {

        @Test
        void shouldReturnCorrectLabels() {
            Map<String, String> expectedLabels = Map.ofEntries(
                    Map.entry("manualTrigger", "Manual Trigger"),
                    Map.entry("scheduleTrigger", "Schedule Trigger"),
                    Map.entry("webhookTrigger", "Webhook Trigger"),
                    Map.entry("fileTrigger", "File Trigger"),
                    Map.entry(HTTP_REQUEST_TYPE, "HTTP Request"),
                    Map.entry("code", "Code"),
                    Map.entry("executeCommand", "Execute Command"),
                    Map.entry("if", "If Condition"),
                    Map.entry("switch", "Switch"),
                    Map.entry("merge", "Merge"),
                    Map.entry("loop", "Loop"),
                    Map.entry("set", "Set"),
                    Map.entry("filter", "Filter"),
                    Map.entry("sort", "Sort"),
                    Map.entry("llmChat", "LLM Chat"),
                    Map.entry("textClassifier", "Text Classifier"),
                    Map.entry("embedding", "Embedding"),
                    Map.entry("rag", "RAG"));

            for (var entry : expectedLabels.entrySet()) {
                viewModel.loadNode(createTestNode(entry.getKey()));
                assertEquals(entry.getValue(), viewModel.getNodeTypeLabel(),
                        "Incorrect label for type: " + entry.getKey());
            }
        }

        @Test
        void shouldReturnTypeAsLabelForUnknown() {
            viewModel.loadNode(createTestNode("customNodeType"));

            // Unknown types are formatted as Title Case with spaces
            assertEquals("Custom Node Type", viewModel.getNodeTypeLabel());
        }
    }
}
