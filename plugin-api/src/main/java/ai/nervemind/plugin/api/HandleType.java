package ai.nervemind.plugin.api;

/**
 * Defines the type of a connection handle.
 * 
 * <p>
 * Handles can either be inputs (accepting incoming connections) or outputs
 * (producing outgoing connections). This type determines how the workflow
 * runtime validates and processes connections.
 * </p>
 * 
 * <h2>Connection Rules</h2>
 * <ul>
 * <li>An output handle can connect to one or more input handles</li>
 * <li>An input handle typically receives from one output handle</li>
 * <li>Connections always flow from OUTPUT to INPUT</li>
 * </ul>
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see HandleDefinition For defining complete handles
 */
public enum HandleType {

    /**
     * An input handle that accepts incoming connections.
     * Data flows INTO the node through input handles.
     */
    INPUT,

    /**
     * An output handle that produces outgoing connections.
     * Data flows OUT of the node through output handles.
     */
    OUTPUT
}
