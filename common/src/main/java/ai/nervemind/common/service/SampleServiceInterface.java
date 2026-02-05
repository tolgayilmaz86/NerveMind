package ai.nervemind.common.service;

import java.util.List;
import java.util.Optional;

import ai.nervemind.common.domain.SampleWorkflow;
import ai.nervemind.common.domain.SampleWorkflow.Difficulty;
import ai.nervemind.common.domain.Workflow;

/**
 * Service interface for discovering and managing sample workflows.
 * 
 * <p>
 * Sample workflows are pre-built examples that help users learn NerveMind.
 * They include step-by-step guides, tagged metadata, and can be imported
 * into the user's workspace.
 * </p>
 * 
 * <h2>Sample Discovery</h2>
 * <p>
 * Samples are loaded from JSON files in the application's resources directory.
 * Each sample includes:
 * </p>
 * <ul>
 * <li><strong>Metadata</strong> - Name, description, category, difficulty</li>
 * <li><strong>Workflow</strong> - The actual workflow definition (nodes,
 * connections)</li>
 * <li><strong>Guide</strong> - Step-by-step tutorial with highlighted
 * nodes</li>
 * <li><strong>Requirements</strong> - Required credentials, environment
 * variables</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * 
 * <pre>
 * // Browse available samples
 * List&lt;SampleWorkflow&gt; allSamples = sampleService.getAllSamples();
 * 
 * // Filter samples
 * List&lt;SampleWorkflow&gt; pythonSamples = sampleService.searchSamples(
 *         null, "Data", "python", Difficulty.BEGINNER);
 * 
 * // Import a sample into the user's workspace
 * SampleWorkflow sample = sampleService.findById("hello-world").orElseThrow();
 * Workflow workflow = sampleService.getWorkflowFromSample(sample.id());
 * workflowService.create(WorkflowDTO.fromWorkflow(workflow));
 * </pre>
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see SampleWorkflow Sample workflow domain model
 * @see Difficulty Sample difficulty levels
 */
public interface SampleServiceInterface {

    /**
     * Get all available samples.
     * 
     * @return all samples
     */
    List<SampleWorkflow> getAllSamples();

    /**
     * Search samples with filters.
     *
     * @param query      Search query (matches name, description, tags)
     * @param category   Filter by category (null for all)
     * @param language   Filter by language (null for all)
     * @param difficulty Filter by difficulty (null for all)
     * @return Filtered list of samples
     */
    List<SampleWorkflow> searchSamples(String query, String category, String language, Difficulty difficulty);

    /**
     * Get unique categories from all samples.
     * 
     * @return list of categories
     */
    List<String> getCategories();

    /**
     * Get unique languages from all samples.
     * 
     * @return list of languages
     */
    List<String> getLanguages();

    /**
     * Find a sample by ID.
     * 
     * @param sampleId the sample ID to find
     * @return optional containing the sample if found
     */
    Optional<SampleWorkflow> findById(String sampleId);

    /**
     * Get the workflow from a sample (for importing).
     * 
     * @param sampleId the sample ID
     * @return the workflow definition
     */
    Workflow getWorkflowFromSample(String sampleId);

    /**
     * Reload all samples.
     */
    void reloadSamples();

    /**
     * Get featured/recommended samples.
     * 
     * @param limit the maximum number of samples to return
     * @return list of featured samples
     */
    List<SampleWorkflow> getFeaturedSamples(int limit);
}
