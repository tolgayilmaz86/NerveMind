/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.canvas;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("NodeHelpProvider")
class NodeHelpProviderTest {

    @Test
    @DisplayName("should return detailed help for known node type")
    void shouldReturnDetailedHelpForKnownNodeType() {
        NodeHelpProvider.NodeHelp help = NodeHelpProvider.getHelp("manualTrigger");

        assertThat(help).isNotNull();
        assertThat(help.title()).isEqualTo("Manual Trigger");
        assertThat(help.shortDescription()).contains("Starts a workflow manually");
        assertThat(help.detailedDescription()).contains("on-demand");
        assertThat(help.sampleCode()).contains("timestamp");
    }

    @Test
    @DisplayName("should return advanced node help for retry")
    void shouldReturnAdvancedNodeHelpForRetry() {
        NodeHelpProvider.NodeHelp help = NodeHelpProvider.getHelp("retry");

        assertThat(help).isNotNull();
        assertThat(help.title()).isEqualTo("Retry with Backoff");
        assertThat(help.shortDescription()).contains("retry");
        assertThat(help.detailedDescription()).contains("Backoff Strategies");
        assertThat(help.sampleCode()).contains("maxRetries");
    }

    @Test
    @DisplayName("should return fallback help for unknown node type")
    void shouldReturnFallbackHelpForUnknownNodeType() {
        NodeHelpProvider.NodeHelp help = NodeHelpProvider.getHelp("unknownCustomNode");

        assertThat(help).isNotNull();
        assertThat(help.title()).isEqualTo("unknownCustomNode");
        assertThat(help.shortDescription()).contains("No description available");
        assertThat(help.detailedDescription()).contains("doesn't have detailed documentation");
        assertThat(help.sampleCode()).contains("No sample code available");
    }

    @Test
    @DisplayName("should return non-fallback help for a broad set of known node types")
    void shouldReturnNonFallbackHelpForBroadSetOfKnownNodeTypes() {
        String[] knownTypes = {
                "manualTrigger",
                "scheduleTrigger",
                "webhookTrigger",
                "httpRequest",
                "code",
                "executeCommand",
                "if",
                "switch",
                "merge",
                "loop",
                "set",
                "filter",
                "sort",
                "llmChat",
                "textClassifier",
                "embedding",
                "rag",
                "subworkflow",
                "parallel",
                "tryCatch",
                "retry",
                "rate_limit"
        };

        for (String type : knownTypes) {
            NodeHelpProvider.NodeHelp help = NodeHelpProvider.getHelp(type);
            assertThat(help).as("help for type %s", type).isNotNull();
            assertThat(help.title()).as("title for type %s", type).isNotBlank();
            assertThat(help.shortDescription()).as("short description for type %s", type)
                    .doesNotContain("No description available");
            assertThat(help.detailedDescription()).as("detailed description for type %s", type)
                    .isNotBlank();
        }
    }
}
