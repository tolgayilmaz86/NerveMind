/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;

@DisplayName("IconResolver")
class IconResolverTest {

    @Test
    @DisplayName("should return default icon for null or blank input")
    void shouldReturnDefaultIconForNullOrBlankInput() {
        Ikon defaultIcon = IconResolver.getDefaultIcon();

        assertThat(IconResolver.resolve(null)).isEqualTo(defaultIcon);
        assertThat(IconResolver.resolve("")).isEqualTo(defaultIcon);
        assertThat(IconResolver.resolve("   ")).isEqualTo(defaultIcon);
    }

    @Test
    @DisplayName("should resolve valid icon with case and separator normalization")
    void shouldResolveValidIconWithCaseAndSeparatorNormalization() {
        assertThat(IconResolver.resolve("puzzle")).isEqualTo(MaterialDesignP.PUZZLE);
        assertThat(IconResolver.resolve("PUZZLE")).isEqualTo(MaterialDesignP.PUZZLE);
        assertThat(IconResolver.resolve("play-circle")).isEqualTo(MaterialDesignP.PLAY_CIRCLE);
        assertThat(IconResolver.resolve("play circle")).isEqualTo(MaterialDesignP.PLAY_CIRCLE);
    }

    @Test
    @DisplayName("should return default icon for unknown icon names")
    void shouldReturnDefaultIconForUnknownIconNames() {
        Ikon defaultIcon = IconResolver.getDefaultIcon();

        assertThat(IconResolver.resolve("NOT_A_REAL_ICON")).isEqualTo(defaultIcon);
        assertThat(IconResolver.resolve("1INVALID")).isEqualTo(defaultIcon);
    }

    @Test
    @DisplayName("should return default icon for unknown names across all first-letter branches")
    void shouldReturnDefaultIconAcrossAllFirstLetterBranches() {
        Ikon defaultIcon = IconResolver.getDefaultIcon();

        for (char c = 'A'; c <= 'Z'; c++) {
            String unknownForBranch = c + "_THIS_ICON_DOES_NOT_EXIST";
            assertThat(IconResolver.resolve(unknownForBranch))
                    .as("Expected default icon for branch %s", c)
                    .isEqualTo(defaultIcon);
        }
    }

    @Test
    @DisplayName("getDefaultIcon should expose puzzle icon")
    void getDefaultIconShouldExposePuzzleIcon() {
        assertThat(IconResolver.getDefaultIcon()).isEqualTo(MaterialDesignP.PUZZLE);
    }
}
