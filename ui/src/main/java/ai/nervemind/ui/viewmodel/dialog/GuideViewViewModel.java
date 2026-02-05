/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.dialog;

import java.util.List;
import java.util.function.Consumer;

import ai.nervemind.common.domain.SampleWorkflow;
import ai.nervemind.common.domain.SampleWorkflow.GuideStep;
import ai.nervemind.ui.viewmodel.BaseDialogViewModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * ViewModel for the Guide View dialog.
 * 
 * <p>
 * Manages navigation through guide steps for sample workflows.
 */
public class GuideViewViewModel extends BaseDialogViewModel<SampleWorkflow> {

    private final SampleWorkflow sample;
    private final ObservableList<GuideStep> steps = FXCollections.observableArrayList();

    // Navigation state
    private final IntegerProperty currentStepIndex = new SimpleIntegerProperty(0);
    private final ObjectProperty<GuideStep> currentStep = new SimpleObjectProperty<>();
    private final BooleanProperty canNavigatePrevious = new SimpleBooleanProperty(false);
    private final BooleanProperty canNavigateNext = new SimpleBooleanProperty(true);
    private final StringProperty stepCounterText = new SimpleStringProperty("");

    // Current step display
    private final StringProperty stepTitle = new SimpleStringProperty("");
    private final StringProperty stepContent = new SimpleStringProperty("");
    private final StringProperty stepCodeSnippet = new SimpleStringProperty("");
    private final BooleanProperty hasCodeSnippet = new SimpleBooleanProperty(false);
    private final BooleanProperty hasHighlightNodes = new SimpleBooleanProperty(false);
    private final StringProperty highlightNodesText = new SimpleStringProperty("");

    // Callbacks
    private Consumer<List<String>> onHighlightNodes;

    /**
     * Creates a new GuideViewViewModel.
     * 
     * @param sample the sample workflow with guide
     */
    public GuideViewViewModel(SampleWorkflow sample) {
        this.sample = sample;

        if (sample.guide() != null && sample.guide().steps() != null) {
            steps.setAll(sample.guide().steps());
        }

        // Setup navigation listeners
        currentStepIndex.addListener((obs, oldVal, newVal) -> {
            updateNavigationState();
            updateCurrentStep();
        });

        // Initialize first step
        if (!steps.isEmpty()) {
            // Manually trigger updates since setting 0 to 0 won't fire the listener
            updateNavigationState();
            updateCurrentStep();
        }
    }

    // ===== Properties =====

    /**
     * Gets the sample workflow being guided.
     *
     * @return the sample workflow
     */
    public SampleWorkflow getSample() {
        return sample;
    }

    /**
     * Gets the list of guide steps.
     *
     * @return the steps list
     */
    public ObservableList<GuideStep> getSteps() {
        return steps;
    }

    /**
     * The current step index.
     *
     * @return the current step index property
     */
    public ReadOnlyIntegerProperty currentStepIndexProperty() {
        return currentStepIndex;
    }

    public int getCurrentStepIndex() {
        return currentStepIndex.get();
    }

    /**
     * The current step.
     *
     * @return the current step property
     */
    public ObjectProperty<GuideStep> currentStepProperty() {
        return currentStep;
    }

    public GuideStep getCurrentStep() {
        return currentStep.get();
    }

    /**
     * Whether navigation to the previous step is possible.
     *
     * @return the can navigate previous property
     */
    public ReadOnlyBooleanProperty canNavigatePreviousProperty() {
        return canNavigatePrevious;
    }

    /**
     * Checks if navigation to the previous step is possible.
     *
     * @return true if can navigate previous
     */
    public boolean canNavigatePrevious() {
        return canNavigatePrevious.get();
    }

    /**
     * Whether navigation to the next step is possible.
     *
     * @return the can navigate next property
     */
    public ReadOnlyBooleanProperty canNavigateNextProperty() {
        return canNavigateNext;
    }

    /**
     * Checks if navigation to the next step is possible.
     *
     * @return true if can navigate next
     */
    public boolean canNavigateNext() {
        return canNavigateNext.get();
    }

    /**
     * The step counter text (e.g., "1 of 5").
     *
     * @return the step counter text property
     */
    public ReadOnlyStringProperty stepCounterTextProperty() {
        return stepCounterText;
    }

    public String getStepCounterText() {
        return stepCounterText.get();
    }

    /**
     * The title of the current step.
     *
     * @return the step title property
     */
    public ReadOnlyStringProperty stepTitleProperty() {
        return stepTitle;
    }

