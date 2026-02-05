package ai.nervemind.plugin.api;

/**
 * Indicates the difficulty level of a sample workflow.
 * 
 * <p>
 * Used to help users find samples appropriate for their skill level.
 * The samples browser can filter by difficulty.
 * </p>
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see SampleDefinition
 */
public enum SampleDifficulty {

    /**
     * Simple samples suitable for new users.
     * Typically uses 2-5 nodes with basic configuration.
     */
    BEGINNER,

    /**
     * Moderate complexity samples for users familiar with basics.
     * May include conditional logic, multiple branches, or API integrations.
     */
    INTERMEDIATE,

    /**
     * Complex samples demonstrating advanced patterns.
     * May include error handling, parallel execution, or complex data transforms.
     */
    ADVANCED
}
