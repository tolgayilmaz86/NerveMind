/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.canvas;

import java.util.HashMap;
import java.util.Map;

import ai.nervemind.common.domain.Node;
import ai.nervemind.common.enums.AiProvider;
import ai.nervemind.common.enums.HttpMethod;
import ai.nervemind.common.enums.ScriptLanguage;
import ai.nervemind.common.enums.SortOrder;
import ai.nervemind.ui.viewmodel.BaseViewModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

/**
 * ViewModel for the NodePropertiesPanel.
 *
 * <p>
 * Manages the state and logic for editing node properties including:
 * <ul>
 * <li>Node metadata (name, type, notes, disabled state)</li>
 * <li>Dynamic parameters based on node type</li>
 * <li>Validation and dirty state tracking</li>
 * </ul>
 *
 * <p>
 * <strong>IMPORTANT:</strong> This ViewModel only uses javafx.beans.* and
 * javafx.collections.*
 * imports. No javafx.scene.* classes are allowed to ensure testability.
 */
public class NodePropertiesViewModel extends BaseViewModel {

    // ===== Node Identity =====
    private final StringProperty nodeId = new SimpleStringProperty();
    private final StringProperty nodeType = new SimpleStringProperty();
    private final StringProperty nodeTypeLabel = new SimpleStringProperty();
    private final StringProperty accentColor = new SimpleStringProperty("#4a9eff");
    private final StringProperty nodeTypeIcon = new SimpleStringProperty("mdi2c-cog");

    // ===== Editable Properties =====
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty notes = new SimpleStringProperty();
    private final BooleanProperty disabled = new SimpleBooleanProperty(false);

    // ===== Dynamic Parameters =====
    private final ObservableMap<String, Object> parameters = FXCollections.observableHashMap();
    private final ObservableList<ParameterDefinition> parameterDefinitions = FXCollections.observableArrayList();

    // ===== State =====
    private final BooleanProperty visible = new SimpleBooleanProperty(false);
    private final BooleanProperty hasNode = new SimpleBooleanProperty(false);
    private final ObjectProperty<Node> originalNode = new SimpleObjectProperty<>();

    // ===== Callbacks =====
    private Runnable onPropertyChanged;
    private java.util.function.BiConsumer<String, Node> onApplyChanges;
    private Runnable onClose;
    private java.util.function.Consumer<String> onShowHelp;
    private java.util.function.Consumer<String> onShowAdvancedEditor;

    /**
     * Creates a new NodePropertiesViewModel.
     */
    public NodePropertiesViewModel() {
        // Track dirty state when properties change
        name.addListener((obs, oldVal, newVal) -> {
            markDirty();
            notifyPropertyChanged();
        });
        notes.addListener((obs, oldVal, newVal) -> {
            markDirty();
            notifyPropertyChanged();
        });
        disabled.addListener((obs, oldVal, newVal) -> {
            markDirty();
            notifyPropertyChanged();
        });
        parameters.addListener((javafx.collections.MapChangeListener<String, Object>) change -> {
            markDirty();
            notifyPropertyChanged();
        });
    }

    // ===== Node Identity Properties =====

    /**
     * Gets the node ID property for data binding.
     *
     * @return the node ID property
     */
    public StringProperty nodeIdProperty() {
        return nodeId;
    }

    /**
     * Gets the current node ID value.
     *
     * @return the node ID
     */
    public String getNodeId() {
        return nodeId.get();
    }

    /**
     * Gets the node type property for data binding.
     *
     * @return the node type property
     */
    public StringProperty nodeTypeProperty() {
        return nodeType;
    }

    /**
     * Gets the current node type value.
     *
     * @return the node type
     */
    public String getNodeType() {
        return nodeType.get();
    }

    /**
     * Gets the node type label property for data binding.
     *
     * @return the node type label property
     */
    public StringProperty nodeTypeLabelProperty() {
        return nodeTypeLabel;
    }

