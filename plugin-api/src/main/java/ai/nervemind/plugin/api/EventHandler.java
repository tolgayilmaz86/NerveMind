package ai.nervemind.plugin.api;

/**
 * Handler for system events.
 * 
 * <p>
 * Plugins can register event handlers to react to various system events
 * such as workflow lifecycle events, execution events, and application events.
 * </p>
 * 
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * public class MyPlugin implements PluginProvider {
 * 
 *     @Override
 *     public void init(PluginContext context) {
 *         context.registerEventHandler(event -> {
 *             if (event instanceof WorkflowCompletedEvent) {
 *                 WorkflowCompletedEvent e = (WorkflowCompletedEvent) event;
 *                 context.getLogger().info(
 *                         "Workflow completed: " + e.getWorkflowId());
 *             }
 *         });
 *     }
 * }
 * }</pre>
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see PluginContext#registerEventHandler(EventHandler)
 * @see EventType
 */
@FunctionalInterface
public interface EventHandler {

    /**
     * Called when an event occurs.
     * 
     * @param event the event that occurred
     */
    void onEvent(Event event);
}