    public String getStepTitle() {
        return stepTitle.get();
    }

    /**
     * The content text for the current step.
     *
     * @return the step content property
     */
    public ReadOnlyStringProperty stepContentProperty() {
        return stepContent;
    }

    public String getStepContent() {
        return stepContent.get();
    }

    /**
     * The code snippet for the current step.
     *
     * @return the step code snippet property
     */
    public ReadOnlyStringProperty stepCodeSnippetProperty() {
        return stepCodeSnippet;
    }

    public String getStepCodeSnippet() {
        return stepCodeSnippet.get();
    }

    /**
     * Whether the current step has a code snippet.
     *
     * @return the has code snippet property
     */
    public ReadOnlyBooleanProperty hasCodeSnippetProperty() {
        return hasCodeSnippet;
    }

    /**
     * Checks if the current step has a code snippet.
     *
     * @return true if has code snippet
     */
    public boolean hasCodeSnippet() {
        return hasCodeSnippet.get();
    }

    /**
     * Whether the current step has highlight nodes.
     *
     * @return the has highlight nodes property
     */
    public ReadOnlyBooleanProperty hasHighlightNodesProperty() {
        return hasHighlightNodes;
    }

    /**
     * Checks if the current step has highlight nodes.
     *
     * @return true if has highlight nodes
     */
    public boolean hasHighlightNodes() {
        return hasHighlightNodes.get();
    }

    /**
     * The highlight nodes text for the current step.
     *
     * @return the highlight nodes text property
     */
    public ReadOnlyStringProperty highlightNodesTextProperty() {
        return highlightNodesText;
    }

    public String getHighlightNodesText() {
        return highlightNodesText.get();
    }

    // ===== Navigation =====

    /**
     * Navigate to a specific step by index.
     *
     * @param index the step index to navigate to
     */
    public void navigateToStep(int index) {
        if (index < 0 || index >= steps.size()) {
            return;
        }
        currentStepIndex.set(index);
    }

    /**
     * Navigate to the previous step.
     */
    public void navigatePrevious() {
        if (currentStepIndex.get() > 0) {
            navigateToStep(currentStepIndex.get() - 1);
        }
    }

    /**
     * Navigate to the next step.
     */
    public void navigateNext() {
        if (currentStepIndex.get() < steps.size() - 1) {
            navigateToStep(currentStepIndex.get() + 1);
        }
    }

    /**
     * Highlight nodes for the current step.
     */
    public void highlightCurrentStepNodes() {
        if (onHighlightNodes == null) {
            return;
        }

        GuideStep step = currentStep.get();
        if (step != null && step.highlightNodes() != null && !step.highlightNodes().isEmpty()) {
            onHighlightNodes.accept(step.highlightNodes());
        }
    }

    /**
     * Set the callback for highlighting nodes.
     *
     * @param callback the highlight callback
     */
    public void setOnHighlightNodes(Consumer<List<String>> callback) {
        this.onHighlightNodes = callback;
    }

    /**
     * Import the sample workflow (confirm the dialog).
     */
    public void importWorkflow() {
        confirm();
    }

    // ===== Validation =====

    @Override
    public boolean validate() {
        setValid(true);
        return true;
    }

    @Override
    protected void buildResult() {
        setResult(sample);
    }

    // ===== Private Methods =====

    private void updateNavigationState() {
        int index = currentStepIndex.get();
        int total = steps.size();

        canNavigatePrevious.set(index > 0);
        canNavigateNext.set(index < total - 1);
        stepCounterText.set("Step " + (index + 1) + " of " + total);
    }

    private void updateCurrentStep() {
        int index = currentStepIndex.get();
        if (index >= 0 && index < steps.size()) {
            GuideStep step = steps.get(index);
            currentStep.set(step);

            stepTitle.set("Step " + (index + 1) + ": " + step.title());
            stepContent.set(step.content() != null ? step.content() : "");

            String code = step.codeSnippet();
            hasCodeSnippet.set(code != null && !code.isEmpty());
            stepCodeSnippet.set(code != null ? code : "");

            List<String> nodes = step.highlightNodes();
            hasHighlightNodes.set(nodes != null && !nodes.isEmpty());
            if (nodes != null && !nodes.isEmpty()) {
                highlightNodesText.set("Click 'Highlight Nodes' to see: " + String.join(", ", nodes));
            } else {
                highlightNodesText.set("");
            }
        }
    }
}
