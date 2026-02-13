package ai.nervemind.plugin.api;

/**
 * Represents a subscription to a specific event type.
 * 
 * <p>
 * Plugins can declare which events they want to receive by registering
 * event handlers via {@link PluginContext#registerEventHandler(EventHandler)}.
 * </p>
 * 
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>{@code
 * // Register an event handler in init()
 * @Override
 * public void init(PluginContext context) {
 *     context.registerEventHandler(event -> {
 *         if (event instanceof WorkflowCompletedEvent e) {
 *             // Handle workflow completion
 *         }
 *     });
 * }
 * }</pre>
 * 
 * @param eventType the event type to subscribe to
 * @param priority  the priority for this subscription
 * @author NerveMind Team
 * @since 1.0.0
 * @see EventHandler
 * @see EventType
 */
public record EventSubscription(

        /** The event type to subscribe to. */
        EventType eventType,

        /** The priority for this subscription. */
        Priority priority) {

    /**
     * Creates a subscription with normal priority.
     * 
     * @param eventType the event type to subscribe to
     * @return a new EventSubscription with normal priority
     */
    public static EventSubscription normal(EventType eventType) {
        return new EventSubscription(eventType, Priority.NORMAL);
    }

    /**
     * Creates a subscription with high priority.
     * 
     * <p>
     * High priority subscriptions are called before normal and low priority ones.
     * </p>
     * 
     * @param eventType the event type to subscribe to
     * @return a new EventSubscription with high priority
     */
    public static EventSubscription high(EventType eventType) {
        return new EventSubscription(eventType, Priority.HIGH);
    }

    /**
     * Creates a subscription with low priority.
     * 
     * <p>
     * Low priority subscriptions are called after normal and high priority ones.
     * </p>
     * 
     * @param eventType the event type to subscribe to
     * @return a new EventSubscription with low priority
     */
    public static EventSubscription low(EventType eventType) {
        return new EventSubscription(eventType, Priority.LOW);
    }

    /**
     * Priority levels for event subscriptions.
     * 
     * <p>
     * Determines the order in which event handlers are called:
     * </p>
     * <ol>
     * <li>{@link #HIGH} - Called first</li>
     * <li>{@link #NORMAL} - Default order</li>
     * <li>{@link #LOW} - Called last</li>
     * </ol>
     */
    public enum Priority {
        /** Low priority - called after normal. */
        LOW(0),
        /** Normal priority - default. */
        NORMAL(1),
        /** High priority - called first. */
        HIGH(2);

        private final int value;

        Priority(int value) {
            this.value = value;
        }

        /**
         * Gets the numeric value of this priority.
         * 
         * @return the priority value (higher = called first)
         */
        public int getValue() {
            return value;
        }
    }

    @Override
    public String toString() {
        return String.format("EventSubscription[type=%s, priority=%s]",
                eventType, priority);
    }
}
