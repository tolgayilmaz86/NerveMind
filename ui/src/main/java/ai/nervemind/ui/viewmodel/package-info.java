/**
 * ViewModels for the MVVM architecture.
 * 
 * <p>
 * ViewModels contain:
 * <ul>
 * <li>Observable properties for UI state</li>
 * <li>Commands/actions as methods</li>
 * <li>Validation logic</li>
 * <li>Coordination with Services</li>
 * </ul>
 * 
 * <p>
 * <strong>Important:</strong> ViewModels should NOT import javafx.scene.*
 * classes.
 * Only javafx.beans.* imports are allowed for property bindings.
 * This ensures ViewModels are testable without JavaFX runtime.
 */
package ai.nervemind.ui.viewmodel;