    /**
     * Gets the current node type label value.
     *
     * @return the node type label
     */
    public String getNodeTypeLabel() {
        return nodeTypeLabel.get();
    }

    /**
     * Gets the accent color property for data binding.
     *
     * @return the accent color property
     */
    public StringProperty accentColorProperty() {
        return accentColor;
    }

    /**
     * Gets the current accent color value.
     *
     * @return the accent color as a hex string
     */
    public String getAccentColor() {
        return accentColor.get();
    }

    /**
     * Gets the node type icon property for data binding.
     *
     * @return the node type icon property
     */
    public StringProperty nodeTypeIconProperty() {
        return nodeTypeIcon;
    }

    /**
     * Gets the current node type icon value.
     *
     * @return the node type icon identifier
     */
    public String getNodeTypeIcon() {
        return nodeTypeIcon.get();
    }

    // ===== Editable Properties =====

    /**
     * Gets the name property for data binding.
     *
     * @return the name property
     */
    public StringProperty nameProperty() {
        return name;
    }

    /**
     * Gets the current name value.
     *
     * @return the node name
     */
    public String getName() {
        return name.get();
    }

    /**
     * Sets the node name.
     *
     * @param name the node name to set
     */
    public void setName(String name) {
        this.name.set(name);
    }

    /**
     * Gets the notes property for data binding.
     *
     * @return the notes property
     */
    public StringProperty notesProperty() {
        return notes;
    }

    /**
     * Gets the current notes value.
     *
     * @return the node notes
     */
    public String getNotes() {
        return notes.get();
    }

    /**
     * Sets the node notes.
     *
     * @param notes the node notes to set
     */
    public void setNotes(String notes) {
        this.notes.set(notes);
    }

    /**
     * Gets the disabled property for data binding.
     *
     * @return the disabled property
     */
    public BooleanProperty disabledProperty() {
        return disabled;
    }

    /**
     * Gets whether the node is disabled.
     *
     * @return true if the node is disabled
     */
    public boolean isDisabled() {
        return disabled.get();
    }

    /**
     * Sets whether the node is disabled.
     *
     * @param disabled true to disable the node
     */
    public void setDisabled(boolean disabled) {
        this.disabled.set(disabled);
    }

    // ===== Parameters =====

    /**
     * Gets the observable map of node parameters.
     *
     * @return the parameters map
     */
    public ObservableMap<String, Object> getParameters() {
        return parameters;
    }

    /**
     * Gets the list of parameter definitions for this node type.
     *
     * @return the parameter definitions
     */
    public ObservableList<ParameterDefinition> getParameterDefinitions() {
        return parameterDefinitions;
    }

    /**
     * Sets a parameter value.
     *
     * @param key   the parameter key
     * @param value the parameter value
     */
    public void setParameter(String key, Object value) {
        parameters.put(key, value);
    }

    /**
     * Gets a parameter value by key.
     *
     * @param key the parameter key
     * @return the parameter value, or null if not found
     */
    public Object getParameter(String key) {
        return parameters.get(key);
    }

    // ===== State Properties =====

    /**
     * Gets the visible property for data binding.
     *
     * @return the visible property
     */
    public BooleanProperty visibleProperty() {
        return visible;
    }

    /**
     * Gets whether the properties panel is visible.
     *
     * @return true if visible
     */
    public boolean isVisible() {
        return visible.get();
    }

    /**
     * Sets whether the properties panel is visible.
     *
     * @param visible true to make visible
     */
    public void setVisible(boolean visible) {
        this.visible.set(visible);
    }

    /**
     * Gets the read-only property indicating if a node is currently selected.
     *
     * @return the has node property
     */
    public ReadOnlyBooleanProperty hasNodeProperty() {
        return hasNode;
    }

    /**
     * Checks if a node is currently selected.
     *
     * @return true if a node is selected
     */
    public boolean hasNode() {
        return hasNode.get();
    }

