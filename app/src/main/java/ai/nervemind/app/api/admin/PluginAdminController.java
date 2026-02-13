package ai.nervemind.app.api.admin;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ai.nervemind.app.service.PluginService;
import ai.nervemind.plugin.api.PluginHandle;
import ai.nervemind.plugin.api.PluginProvider;

/**
 * Admin API controller for plugin management and development.
 * 
 * <p>
 * Provides REST endpoints for:
 * </p>
 * <ul>
 * <li>Listing all installed plugins</li>
 * <li>Plugin health and status monitoring</li>
 * <li>Debug information</li>
 * </ul>
 * 
 * <h2>API Endpoints</h2>
 * 
 * <pre>{@code
 * GET  /api/admin/plugins              - List all plugins
 * GET  /api/admin/plugins/triggers    - List triggers
 * GET  /api/admin/plugins/actions     - List actions
 * GET  /api/admin/plugins/providers   - List unified providers
 * GET  /api/admin/plugins/health      - Health report
 * GET  /api/admin/debug               - Debug info
 * }</pre>
 */
@RestController
@RequestMapping("/api/admin/plugins")
public class PluginAdminController {

    private final PluginService pluginService;

    public PluginAdminController(PluginService pluginService) {
        this.pluginService = pluginService;
    }

    /**
     * List all installed plugins (both legacy and unified API).
     */
    @GetMapping
    public ResponseEntity<AllPluginsResponse> listAllPlugins() {
        List<PluginSummary> plugins = new ArrayList<>();

        // Add unified PluginProviders
        for (PluginProvider provider : pluginService.getPluginProviders()) {
            plugins.add(createPluginSummary(provider));
        }

        return ResponseEntity.ok(new AllPluginsResponse(
                plugins.size(),
                plugins,
                Instant.now().toString()));
    }

    /**
     * Get all trigger plugins.
     */
    @GetMapping("/triggers")
    public ResponseEntity<List<PluginSummary>> listTriggers() {
        List<PluginSummary> plugins = new ArrayList<>();

        for (var trigger : pluginService.getTriggerProviders()) {
            plugins.add(new PluginSummary(
                    trigger.getNodeType(),
                    trigger.getDisplayName(),
                    trigger.getDescription(),
                    "TRIGGER",
                    "TRIGGER",
                    true));
        }

        return ResponseEntity.ok(plugins);
    }

    /**
     * Get all action plugins.
     */
    @GetMapping("/actions")
    public ResponseEntity<List<PluginSummary>> listActions() {
        List<PluginSummary> plugins = new ArrayList<>();

        for (var action : pluginService.getActionProviders()) {
            plugins.add(new PluginSummary(
                    action.getNodeType(),
                    action.getDisplayName(),
                    action.getDescription(),
                    "ACTION",
                    "ACTION",
                    true));
        }

        return ResponseEntity.ok(plugins);
    }

    /**
     * Get unified plugin providers.
     */
    @GetMapping("/providers")
    public ResponseEntity<List<PluginSummary>> listProviders() {
        List<PluginSummary> plugins = new ArrayList<>();

        for (PluginProvider provider : pluginService.getPluginProviders()) {
            plugins.add(createPluginSummary(provider));
        }

        return ResponseEntity.ok(plugins);
    }

