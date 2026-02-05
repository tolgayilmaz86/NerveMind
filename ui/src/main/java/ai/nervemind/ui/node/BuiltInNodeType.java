package ai.nervemind.ui.node;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignB;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignR;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;
import org.kordamp.ikonli.materialdesign2.MaterialDesignT;
import org.kordamp.ikonli.materialdesign2.MaterialDesignV;
import org.kordamp.ikonli.materialdesign2.MaterialDesignW;

import ai.nervemind.common.enums.NodeCategory;

/**
 * Built-in node types with their metadata.
 * 
 * <p>
 * This enum centralizes all built-in node type definitions, including their
 * IDs,
 * display names, icons, and categories. This provides type safety and
 * eliminates
 * the need for scattered string constants.
 * </p>
 * 
 * <h2>Benefits</h2>
 * <ul>
 * <li>Type-safe node type references</li>
 * <li>Centralized metadata management</li>
 * <li>Consistent naming across the application</li>
 * <li>Easy to add new node types</li>
 * </ul>
 */
public enum BuiltInNodeType implements NodeTypeDescriptor {
        // Triggers
        /** Manual trigger node. */
        MANUAL_TRIGGER("manualTrigger", "Manual Trigger", MaterialDesignP.PLAY_CIRCLE, NodeCategory.TRIGGER,
                        "Starts the workflow when manually triggered."),
        /** Schedule trigger node. */
        SCHEDULE_TRIGGER("scheduleTrigger", "Schedule", MaterialDesignC.CLOCK_OUTLINE, NodeCategory.TRIGGER,
                        "Runs on a schedule. Set cron expression in parameters."),
        /** Webhook trigger node. */
        WEBHOOK_TRIGGER("webhookTrigger", "Webhook", MaterialDesignW.WEBHOOK, NodeCategory.TRIGGER,
                        "Listens for HTTP webhook calls."),

        // Actions
        // Actions
        /** HTTP Request node. */
        HTTP_REQUEST("httpRequest", "HTTP Request", MaterialDesignW.WEB, NodeCategory.ACTION,
                        "Make HTTP requests. Set url, method, headers, body."),
        /** Code execution node. */
        CODE("code", "Code", MaterialDesignC.CODE_TAGS, NodeCategory.ACTION,
                        "Execute JavaScript or Python code. Access input via 'items'."),
        /** Command execution node. */
        EXECUTE_COMMAND("executeCommand", "Execute Command", MaterialDesignC.CONSOLE, NodeCategory.ACTION,
                        "Run shell commands on the system."),

        // Flow Control
        // Flow Control
        /** If-Condition node. */
        IF("if", "If", MaterialDesignC.CALL_SPLIT, NodeCategory.FLOW,
                        "Branch based on conditions. Set condition in parameters."),
        /** Switch-Case node. */
        SWITCH("switch", "Switch", MaterialDesignS.SWAP_HORIZONTAL, NodeCategory.FLOW,
                        "Multiple branches based on value matching."),
        /** Merge node. */
        MERGE("merge", "Merge", MaterialDesignC.CALL_MERGE, NodeCategory.FLOW,
                        "Merge multiple input branches into one."),
        /** Loop node. */
        LOOP("loop", "Loop", MaterialDesignR.REPEAT, NodeCategory.FLOW,
                        "Iterate over items. Set batchSize for parallel processing."),

        // Data
        // Data
        /** Set Variable node. */
        SET("set", "Set", MaterialDesignP.PENCIL, NodeCategory.DATA,
                        "Set or modify data values."),
        /** Filter node. */
        FILTER("filter", "Filter", MaterialDesignF.FILTER_OUTLINE, NodeCategory.DATA,
                        "Filter items based on conditions."),
        /** Sort node. */
        SORT("sort", "Sort", MaterialDesignS.SORT_ASCENDING, NodeCategory.DATA,
                        "Sort items by specified field."),

        // AI
        // AI
        /** LLM Chat node. */
        LLM_CHAT("llmChat", "LLM Chat", MaterialDesignR.ROBOT, NodeCategory.AI,
                        "Chat with LLM models. Set model, prompt, temperature."),
        /** Text Classifier node. */
        TEXT_CLASSIFIER("textClassifier", "Text Classifier", MaterialDesignT.TAG_TEXT_OUTLINE, NodeCategory.AI,
                        "Classify text into categories using AI."),
        /** Embedding node. */
        EMBEDDING("embedding", "Embedding", MaterialDesignV.VECTOR_BEZIER, NodeCategory.AI,
                        "Generate vector embeddings for semantic search."),
        /** RAG node. */
        RAG("rag", "RAG", MaterialDesignB.BOOK_SEARCH, NodeCategory.AI,
                        "Retrieval-Augmented Generation for document Q&A."),

