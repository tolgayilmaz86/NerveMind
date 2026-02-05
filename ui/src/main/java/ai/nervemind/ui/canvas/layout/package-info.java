/**
 * Auto-layout engine for workflow canvas.
 * 
 * <p>
 * This package provides a clean separation of the auto-layout algorithm
 * from the UI canvas. The layout engine computes optimal node positions
 * without any JavaFX dependencies.
 * </p>
 * 
 * <h2>Key Classes</h2>
 * <ul>
 * <li>{@link ai.nervemind.ui.canvas.layout.AutoLayoutEngine} - Main layout
 * algorithm</li>
 * <li>{@link ai.nervemind.ui.canvas.layout.LayoutConfig} - Configuration
 * options</li>
 * </ul>
 * 
 * <h2>Algorithm Overview</h2>
 * <ol>
 * <li>Find trigger nodes (no incoming connections)</li>
 * <li>BFS from triggers to assign columns (with cycle handling)</li>
 * <li>Place disconnected nodes in a separate end column</li>
 * <li>Sort nodes within columns using barycenter heuristic</li>
 * <li>Compute positions with vertical centering</li>
 * </ol>
 */
package ai.nervemind.ui.canvas.layout;
