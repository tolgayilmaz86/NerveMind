package ai.nervemind.ui.canvas.layout;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link LayoutConfig}.
 */
class LayoutConfigTest {

    @Test
    @DisplayName("defaults should return expected default values")
    void defaults() {
        LayoutConfig config = LayoutConfig.defaults();

        assertThat(config.startX()).isEqualTo(100);
        assertThat(config.startY()).isEqualTo(100);
        assertThat(config.baseXSpacing()).isEqualTo(280);
        assertThat(config.baseYSpacing()).isEqualTo(100);
        assertThat(config.compactYSpacing()).isEqualTo(90);
        assertThat(config.compactThreshold()).isEqualTo(6);
        assertThat(config.gridSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("compact should return tighter spacing values")
    void compact() {
        LayoutConfig config = LayoutConfig.compact();

        assertThat(config.startX()).isEqualTo(50);
        assertThat(config.startY()).isEqualTo(50);
        assertThat(config.baseXSpacing()).isEqualTo(220);
        assertThat(config.baseYSpacing()).isEqualTo(80);
        assertThat(config.compactYSpacing()).isEqualTo(70);
        assertThat(config.compactThreshold()).isEqualTo(4);
        assertThat(config.gridSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("spacious should return wider spacing values")
    void spacious() {
        LayoutConfig config = LayoutConfig.spacious();

        assertThat(config.startX()).isEqualTo(150);
        assertThat(config.startY()).isEqualTo(150);
        assertThat(config.baseXSpacing()).isEqualTo(350);
        assertThat(config.baseYSpacing()).isEqualTo(130);
        assertThat(config.compactYSpacing()).isEqualTo(110);
        assertThat(config.compactThreshold()).isEqualTo(8);
        assertThat(config.gridSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("builder should create config with custom values")
    void builderCustomValues() {
        LayoutConfig config = LayoutConfig.builder()
                .startX(50)
                .startY(75)
                .baseXSpacing(200)
                .baseYSpacing(80)
                .compactYSpacing(60)
                .compactThreshold(4)
                .gridSize(10)
                .build();

        assertThat(config.startX()).isEqualTo(50);
        assertThat(config.startY()).isEqualTo(75);
        assertThat(config.baseXSpacing()).isEqualTo(200);
        assertThat(config.baseYSpacing()).isEqualTo(80);
        assertThat(config.compactYSpacing()).isEqualTo(60);
        assertThat(config.compactThreshold()).isEqualTo(4);
        assertThat(config.gridSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("builder should use defaults for unset values")
    void builderDefaultsForUnset() {
        LayoutConfig config = LayoutConfig.builder()
                .startX(200) // Only set this
                .build();

        assertThat(config.startX()).isEqualTo(200);
        assertThat(config.startY()).isEqualTo(100); // Default
        assertThat(config.baseXSpacing()).isEqualTo(280); // Default
    }

    @Test
    @DisplayName("config should be immutable record")
    void immutableRecord() {
        LayoutConfig config1 = LayoutConfig.defaults();
        LayoutConfig config2 = LayoutConfig.defaults();

        // Records with same values should be equal
        assertThat(config1).isEqualTo(config2);
        assertThat(config1.hashCode()).isEqualTo(config2.hashCode());
    }
}
