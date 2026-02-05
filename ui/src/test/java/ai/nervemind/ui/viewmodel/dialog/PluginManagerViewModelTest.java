/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.dialog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ai.nervemind.common.enums.NodeCategory;
import ai.nervemind.common.enums.PluginType;
import ai.nervemind.common.service.PluginServiceInterface;
import ai.nervemind.common.service.PluginServiceInterface.PluginInfo;
import ai.nervemind.ui.viewmodel.ViewModelTestBase;

/**
 * Unit tests for PluginManagerViewModel.
 */
@DisplayName("PluginManagerViewModel")
class PluginManagerViewModelTest extends ViewModelTestBase {

    private PluginServiceInterface pluginService;
    private PluginManagerViewModel viewModel;

    private static final PluginInfo TRIGGER_PLUGIN = new PluginInfo(
            "ai.nervemind.plugin.trigger1",
            "Schedule Trigger",
            "Triggers workflows on schedule",
            "1.0.0",
            PluginType.TRIGGER,
            true,
            null,
            NodeCategory.TRIGGER,
            "trigger",
            "Triggers workflows on schedule");

    private static final PluginInfo ACTION_PLUGIN = new PluginInfo(
            "ai.nervemind.plugin.action1",
            "HTTP Action",
            "Makes HTTP requests",
            "1.0.0",
            PluginType.ACTION,
            true,
            null,
            NodeCategory.ACTION,
            "action",
            "Makes HTTP requests");

    private static final PluginInfo EXTERNAL_PLUGIN = new PluginInfo(
            "com.external.plugin",
            "External Plugin",
            "Third party plugin",
            "2.0.0",
            PluginType.ACTION,
            false,
            null,
            NodeCategory.ACTION,
            "plugin",
            "Third party plugin");

    @BeforeEach
    void setUp() {
        pluginService = mock(PluginServiceInterface.class);
        when(pluginService.getAllDiscoveredPlugins()).thenReturn(
                List.of(TRIGGER_PLUGIN, ACTION_PLUGIN, EXTERNAL_PLUGIN));
        viewModel = new PluginManagerViewModel(pluginService);
    }

    @Nested
    @DisplayName("Initialization")
    class Initialization {

        @Test
        @DisplayName("Should load plugins on initialization")
        void shouldLoadPluginsOnInitialization() {
            assertThat(viewModel.getAllPlugins()).hasSize(3);
        }

        @Test
        @DisplayName("Should separate trigger plugins")
        void shouldSeparateTriggerPlugins() {
            assertThat(viewModel.getTriggerPlugins())
                    .hasSize(1)
                    .first()
                    .extracting(PluginInfo::name)
                    .isEqualTo("Schedule Trigger");
        }

        @Test
        @DisplayName("Should separate action plugins")
        void shouldSeparateActionPlugins() {
            assertThat(viewModel.getActionPlugins()).hasSize(2);
        }

        @Test
        @DisplayName("Should have empty search query initially")
        void shouldHaveEmptySearchQuery() {
            assertThat(viewModel.searchQueryProperty().get()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Search Filtering")
    class SearchFiltering {

        @Test
        @DisplayName("Should filter plugins by name")
        void shouldFilterPluginsByName() {
            viewModel.searchQueryProperty().set("HTTP");

            assertThat(viewModel.getActionPlugins())
                    .hasSize(1)
                    .first()
                    .extracting(PluginInfo::name)
                    .isEqualTo("HTTP Action");
        }

        @Test
        @DisplayName("Should filter plugins by description")
        void shouldFilterPluginsByDescription() {
            viewModel.searchQueryProperty().set("schedule");

            assertThat(viewModel.getTriggerPlugins()).hasSize(1);
        }

        @Test
        @DisplayName("Should filter plugins by ID")
        void shouldFilterPluginsById() {
            viewModel.searchQueryProperty().set("external");

            assertThat(viewModel.getActionPlugins())
                    .hasSize(1)
                    .first()
                    .extracting(PluginInfo::id)
                    .asString()
                    .contains("external");
        }

        @Test
        @DisplayName("Should be case insensitive")
        void shouldBeCaseInsensitive() {
            viewModel.searchQueryProperty().set("HTTP");
            int upperCaseCount = viewModel.getActionPlugins().size();

            viewModel.searchQueryProperty().set("http");
            int lowerCaseCount = viewModel.getActionPlugins().size();

            assertThat(upperCaseCount).isEqualTo(lowerCaseCount);
        }

        @Test
        @DisplayName("Should show all plugins when search cleared")
        void shouldShowAllPluginsWhenSearchCleared() {
            viewModel.searchQueryProperty().set("HTTP");
            viewModel.searchQueryProperty().set("");

            assertThat(viewModel.getActionPlugins()).hasSize(2);
            assertThat(viewModel.getTriggerPlugins()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Plugin Type Detection")
    class PluginTypeDetection {

        @Test
        @DisplayName("Should detect trigger plugins")
        void shouldDetectTriggerPlugins() {
            assertThat(viewModel.isTriggerPlugin(TRIGGER_PLUGIN)).isTrue();
            assertThat(viewModel.isTriggerPlugin(ACTION_PLUGIN)).isFalse();
        }
    }

    @Nested
    @DisplayName("Status Text")
    class StatusText {

        @Test
        @DisplayName("Should show plugin count in status")
        void shouldShowPluginCountInStatus() {
            String status = viewModel.statusTextProperty().get();

            assertThat(status).contains("3");
            assertThat(status).contains("plugins");
        }

        @Test
        @DisplayName("Should show enabled count in status")
        void shouldShowEnabledCountInStatus() {
            String status = viewModel.statusTextProperty().get();

            // 2 plugins are enabled (TRIGGER_PLUGIN and ACTION_PLUGIN)
            assertThat(status).contains("2");
            assertThat(status).contains("enabled");
        }
    }

    @Nested
    @DisplayName("Plugin Actions")
    class PluginActions {

        @Test
        @DisplayName("Should mark dirty when enabling plugin")
        void shouldMarkDirtyWhenEnablingPlugin() {
            viewModel.setPluginEnabled("ai.nervemind.plugin.trigger1", false);

            assertThat(viewModel.isDirty()).isTrue();
        }

        @Test
        @DisplayName("Should get plugins folder path")
        void shouldGetPluginsFolderPath() {
            String path = viewModel.getPluginsFolderPath();

            assertThat(path).isNotEmpty();
            assertThat(path).contains("plugins");
        }
    }

    @Nested
    @DisplayName("Refresh")
    class Refresh {

        @Test
        @DisplayName("Should refresh plugins from service")
        void shouldRefreshPluginsFromService() {
            when(pluginService.getAllDiscoveredPlugins()).thenReturn(
                    List.of(TRIGGER_PLUGIN));

            viewModel.refreshPlugins();

            assertThat(viewModel.getAllPlugins()).hasSize(1);
        }
    }
}
