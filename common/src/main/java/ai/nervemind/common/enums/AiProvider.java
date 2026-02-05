package ai.nervemind.common.enums;

/**
 * AI/LLM providers supported by the application.
 * 
 * <p>
 * This enum provides type-safe provider identification, replacing
 * hardcoded string values in node parameters and settings.
 * </p>
 * 
 * <p>
 * Plugins can still provide custom providers by implementing their
 * own provider identification, but built-in providers are defined here.
 * </p>
 */
public enum AiProvider {
    /** OpenAI (GPT-4, GPT-3.5, etc.) */
    OPENAI("openai", "OpenAI", "GPT models from OpenAI"),

    /** Anthropic (Claude) */
    ANTHROPIC("anthropic", "Anthropic", "Claude models from Anthropic"),

    /** Ollama (local models) */
    OLLAMA("ollama", "Ollama", "Locally hosted models via Ollama"),

    /** Azure OpenAI Service */
    AZURE("azure", "Azure OpenAI", "OpenAI models hosted on Azure"),

    /** Google Gemini */
    GEMINI("gemini", "Google Gemini", "Gemini models from Google"),

    /** Cohere */
    COHERE("cohere", "Cohere", "Cohere language models"),

    /** Groq */
    GROQ("groq", "Groq", "Fast inference with Groq"),

    /** Custom/Other provider */
    CUSTOM("custom", "Custom", "Custom AI provider");

    private final String id;
    private final String displayName;
    private final String description;

    AiProvider(String id, String displayName, String description) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Get the provider ID used in parameters and settings.
     * 
     * @return the provider identifier (e.g., "openai")
     */
    public String getId() {
        return id;
    }

    /**
     * Get the display name for this provider.
     * 
     * @return the user-facing name (e.g., "OpenAI")
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get the description of this provider.
     * 
     * @return human-readable description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Parse a provider from its ID string.
     * 
     * @param id the provider ID (case-insensitive)
     * @return the corresponding AiProvider, or OPENAI as default
     */
    public static AiProvider fromId(String id) {
        if (id == null) {
            return OPENAI;
        }
        return switch (id.toLowerCase()) {
            case "anthropic" -> ANTHROPIC;
            case "ollama" -> OLLAMA;
            case "azure" -> AZURE;
            case "gemini" -> GEMINI;
            case "cohere" -> COHERE;
            case "groq" -> GROQ;
            case "custom" -> CUSTOM;
            default -> OPENAI;
        };
    }

    /**
     * Get providers for LLM chat operations.
     * 
     * @return array of chat-capable providers
     */
    public static AiProvider[] chatProviders() {
        return new AiProvider[] { OPENAI, ANTHROPIC, OLLAMA, AZURE, GEMINI };
    }

    /**
     * Get providers for embedding operations.
     * 
     * @return array of embedding-capable providers
     */
    public static AiProvider[] embeddingProviders() {
        return new AiProvider[] { OPENAI, OLLAMA, COHERE };
    }

    /**
     * Get all provider IDs as string array for UI dropdowns.
     * 
     * @param providers the array of providers to get IDs for
     * @return array of provider ID strings
     */
    public static String[] getIds(AiProvider[] providers) {
        String[] ids = new String[providers.length];
        for (int i = 0; i < providers.length; i++) {
            ids[i] = providers[i].getId();
        }
        return ids;
    }

    @Override
    public String toString() {
        return id;
    }
}
