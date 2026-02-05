package ai.nervemind.common.enums;

/**
 * Scripting languages supported by the Code node.
 * 
 * <p>
 * This enum provides type-safe language identification, replacing
 * hardcoded string values in node parameters.
 * </p>
 * 
 * <p>
 * Plugins can extend this by implementing their own language support,
 * but built-in languages are defined here.
 * </p>
 */
public enum ScriptLanguage {
    /** JavaScript (GraalJS engine) */
    JAVASCRIPT("javascript", "JavaScript", "js", "GraalJS JavaScript engine"),

    /** Python (GraalPy or external Python) */
    PYTHON("python", "Python", "py", "Python scripting support"),

    /** Expression language (simple expressions) */
    EXPRESSION("expression", "Expression", "expr", "Simple expression evaluation");

    private final String id;
    private final String displayName;
    private final String fileExtension;
    private final String description;

    ScriptLanguage(String id, String displayName, String fileExtension, String description) {
        this.id = id;
        this.displayName = displayName;
        this.fileExtension = fileExtension;
        this.description = description;
    }

    /**
     * Get the language ID used in parameters.
     * 
     * @return the language identifier (e.g., "javascript")
     */
    public String getId() {
        return id;
    }

    /**
     * Get the display name for this language.
     * 
     * @return the user-facing name (e.g., "JavaScript")
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get the file extension for this language.
     * 
     * @return the file extension (e.g., "js")
     */
    public String getFileExtension() {
        return fileExtension;
    }

    /**
     * Get the description of this language.
     * 
     * @return human-readable description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Parse a language from its ID string.
     * 
     * @param id the language ID (case-insensitive)
     * @return the corresponding ScriptLanguage, or JAVASCRIPT as default
     */
    public static ScriptLanguage fromId(String id) {
        if (id == null) {
            return JAVASCRIPT;
        }
        return switch (id.toLowerCase()) {
            case "python", "py" -> PYTHON;
            case "expression", "expr" -> EXPRESSION;
            default -> JAVASCRIPT;
        };
    }

    /**
     * Get languages supported in the Code node.
     * 
     * @return array of supported scripting languages
     */
    public static ScriptLanguage[] codeNodeLanguages() {
        return new ScriptLanguage[] { JAVASCRIPT, PYTHON };
    }

    /**
     * Get language IDs as string array for UI dropdowns.
     * 
     * @param languages the languages to convert
     * @return array of language ID strings
     */
    public static String[] getIds(ScriptLanguage[] languages) {
        String[] ids = new String[languages.length];
        for (int i = 0; i < languages.length; i++) {
            ids[i] = languages[i].getId();
        }
        return ids;
    }

    @Override
    public String toString() {
        return id;
    }
}