    /**
     * Gets the original node property for data binding.
     *
     * @return the original node property
     */
    public ObjectProperty<Node> originalNodeProperty() {
        return originalNode;
    }

    /**
     * Gets the original node being edited.
     *
     * @return the original node
     */
    public Node getOriginalNode() {
        return originalNode.get();
    }

    // ===== Actions =====

    /**
     * Load a node into the panel for editing.
     *
     * @param node the node to edit
     */
    public void loadNode(Node node) {
        if (node == null) {
            clearNode();
            return;
        }

        originalNode.set(node);
        hasNode.set(true);

        // Set identity
        nodeId.set(node.id());
        nodeType.set(node.type());
        nodeTypeLabel.set(getNodeTypeLabelText(node.type()));

        // Set visual styling
        updateNodeTypeVisuals(node.type());

        // Set editable properties
        name.set(node.name());
        notes.set(node.notes() != null ? node.notes() : "");
        disabled.set(node.disabled());

        // Load parameters
        parameters.clear();
        if (node.parameters() != null) {
            parameters.putAll(node.parameters());
        }

        // Build parameter definitions for UI
        buildParameterDefinitions(node.type(), node.parameters());

        clearDirty();
        visible.set(true);
    }

    /**
     * Clear the current node and hide the panel.
     */
    public void clearNode() {
        originalNode.set(null);
        hasNode.set(false);
        nodeId.set(null);
        nodeType.set(null);
        nodeTypeLabel.set(null);
        name.set(null);
        notes.set(null);
        disabled.set(false);
        parameters.clear();
        parameterDefinitions.clear();
        clearDirty();
    }

    /**
     * Show the panel with a node.
     *
     * @param node the node to display
     */
    public void show(Node node) {
        loadNode(node);
        visible.set(true);
    }

    /**
     * Hide the panel.
     */
    public void hide() {
        visible.set(false);
        if (onClose != null) {
            onClose.run();
        }
    }

    /**
     * Reset changes to the original node values.
     */
    public void reset() {
        Node original = originalNode.get();
        if (original != null) {
            loadNode(original);
        }
    }

    /**
     * Apply changes and create an updated node.
     *
     * @return the updated node, or null if no node is loaded
     */
    public Node applyChanges() {
        Node original = originalNode.get();
        if (original == null) {
            return null;
        }

        String newName = name.get();
        if (newName == null || newName.isBlank()) {
            newName = original.name();
        }

        Node updatedNode = new Node(
                original.id(),
                original.type(),
                newName.trim(),
                original.position(),
                new HashMap<>(parameters),
                original.credentialId(),
                disabled.get(),
                notes.get());

        if (onApplyChanges != null) {
            onApplyChanges.accept(original.id(), updatedNode);
        }

        originalNode.set(updatedNode);
        clearDirty();

        return updatedNode;
    }

    /**
     * Request to show help for the current node type.
     */
    public void showHelp() {
        if (onShowHelp != null && nodeType.get() != null) {
            onShowHelp.accept(nodeType.get());
        }
    }

    /**
     * Request to open the advanced editor.
     */
    public void showAdvancedEditor() {
        if (onShowAdvancedEditor != null && nodeId.get() != null) {
            onShowAdvancedEditor.accept(nodeId.get());
        }
    }

    // ===== Callbacks =====

    /**
     * Sets the callback to be invoked when properties change.
     *
     * @param callback the callback to run when properties change
     */
    public void setOnPropertyChanged(Runnable callback) {
        this.onPropertyChanged = callback;
    }

    /**
     * Sets the callback to be invoked when changes are applied.
     *
     * @param callback the callback accepting node ID and updated node
     */
    public void setOnApplyChanges(java.util.function.BiConsumer<String, Node> callback) {
        this.onApplyChanges = callback;
    }

    /**
     * Sets the callback to be invoked when the panel is closed.
     *
     * @param callback the callback to run when closed
     */
    public void setOnClose(Runnable callback) {
        this.onClose = callback;
    }

