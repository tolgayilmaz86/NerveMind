package ai.nervemind.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import ai.nervemind.common.domain.Node;
import ai.nervemind.common.domain.SampleWorkflow;
import ai.nervemind.common.domain.Workflow;

/**
 * Tests for sample workflow loading and validation.
 * 
 * <p>
 * Ensures all bundled sample workflows:
 * </p>
 * <ul>
 * <li>Load successfully from classpath</li>
 * <li>Have valid metadata (id, name, description)</li>
 * <li>Have valid workflow definitions</li>
 * <li>Cover all expected node types</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
class SampleWorkflowLoadingTest {

        @Autowired
        private SampleService sampleService;

        @Test
        @DisplayName("All samples load successfully")
        void allSamplesLoad() {
                List<SampleWorkflow> samples = sampleService.getAllSamples();

                assertThat(samples)
                                .as("Should have sample workflows loaded")
                                .isNotEmpty();

                // We expect at least the 12 bundled samples (00-12)
                assertThat(samples.size())
                                .as("Should have at least 12 sample workflows")
                                .isGreaterThanOrEqualTo(12);
        }

        @Test
        @DisplayName("All samples have required metadata")
        void allSamplesHaveRequiredMetadata() {
                List<SampleWorkflow> samples = sampleService.getAllSamples();

                for (SampleWorkflow sample : samples) {
                        assertThat(sample.id())
                                        .as("Sample should have an ID: " + sample.name())
                                        .isNotBlank();

                        assertThat(sample.name())
                                        .as("Sample should have a name: " + sample.id())
                                        .isNotBlank();

                        assertThat(sample.description())
                                        .as("Sample %s should have a description", sample.id())
                                        .isNotBlank();

                        assertThat(sample.category())
                                        .as("Sample %s should have a category", sample.id())
                                        .isNotBlank();

                        assertThat(sample.difficulty())
                                        .as("Sample %s should have a difficulty", sample.id())
                                        .isNotNull();
                }
        }

        @Test
        @DisplayName("All samples have valid workflow definitions")
        void allSamplesHaveValidWorkflows() {
                List<SampleWorkflow> samples = sampleService.getAllSamples();

                for (SampleWorkflow sample : samples) {
                        Workflow workflow = sample.workflow();

                        assertThat(workflow)
                                        .as("Sample %s should have a workflow", sample.id())
                                        .isNotNull();

                        assertThat(workflow.nodes())
                                        .as("Sample %s workflow should have nodes", sample.id())
                                        .isNotNull()
                                        .isNotEmpty();

                        // Every workflow should have at least one trigger
                        boolean hasTrigger = workflow.nodes().stream()
                                        .anyMatch(n -> n.type().endsWith("Trigger"));

                        assertThat(hasTrigger)
                                        .as("Sample %s should have at least one trigger node", sample.id())
                                        .isTrue();
                }
        }

        @Test
        @DisplayName("All sample IDs are unique")
        void allSampleIdsAreUnique() {
                List<SampleWorkflow> samples = sampleService.getAllSamples();
                Set<String> ids = new HashSet<>();

                for (SampleWorkflow sample : samples) {
                        boolean isUnique = ids.add(sample.id());
                        assertThat(isUnique)
                                        .as("Sample ID should be unique: " + sample.id())
                                        .isTrue();
                }
        }

        @Test
        @DisplayName("Sample search works correctly")
        void sampleSearchWorks() {
                // Search by query
                List<SampleWorkflow> aiSamples = sampleService.searchSamples("AI", null, null, null);
                assertThat(aiSamples)
                                .as("Should find samples containing 'AI'")
                                .isNotEmpty();

                // Search by category - use a category that actually exists in samples
                List<SampleWorkflow> apiSamples = sampleService.searchSamples(
                                null, "API Integration", null, null);
                assertThat(apiSamples)
                                .as("Should find samples in 'API Integration' category")
                                .isNotEmpty();

                // Search by difficulty
                List<SampleWorkflow> beginnerSamples = sampleService.searchSamples(
                                null, null, null, SampleWorkflow.Difficulty.BEGINNER);
                assertThat(beginnerSamples)
                                .as("Should find beginner samples")
                                .isNotEmpty();
        }

        @Test
        @DisplayName("Categories are extracted from samples")
        void categoriesAreExtracted() {
                List<String> categories = sampleService.getCategories();

                assertThat(categories)
                                .as("Should have categories")
                                .isNotEmpty();

                // Check for expected categories (using actual categories from samples)
                assertThat(categories)
                                .as("Should contain expected categories")
                                .containsAnyOf("API Integration", "AI/ML", "Data Processing", "Reliability");
        }

        @Test
        @DisplayName("Samples with guides have valid guide structure")
        void samplesWithGuidesAreValid() {
                List<SampleWorkflow> samples = sampleService.getAllSamples();

                for (SampleWorkflow sample : samples) {
                        if (sample.guide() != null) {
                                assertThat(sample.guide().steps())
                                                .as("Sample %s guide should have steps", sample.id())
                                                .isNotNull()
                                                .isNotEmpty();

                                for (SampleWorkflow.GuideStep step : sample.guide().steps()) {
                                        assertThat(step.title())
                                                        .as("Guide step should have a title in sample %s", sample.id())
                                                        .isNotBlank();

                                        assertThat(step.content())
                                                        .as("Guide step should have content in sample %s", sample.id())
                                                        .isNotBlank();
                                }
                        }
                }
        }

        @Test
        @DisplayName("All built-in node types are covered by samples")
        void allNodeTypesCovered() {
                // Expected core node types that should be covered (using actual executor type
                // names)
                // Note: subworkflow is excluded as it requires workflow references
                Set<String> expectedNodeTypes = Set.of(
                                "manualTrigger", "scheduleTrigger", "webhookTrigger",
                                "httpRequest", "code", "executeCommand",
                                "if", "switch", "merge", "loop",
                                "set", "filter", "sort",
                                "llmChat", "textClassifier", "embedding", "rag",
                                "parallel",
                                "tryCatch", "retry", "rate_limit"); // Note: rate_limit not rateLimit

                List<SampleWorkflow> samples = sampleService.getAllSamples();
                Set<String> foundNodeTypes = new HashSet<>();

                for (SampleWorkflow sample : samples) {
                        for (Node node : sample.workflow().nodes()) {
                                foundNodeTypes.add(node.type());
                        }
                }

                // Check coverage
                Set<String> missingTypes = new HashSet<>(expectedNodeTypes);
                missingTypes.removeAll(foundNodeTypes);

                assertThat(missingTypes)
                                .as("All expected node types should be covered by samples. Missing: " + missingTypes)
                                .isEmpty();
        }

        @Test
        @DisplayName("Find sample by ID works")
        void findByIdWorks() {
                // Get a known sample (using actual ID from the sample file)
                var weatherSample = sampleService.findById("sample-weather-alert");

                // Should find the sample
                assertThat(weatherSample)
                                .as("Should find the weather alert sample by ID")
                                .isPresent();

                assertThat(weatherSample.get().id())
                                .as("Found sample should have correct ID")
                                .isEqualTo("sample-weather-alert");

                // Non-existent ID returns empty
                var notFound = sampleService.findById("non-existent-sample-id");
                assertThat(notFound)
                                .as("Should return empty for non-existent ID")
                                .isEmpty();
        }

        @Test
        @DisplayName("Get workflow from sample works")
        void getWorkflowFromSampleWorks() {
                List<SampleWorkflow> samples = sampleService.getAllSamples();
                assertThat(samples).isNotEmpty();

                String sampleId = samples.get(0).id();
                Workflow workflow = sampleService.getWorkflowFromSample(sampleId);

                assertThat(workflow)
                                .as("Should retrieve workflow from sample")
                                .isNotNull();

                assertThat(workflow.nodes())
                                .as("Retrieved workflow should have nodes")
                                .isNotEmpty();
        }
}
