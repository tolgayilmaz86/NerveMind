/**
 * NerveMind Plugin API.
 * 
 * This module provides the interfaces and contracts for extending NerveMind
 * with custom workflow nodes (triggers and actions).
 * 
 * <h2>Creating a Plugin</h2>
 * <ol>
 * <li>Implement {@link ai.nervemind.plugin.api.TriggerProvider} or
 * {@link ai.nervemind.plugin.api.ActionProvider}</li>
 * <li>Create a META-INF/services file pointing to your implementation</li>
 * <li>Package as a JAR and place on the classpath</li>
 * </ol>
 * 
 * <h2>Key Interfaces</h2>
 * <ul>
 * <li>{@link ai.nervemind.plugin.api.NodeDescriptor} - Metadata for UI
 * rendering</li>
 * <li>{@link ai.nervemind.plugin.api.NodeExecutor} - Execution logic</li>
 * <li>{@link ai.nervemind.plugin.api.PropertyDefinition} - Property
 * metadata</li>
 * </ul>
 */
package ai.nervemind.plugin.api;