    /**
     * Sets the callback to be invoked when help is requested.
     *
     * @param callback the callback accepting the node type for help
     */
    public void setOnShowHelp(java.util.function.Consumer<String> callback) {
        this.onShowHelp = callback;
    }

    /**
     * Sets the callback to be invoked when advanced editor is requested.
     *
     * @param callback the callback accepting the node ID for advanced editing
     */
    public void setOnShowAdvancedEditor(java.util.function.Consumer<String> callback) {
        this.onShowAdvancedEditor = callback;
    }

    // ===== Private Helpers =====

    private void notifyPropertyChanged() {
        if (onPropertyChanged != null) {
            onPropertyChanged.run();
        }
    }

    private void updateNodeTypeVisuals(String type) {
        // Use NodeTypeRegistry for type-safe lookup with plugin support
        ai.nervemind.ui.node.NodeTypeRegistry registry = new ai.nervemind.ui.node.NodeTypeRegistry();
        ai.nervemind.ui.node.NodeTypeDescriptor descriptor = registry.get(type);

        String color;
        String icon;

        if (descriptor != null) {
            // Use enum-defined visual properties
            color = descriptor.getAccentColor();
            icon = descriptor.getPropertiesIcon();
        } else {
            // Fallback for unknown/plugin types - infer from naming patterns
            if (type.endsWith("Trigger")) {
                color = "linear-gradient(to right, #f59e0b 0%, #fbbf24 100%)";
                icon = "mdi2l-lightning-bolt";
            } else {
                // Default for unknown types
                color = "linear-gradient(to right, #4a9eff 0%, #6bb3ff 100%)";
                icon = "mdi2c-cog";
            }
        }

        accentColor.set(color);
        nodeTypeIcon.set(icon);
    }

    private String getNodeTypeLabelText(String type) {
        return switch (type) {
            case "manualTrigger" -> "Manual Trigger";
            case "scheduleTrigger" -> "Schedule Trigger";
            case "webhookTrigger" -> "Webhook Trigger";
            case "httpRequest" -> "HTTP Request";
            case "code" -> "Code";
            case "executeCommand" -> "Execute Command";
            case "if" -> "If Condition";
            case "switch" -> "Switch";
            case "merge" -> "Merge";
            case "loop" -> "Loop";
            case "set" -> "Set";
            case "filter" -> "Filter";
            case "sort" -> "Sort";
            case "llmChat" -> "LLM Chat";
            case "textClassifier" -> "Text Classifier";
            case "embedding" -> "Embedding";
            case "rag" -> "RAG";
            default -> formatTypeAsLabel(type);
        };
    }

    /**
     * Formats a camelCase type string as a readable label.
     */
    private String formatTypeAsLabel(String type) {
        if (type == null || type.isEmpty()) {
            return type;
        }
        // Convert camelCase to Title Case with spaces
        StringBuilder result = new StringBuilder();
        for (char c : type.toCharArray()) {
            if (Character.isUpperCase(c) && !result.isEmpty()) {
                result.append(' ');
            }
            result.append(result.isEmpty() ? Character.toUpperCase(c) : c);
        }
        return result.toString();
    }

    /**
     * Build parameter definitions based on node type.
     * These definitions tell the UI how to render each parameter field.
     * 
     * <p>
     * Note: Plugin nodes fall through to buildGenericParams which renders
     * a simple key-value form. Future work should expose PropertyDefinition
     * from the plugin API to enable rich property editing for plugins.
     * </p>
     */
    private void buildParameterDefinitions(String type, Map<String, Object> params) {
        parameterDefinitions.clear();

        if (params == null) {
            params = new HashMap<>();
        }

        switch (type) {
            case "httpRequest" -> buildHttpRequestParams(params);
            case "code" -> buildCodeParams(params);
            case "executeCommand" -> buildExecuteCommandParams(params);
            case "if" -> buildIfParams(params);
            case "switch" -> buildSwitchParams(params);
            case "loop" -> buildLoopParams(params);
            case "set" -> buildSetParams(params);
            case "filter" -> buildFilterParams(params);
            case "sort" -> buildSortParams(params);
            case "scheduleTrigger" -> buildScheduleTriggerParams(params);
            case "webhookTrigger" -> buildWebhookTriggerParams(params);
            case "llmChat" -> buildLlmChatParams(params);
            case "textClassifier" -> buildTextClassifierParams(params);
            case "embedding" -> buildEmbeddingParams(params);
            case "rag" -> buildRagParams(params);
            default -> buildGenericParams(params);
        }
    }

