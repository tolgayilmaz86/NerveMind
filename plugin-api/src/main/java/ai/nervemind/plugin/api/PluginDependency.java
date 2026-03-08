package ai.nervemind.plugin.api;

/**
 * Represents a dependency on another plugin.
 * 
 * <p>
 * Use this to declare that your plugin requires another plugin to function.
 * The system will ensure dependencies are loaded before your plugin.
 * </p>
 * 
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * public class MyPlugin implements PluginProvider {
 * 
 *     @Override
 *     public List<PluginDependency> getDependencies() {
 *         return List.of(
 *                 PluginDependency.required("ai.nervemind.plugin.http", ">=1.0.0"),
 *                 PluginDependency.optional("com.example.helper-plugin", ">=2.0.0"));
 *     }
 * }
 * }</pre>
 * 
 * @param pluginId           the plugin ID of the dependency (e.g.,
 *                           "ai.nervemind.plugin.http")
 * @param versionRequirement version requirement specification (e.g., "1.0.0",
 *                           ">=1.0.0", "^1.0.0")
 * @author NerveMind Team
 * @since 1.0.0
 * @see PluginProvider#getDependencies()
 */
public record PluginDependency(

        /**
         * The plugin ID (node type) of the dependency.
         * 
         * <p>
         * Example: "ai.nervemind.plugin.http" or "com.example.helper"
         * </p>
         */
        String pluginId,

        /**
         * Version requirement specification.
         * 
         * <p>
         * Supports the following formats:
         * </p>
         * <ul>
         * <li>"1.0.0" - Exact version</li>
         * <li>"1.0.0+" - At least version 1.0.0</li>
         * <li>"1.0.0 - 2.0.0" - Range (inclusive)</li>
         * <li>"~1.0.0" - Compatible with 1.0.0 (same major.minor)</li>
         * <li>"^1.0.0" - Compatible with 1.x.x (same major)</li>
         * </ul>
         */
        String versionRequirement) {

    /**
     * Creates a dependency requiring an exact version.
     * 
     * @param pluginId the plugin ID
     * @param version  the exact version required
     * @return a PluginDependency with exact version
     */
    public static PluginDependency exact(String pluginId, String version) {
        return new PluginDependency(pluginId, version);
    }

    /**
     * Creates a required dependency requiring at least a version.
     * 
     * <p>
     * The plugin will fail to load if this dependency is not available.
     * </p>
     * 
     * @param pluginId   the plugin ID
     * @param minVersion the minimum version required (e.g., "1.0.0")
     * @return a PluginDependency with minimum version
     */
    public static PluginDependency required(String pluginId, String minVersion) {
        return new PluginDependency(pluginId, minVersion.startsWith(">=") ? minVersion : ">=" + minVersion);
    }

    /**
     * Creates an optional dependency requiring at least a version.
     * 
     * <p>
     * The plugin will load even if this dependency is not available,
     * but features depending on it may be disabled.
     * </p>
     * 
     * @param pluginId   the plugin ID
     * @param minVersion the minimum version required
     * @return a PluginDependency with minimum version
     */
    public static PluginDependency optional(String pluginId, String minVersion) {
        return new PluginDependency(pluginId, minVersion + "+");
    }

    /**
     * Creates a dependency requiring at least a version.
     * 
     * @param pluginId   the plugin ID
     * @param minVersion the minimum version required
     * @return a PluginDependency with minimum version
     */
    public static PluginDependency atLeast(String pluginId, String minVersion) {
        return new PluginDependency(pluginId, minVersion + "+");
    }

    /**
     * Creates a dependency with a version range.
     * 
     * @param pluginId   the plugin ID
     * @param minVersion the minimum version (inclusive)
     * @param maxVersion the maximum version (inclusive)
     * @return a PluginDependency with version range
     */
    public static PluginDependency range(String pluginId, String minVersion, String maxVersion) {
        return new PluginDependency(pluginId, minVersion + " - " + maxVersion);
    }

    /**
     * Creates a dependency compatible with a major version.
     * 
     * <p>
     * Uses caret semantics: ^1.0.0 means compatible with 1.x.x
     * </p>
     * 
     * @param pluginId     the plugin ID
     * @param majorVersion the major version
     * @return a PluginDependency compatible with the major version
     */
    public static PluginDependency compatible(String pluginId, String majorVersion) {
        return new PluginDependency(pluginId, "^" + majorVersion);
    }

    /**
     * Checks if this dependency is satisfied by the given version.
     * 
     * @param availableVersion the version to check
     * @return true if the version satisfies this dependency
     */
    public boolean isSatisfiedBy(String availableVersion) {
        if (availableVersion == null) {
            return false;
        }

        // Exact version
        if (versionRequirement.equals(availableVersion)) {
            return true;
        }

        // At least version (e.g., "1.0.0+" or ">=1.0.0")
        if (versionRequirement.endsWith("+") || versionRequirement.startsWith(">=")) {
            String min = versionRequirement.startsWith(">=")
                    ? versionRequirement.substring(2)
                    : versionRequirement.substring(0, versionRequirement.length() - 1);
            return compareVersions(availableVersion, min) >= 0;
        }

        // Range (e.g., "1.0.0 - 2.0.0")
        if (versionRequirement.contains(" - ")) {
            String[] parts = versionRequirement.split(" - ");
            if (parts.length == 2) {
                String min = parts[0].trim();
                String max = parts[1].trim();
                return compareVersions(availableVersion, min) >= 0 &&
                        compareVersions(availableVersion, max) <= 0;
            }
        }

        // Tilde (~1.0.0) - patch compatible (same major.minor)
        if (versionRequirement.startsWith("~")) {
            String min = versionRequirement.substring(1);
            String[] minParts = min.split("\\.");
            String[] availParts = availableVersion.split("\\.");
            // Must match major and minor, allow any patch
            boolean majorMatch = minParts.length > 0 && availParts.length > 0
                    && minParts[0].equals(availParts[0]);
            boolean minorMatch = minParts.length > 1 && availParts.length > 1
                    && minParts[1].equals(availParts[1]);
            return majorMatch && minorMatch && compareVersions(availableVersion, min) >= 0;
        }

        // Caret (^1.0.0) - major compatible
        if (versionRequirement.startsWith("^")) {
            String min = versionRequirement.substring(1);
            String[] minParts = min.split("\\.");
            String[] availParts = availableVersion.split("\\.");
            return minParts[0].equals(availParts[0]);
        }

        return false;
    }

    /**
     * Compares two semantic version strings.
     * 
     * @param v1 first version
     * @param v2 second version
     * @return negative if v1 < v2, positive if v1 > v2, zero if equal
     */
    private int compareVersions(String v1, String v2) {
        String[] p1 = v1.split("\\.");
        String[] p2 = v2.split("\\.");

        for (int i = 0; i < Math.max(p1.length, p2.length); i++) {
            int n1 = i < p1.length ? parseVersionPart(p1[i]) : 0;
            int n2 = i < p2.length ? parseVersionPart(p2[i]) : 0;

            if (n1 != n2) {
                return Integer.compare(n1, n2);
            }
        }
        return 0;
    }

    private int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part.replaceAll("\\D.*", ""));
        } catch (NumberFormatException _) {
            return 0;
        }
    }

    /**
     * Gets a human-readable description of this dependency.
     * 
     * @return description string
     */
    public String getDescription() {
        return String.format("Plugin %s version %s", pluginId, versionRequirement);
    }
}
