package ai.nervemind.plugin.api;

/**
 * Provider interface for Trigger nodes in NerveMind workflows.
 * 
 * <p>
 * Triggers are the entry points for workflows. They define what event or
 * condition
 * starts a workflow execution. Common trigger types include manual clicks,
 * scheduled times,
 * file system changes, or incoming webhooks.
 * </p>
 * 
 * <h2>Plugin Registration</h2>
 * <p>
 * Implementations must be registered via the Java ServiceLoader mechanism by
 * creating
 * a file at:
 * </p>
 * 
 * <pre>
 * META - INF / services / ai.nervemind.plugin.api.TriggerProvider
 * </pre>
 * <p>
 * The file should contain the fully qualified class name of each
 * implementation.
 * </p>
 * 
 * <h2>Implementation Example</h2>
 * 
 * <pre>
 * {@code
 * package com.example.myplugin;
 * 
 * public class MyTriggerProvider implements TriggerProvider {
 *     &#64;Override
 *     public String getNodeType() {
 *         return "com.example.myplugin.emailtrigger";
 *     }
 * 
 *     &#64;Override
 *     public String getDisplayName() {
 *         return "Email Trigger";
 *     }
 * 
 *     &#64;Override
 *     public String getDescription() {
 *         return "Triggers when a new email arrives";
 *     }
 * 
 *     &#64;Override
 *     public List<PropertyDefinition> getProperties() {
 *         return List.of(
 *                 PropertyDefinition.requiredString("mailbox", "Mailbox", "IMAP mailbox to monitor"));
 *     }
 * 
 *     &#64;Override
 *     public NodeExecutor getExecutor() {
 *         return new EmailTriggerExecutor();
 *     }
 * 
 *     @Override
 *     public boolean requiresBackgroundService() {
 *         return true; // Email monitoring runs in background
 *     }
 * }
 * }
 * </pre>
 * 
 * <h2>Background Triggers vs Manual Triggers</h2>
 * <ul>
 * <li><strong>Background triggers</strong>
 * ({@link #requiresBackgroundService()} returns {@code true})
 * run continuously in the background while the workflow is active. Examples:
 * file watchers,
 * scheduled tasks, email monitors.</li>
 * <li><strong>Manual triggers</strong> ({@link #requiresBackgroundService()}
 * returns {@code false})
 * are invoked on-demand by user action. Example: manual trigger button.</li>
 * </ul>
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see NodeDescriptor Base interface with common metadata
 * @see NodeExecutor Interface your executor must implement
 * @see ActionProvider For action node implementations
 */
public interface TriggerProvider extends NodeDescriptor {

    /**
     * Gets the executor that runs the trigger logic.
     * 
     * <p>
     * The executor is invoked when the trigger fires. For background triggers,
     * this may be called repeatedly. For manual triggers, it's called once per
     * user invocation.
     * </p>
     * 
     * @return the executor instance; must not be {@code null}
     * @see NodeExecutor
     */
    NodeExecutor getExecutor();

    /**
     * Indicates whether this trigger requires a background service.
     * 
     * <p>
     * Background triggers run continuously while the workflow is active.
     * They are managed by the NerveMind runtime and automatically started/stopped
     * when workflows are activated/deactivated.
     * </p>
     * 
     * <p>
     * Examples of background triggers:
     * </p>
     * <ul>
     * <li><strong>File Watcher</strong> - Monitors directories for file
     * changes</li>
     * <li><strong>Schedule</strong> - Runs on cron schedule</li>
     * <li><strong>Webhook</strong> - Listens for incoming HTTP requests</li>
     * </ul>
     * 
     * @return {@code true} if this trigger needs a background service,
     *         {@code false} for on-demand triggers like Manual Trigger
     */
    default boolean requiresBackgroundService() {
        return false;
    }

    /**
     * Returns the category for this node.
     * 
     * <p>
     * For trigger providers, this always returns {@link NodeCategory#TRIGGER}.
     * </p>
     * 
     * @return {@link NodeCategory#TRIGGER}
     */
    @Override
    default NodeCategory getCategory() {
        return NodeCategory.TRIGGER;
    }
}