    // ===== Parameter Builders =====

    private void buildHttpRequestParams(Map<String, Object> params) {
        addTextField("url", "URL", params.getOrDefault("url", "").toString(), "https://api.example.com/endpoint");
        addComboBox("method", "Method", HttpMethod.getValuesArray(HttpMethod.commonMethods()),
                params.getOrDefault("method", HttpMethod.GET.getValue()).toString());
        addTextArea("headers", "Headers (JSON)", params.getOrDefault("headers", "{}").toString());
        addTextArea("body", "Request Body", params.getOrDefault("body", "").toString());
        addSpinner("timeout", "Timeout (seconds)",
                ((Number) params.getOrDefault("timeout", 30)).intValue(), 1, 300);
    }

    private void buildCodeParams(Map<String, Object> params) {
        addComboBox("language", "Language", ScriptLanguage.getIds(ScriptLanguage.codeNodeLanguages()),
                params.getOrDefault("language", ScriptLanguage.JAVASCRIPT.getId()).toString());
        addTextArea("code", "Code", params.getOrDefault("code", "").toString());
    }

    private void buildExecuteCommandParams(Map<String, Object> params) {
        addTextField("command", "Command", params.getOrDefault("command", "").toString(), "echo Hello");
        addTextField("args", "Arguments", params.getOrDefault("args", "").toString(), "--flag value");
        addTextField("cwd", "Working Directory", params.getOrDefault("cwd", "").toString(), "/path/to/dir");
        addSpinner("timeout", "Timeout (seconds)",
                ((Number) params.getOrDefault("timeout", 60)).intValue(), 1, 3600);
    }

    private void buildIfParams(Map<String, Object> params) {
        addTextArea("condition", "Condition", params.getOrDefault("condition", "").toString());
        addHint("Use expressions like: {{value}} > 10");
    }

    private void buildSwitchParams(Map<String, Object> params) {
        addTextField("value", "Value to Switch On", params.getOrDefault("value", "").toString(), "{{status}}");
        addTextArea("cases", "Cases (JSON)", params.getOrDefault("cases", "{}").toString());
    }

    private void buildLoopParams(Map<String, Object> params) {
        addTextField("items", "Items Expression", params.getOrDefault("items", "").toString(), "{{data.items}}");
        addSpinner("batchSize", "Batch Size",
                ((Number) params.getOrDefault("batchSize", 1)).intValue(), 1, 100);
        addCheckBox("parallel", "Run in Parallel",
                Boolean.parseBoolean(params.getOrDefault("parallel", "false").toString()));
    }

    private void buildSetParams(Map<String, Object> params) {
        addTextArea("values", "Values (JSON)", params.getOrDefault("values", "{}").toString());
        addCheckBox("keepOnlySet", "Keep Only Set Values",
                Boolean.parseBoolean(params.getOrDefault("keepOnlySet", "false").toString()));
    }

    private void buildFilterParams(Map<String, Object> params) {
        addTextField("items", "Items Expression", params.getOrDefault("items", "").toString(), "{{data}}");
        addTextArea("condition", "Filter Condition", params.getOrDefault("condition", "").toString());
        addSpinner("limit", "Limit", ((Number) params.getOrDefault("limit", 0)).intValue(), 0, 10000);
    }