        // Advanced
        // Advanced
        /** Subworkflow node. */
        SUBWORKFLOW("subworkflow", "Subworkflow", MaterialDesignS.SITEMAP, NodeCategory.ADVANCED,
                        "Execute another workflow as a sub-process."),
        /** Parallel execution node. */
        PARALLEL("parallel", "Parallel", MaterialDesignF.FORMAT_ALIGN_JUSTIFY, NodeCategory.ADVANCED,
                        "Execute multiple branches in parallel."),
        /** Try/Catch node. */
        TRY_CATCH("tryCatch", "Try/Catch", MaterialDesignS.SHIELD_CHECK, NodeCategory.ADVANCED,
                        "Handle errors gracefully with try/catch logic."),
        /** Retry node. */
        RETRY("retry", "Retry", MaterialDesignR.REFRESH, NodeCategory.ADVANCED,
                        "Retry failed operations with configurable attempts."),
        /** Rate Limit node. */
        RATE_LIMIT("rate_limit", "Rate Limit", MaterialDesignS.SPEEDOMETER, NodeCategory.ADVANCED,
                        "Limit execution rate to avoid overwhelming APIs.");

        private final String id;
        private final String displayName;
        private final Ikon icon;
        private final NodeCategory category;
        private final String helpText;
        private final String accentColor;
        private final String propertiesIcon;

        BuiltInNodeType(String id, String displayName, Ikon icon, NodeCategory category, String helpText) {
                this.id = id;
                this.displayName = displayName;
                this.icon = icon;
                this.category = category;
                this.helpText = helpText;
                // Infer visual properties from category, with specific overrides
                this.accentColor = inferAccentColor(id, category);
                this.propertiesIcon = inferPropertiesIcon(id, icon, category);
        }

        private static String inferAccentColor(String id, NodeCategory category) {
                // Specific overrides for certain node types
                return switch (id) {
                        case "httpRequest" -> "linear-gradient(to right, #3b82f6 0%, #60a5fa 100%)";
                        case "code" -> "linear-gradient(to right, #06b6d4 0%, #22d3ee 100%)";
                        default -> switch (category) {
                                case TRIGGER -> "linear-gradient(to right, #f59e0b 0%, #fbbf24 100%)";
                                case AI -> "linear-gradient(to right, #8b5cf6 0%, #a78bfa 100%)";
                                case FLOW -> "linear-gradient(to right, #7c3aed 0%, #a78bfa 100%)";
                                case DATA -> "linear-gradient(to right, #10b981 0%, #34d399 100%)";
                                case ACTION -> "linear-gradient(to right, #06b6d4 0%, #22d3ee 100%)";
                                default -> "linear-gradient(to right, #4a9eff 0%, #6bb3ff 100%)";
                        };
                };
        }

        private static String inferPropertiesIcon(String id, Ikon icon, NodeCategory category) {
                // Specific overrides for certain node types - use the node's own icon
                return switch (id) {
                        case "httpRequest" -> "mdi2w-web";
                        case "code" -> "mdi2c-code-tags";
                        default -> switch (category) {
                                case TRIGGER -> "mdi2l-lightning-bolt";
                                case AI -> "mdi2b-brain";
                                case FLOW -> "mdi2s-sitemap";
                                case DATA -> "mdi2d-database";
                                default -> "mdi2c-cog";
                        };
                };
        }

        @Override
        public String getId() {
                return id;
        }

        @Override
        public String getDisplayName() {
                return displayName;
        }

        @Override
        public Ikon getIcon() {
                return icon;
        }

        @Override
        public NodeCategory getCategory() {
                return category;
        }

        @Override
        public String getHelpText() {
                return helpText;
        }

        @Override
        public String getAccentColor() {
                return accentColor;
        }

        @Override
        public String getPropertiesIcon() {
                return propertiesIcon;
        }

        /**
         * Find a built-in node type by its ID.
         * 
         * @param id the node type ID to search for
         * @return the matching node type, or null if not found
         */
        public static BuiltInNodeType fromId(String id) {
                for (BuiltInNodeType type : values()) {
                        if (type.id.equals(id)) {
                                return type;
                        }
                }
                return null;
        }
}