    /**
     * Get plugin health report with actual health checks.
     */
    @GetMapping("/health")
    public ResponseEntity<HealthReport> getHealthReport() {
        Map<String, String> health = new LinkedHashMap<>();
        int healthy = 0;
        int unhealthy = 0;
        int total = 0;

        // Check PluginProviders
        for (PluginProvider provider : pluginService.getPluginProviders()) {
            total++;
            String status = checkPluginHealth(provider);
            health.put(provider.getId(), status);
            if ("healthy".equals(status)) {
                healthy++;
            } else {
                unhealthy++;
            }
        }

        // Check TriggerProviders
        for (var trigger : pluginService.getTriggerProviders()) {
            if (!health.containsKey(trigger.getNodeType())) {
                total++;
                String status = checkNodeDescriptorHealth(trigger);
                health.put(trigger.getNodeType(), status);
                if ("healthy".equals(status)) {
                    healthy++;
                } else {
                    unhealthy++;
                }
            }
        }

        // Check ActionProviders
        for (var action : pluginService.getActionProviders()) {
            if (!health.containsKey(action.getNodeType())) {
                total++;
                String status = checkNodeDescriptorHealth(action);
                health.put(action.getNodeType(), status);
                if ("healthy".equals(status)) {
                    healthy++;
                } else {
                    unhealthy++;
                }
            }
        }

        return ResponseEntity.ok(new HealthReport(
                health,
                Map.of("total", total, "healthy", healthy, "unhealthy", unhealthy),
                Instant.now().toString()));
    }

    /**
     * Performs actual health checks on a PluginProvider.
     */
    private String checkPluginHealth(PluginProvider provider) {
        try {
            // Verify plugin has required metadata
            if (provider.getId() == null || provider.getId().isBlank()) {
                return "unhealthy: missing plugin ID";
            }
            if (provider.getName() == null || provider.getName().isBlank()) {
                return "unhealthy: missing plugin name";
            }

            // Verify handles are accessible
            var handles = provider.getHandles();
            if (handles == null || handles.isEmpty()) {
                return "warning: no handles defined";
            }

            // Verify each handle has an executor
            for (PluginHandle handle : handles) {
                if (handle.executor() == null && handle.schema() == null) {
                    return "warning: handle '" + handle.id() + "' has no executor";
                }
            }

            return "healthy";
        } catch (Exception e) {
            return "unhealthy: " + e.getMessage();
        }
    }

    /**
     * Performs health checks on a legacy NodeDescriptor
     * (TriggerProvider/ActionProvider).
     */
    private String checkNodeDescriptorHealth(ai.nervemind.plugin.api.NodeDescriptor descriptor) {
        try {
            if (descriptor.getNodeType() == null || descriptor.getNodeType().isBlank()) {
                return "unhealthy: missing node type";
            }
            if (descriptor.getDisplayName() == null || descriptor.getDisplayName().isBlank()) {
                return "unhealthy: missing display name";
            }
            return "healthy";
        } catch (Exception e) {
            return "unhealthy: " + e.getMessage();
        }
    }

    /**
     * Get debug information.
     */
    @GetMapping("/debug")
    public ResponseEntity<DebugInfo> getDebugInfo() {
        int providerCount = pluginService.getPluginProviders().size();
        int triggerCount = pluginService.getTriggerProviders().size();
        int actionCount = pluginService.getActionProviders().size();

        DebugInfo info = new DebugInfo(
                providerCount,
                triggerCount,
                actionCount,
                System.currentTimeMillis());

        return ResponseEntity.ok(info);
    }

    // ===== Helper Methods =====

    private PluginSummary createPluginSummary(PluginProvider provider) {
        String type = "PLUGIN";
        String category = "PLUGIN";

        if (!provider.getHandles().isEmpty()) {
            var firstHandle = provider.getHandles().get(0);
            type = firstHandle.category() != null ? firstHandle.category().name() : "PLUGIN";
        }

        return new PluginSummary(
                provider.getId(),
                provider.getName(),
                provider.getDescription(),
                type,
                category,
                true);
    }

    // ===== Response Records =====

    public record AllPluginsResponse(
            int count,
            List<PluginSummary> plugins,
            String timestamp) {
    }

    public record PluginSummary(
            String id,
            String name,
            String description,
            String type,
            String category,
            boolean loaded) {
    }

    public record HealthReport(
            Map<String, String> plugins,
            Map<String, Integer> summary,
            String generatedAt) {
    }

    public record DebugInfo(
            int providerCount,
            int triggerCount,
            int actionCount,
            long serverTime) {
    }
}