    private void buildSortParams(Map<String, Object> params) {
        addTextField("items", "Items Expression", params.getOrDefault("items", "").toString(), "{{data}}");
        addTextField("sortBy", "Sort By Field", params.getOrDefault("sortBy", "").toString(), "name");
        addComboBox("order", "Order", new String[] { SortOrder.ASC.getValue(), SortOrder.DESC.getValue() },
                params.getOrDefault("order", SortOrder.ASC.getValue()).toString());
    }

    private void buildScheduleTriggerParams(Map<String, Object> params) {
        addTextField("cronExpression", "Cron Expression",
                params.getOrDefault("cronExpression", "").toString(), "0 0 * * *");
        addTextField("timezone", "Timezone", params.getOrDefault("timezone", "").toString(), "UTC");
        addHint("Examples: '0 9 * * MON-FRI' (9 AM weekdays)");
    }

    private void buildWebhookTriggerParams(Map<String, Object> params) {
        addTextField("path", "Webhook Path", params.getOrDefault("path", "").toString(), "/my-webhook");
        addComboBox("method", "HTTP Method", HttpMethod.getValuesArray(HttpMethod.webhookMethods()),
                params.getOrDefault("method", HttpMethod.POST.getValue()).toString());
        addSpinner("responseCode", "Response Code",
                ((Number) params.getOrDefault("responseCode", 200)).intValue(), 100, 599);
    }

    private void buildLlmChatParams(Map<String, Object> params) {
        addComboBox("provider", "Provider", AiProvider.getIds(AiProvider.chatProviders()),
                params.getOrDefault("provider", AiProvider.OPENAI.getId()).toString());
        addTextField("model", "Model", params.getOrDefault("model", "").toString(), "gpt-4");
        addTextArea("systemPrompt", "System Prompt", params.getOrDefault("systemPrompt", "").toString());
        addTextArea("prompt", "User Prompt", params.getOrDefault("prompt", "").toString());
        addSpinner("temperature", "Temperature (x10)",
                (int) (((Number) params.getOrDefault("temperature", 0.7)).doubleValue() * 10), 0, 20);
        addSpinner("maxTokens", "Max Tokens",
                ((Number) params.getOrDefault("maxTokens", 1000)).intValue(), 1, 128000);
    }

    private void buildTextClassifierParams(Map<String, Object> params) {
        addTextField("text", "Text Expression", params.getOrDefault("text", "").toString(), "{{input.text}}");
        addTextArea("categories", "Categories (comma-separated)",
                params.getOrDefault("categories", "").toString());
        addCheckBox("multiLabel", "Allow Multiple Labels",
                Boolean.parseBoolean(params.getOrDefault("multiLabel", "false").toString()));
    }

    private void buildEmbeddingParams(Map<String, Object> params) {
        addComboBox("provider", "Provider", AiProvider.getIds(AiProvider.embeddingProviders()),
                params.getOrDefault("provider", AiProvider.OPENAI.getId()).toString());
        addTextField("model", "Model", params.getOrDefault("model", "").toString(), "text-embedding-3-small");
        addTextArea("text", "Text to Embed", params.getOrDefault("text", "").toString());
    }

    private void buildRagParams(Map<String, Object> params) {
        addTextField("query", "Query Expression", params.getOrDefault("query", "").toString(), "{{input.question}}");
        addTextField("documents", "Documents Expression",
                params.getOrDefault("documents", "").toString(), "{{knowledgeBase}}");
        addSpinner("topK", "Top K Results",
                ((Number) params.getOrDefault("topK", 5)).intValue(), 1, 50);
        addComboBox("provider", "LLM Provider", new String[] { "openai", "anthropic", "ollama" },
                params.getOrDefault("provider", "openai").toString());
        addTextField("model", "Model", params.getOrDefault("model", "").toString(), "gpt-4");
    }

