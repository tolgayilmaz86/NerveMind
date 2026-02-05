/**
 * View Controllers for the MVVM architecture.
 * 
 * <p>
 * View Controllers are thin adapters between FXML and ViewModels:
 * <ul>
 * <li>Annotated with @Component and @FxmlView</li>
 * <li>Inject ViewModel via Spring DI</li>
 * <li>Bind FXML @FXML fields to ViewModel properties</li>
 * <li>Delegate actions to ViewModel methods</li>
 * </ul>
 * 
 * <p>
 * Controllers should be kept minimal - business logic belongs in ViewModels.
 */
package ai.nervemind.ui.view;