    private void buildGenericParams(Map<String, Object> params) {
        if (params.isEmpty()) {
            addHint("No configurable parameters for this node type.");
        } else {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                addTextField(entry.getKey(), entry.getKey(),
                        entry.getValue() != null ? entry.getValue().toString() : "", "");
            }
        }
    }

    // ===== Parameter Definition Helpers =====

    private void addTextField(String key, String label, String value, String placeholder) {
        parameterDefinitions.add(new ParameterDefinition(
                key, label, ParameterType.TEXT_FIELD, value, placeholder, null, 0, 0, null));
    }

    private void addTextArea(String key, String label, String value) {
        parameterDefinitions.add(new ParameterDefinition(
                key, label, ParameterType.TEXT_AREA, value, null, null, 0, 0, null));
    }

    private void addComboBox(String key, String label, String[] options, String selected) {
        parameterDefinitions.add(new ParameterDefinition(
                key, label, ParameterType.COMBO_BOX, selected, null, options, 0, 0, null));
    }

    private void addSpinner(String key, String label, int value, int min, int max) {
        parameterDefinitions.add(new ParameterDefinition(
                key, label, ParameterType.SPINNER, String.valueOf(value), null, null, min, max, null));
    }

    private void addCheckBox(String key, String label, boolean value) {
        parameterDefinitions.add(new ParameterDefinition(
                key, label, ParameterType.CHECK_BOX, String.valueOf(value), null, null, 0, 0, null));
    }

    private void addHint(String text) {
        parameterDefinitions.add(new ParameterDefinition(
                null, text, ParameterType.HINT, null, null, null, 0, 0, null));
    }

    // ===== Inner Types =====

    /**
     * Enum for parameter field types.
     */
    public enum ParameterType {
        /** Single-line text input field */
        TEXT_FIELD,
        /** Multi-line text input area */
        TEXT_AREA,
        /** Dropdown selection box */
        COMBO_BOX,
        /** Numeric spinner control */
        SPINNER,
        /** Boolean checkbox */
        CHECK_BOX,
        /** Read-only hint text */
        HINT,
        /** Expression editor field */
        EXPRESSION
    }

    /**
     * Definition of a parameter field for the UI to render.
     *
     * @param key         Technical key suffix for the parameter
     * @param label       Display label for the field
     * @param type        The UI component type (TEXT_FIELD, etc.)
     * @param value       Current string-serialized value
     * @param placeholder Optional ghost text for empty fields
     * @param options     Selection options for COMBO_BOX type
     * @param min         Minimum numeric value for SPINNER type
     * @param max         Maximum numeric value for SPINNER type
     * @param hint        Small helper text shown below the field
     */
    public record ParameterDefinition(
            String key,
            String label,
            ParameterType type,
            String value,
            String placeholder,
            String[] options,
            int min,
            int max,
            String hint) {

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            ParameterDefinition that = (ParameterDefinition) o;
            return min == that.min &&
                    max == that.max &&
                    java.util.Objects.equals(key, that.key) &&
                    java.util.Objects.equals(label, that.label) &&
                    type == that.type &&
                    java.util.Objects.equals(value, that.value) &&
                    java.util.Objects.equals(placeholder, that.placeholder) &&
                    java.util.Arrays.equals(options, that.options) &&
                    java.util.Objects.equals(hint, that.hint);
        }

        @Override
        public int hashCode() {
            int result = java.util.Objects.hash(key, label, type, value, placeholder, min, max, hint);
            result = 31 * result + java.util.Arrays.hashCode(options);
            return result;
        }

        @Override
        public String toString() {
            return "ParameterDefinition[" +
                    "key=" + key +
                    ", label=" + label +
                    ", type=" + type +
                    ", value=" + value +
                    ", placeholder=" + placeholder +
                    ", options=" + java.util.Arrays.toString(options) +
                    ", min=" + min +
                    ", max=" + max +
                    ", hint=" + hint +
                    ']';
        }
    }
}
