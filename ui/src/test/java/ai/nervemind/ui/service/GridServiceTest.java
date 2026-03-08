/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GridService")
class GridServiceTest {

    @Test
    @DisplayName("default constructor should initialize expected defaults")
    void defaultConstructorShouldInitializeExpectedDefaults() {
        GridService service = new GridService();

        assertThat(service.getGridSize()).isEqualTo(GridService.DEFAULT_GRID_SIZE);
        assertThat(service.isShowGrid()).isTrue();
        assertThat(service.isSnapToGrid()).isTrue();
    }

    @Test
    @DisplayName("custom constructor should set grid size")
    void customConstructorShouldSetGridSize() {
        GridService service = new GridService(32.0);

        assertThat(service.getGridSize()).isEqualTo(32.0);
    }

    @Test
    @DisplayName("snap should round to nearest grid when enabled")
    void snapShouldRoundToNearestGridWhenEnabled() {
        GridService service = new GridService(20.0);

        assertThat(service.snap(9.9)).isEqualTo(0.0);
        assertThat(service.snap(10.1)).isEqualTo(20.0);
        assertThat(service.snap(39.9)).isEqualTo(40.0);
    }

    @Test
    @DisplayName("snap should return original when snapping disabled")
    void snapShouldReturnOriginalWhenSnappingDisabled() {
        GridService service = new GridService(20.0);
        service.setSnapToGrid(false);

        assertThat(service.snap(13.37)).isEqualTo(13.37);
        assertThat(service.snap(13.37, 27.13)).containsExactly(13.37, 27.13);
    }

    @Test
    @DisplayName("nearestGridPoint should compute nearest intersection")
    void nearestGridPointShouldComputeNearestIntersection() {
        GridService service = new GridService(10.0);

        assertThat(service.nearestGridPoint(14.9, 15.1)).containsExactly(10.0, 20.0);
    }

    @Test
    @DisplayName("cell utilities should compute indices and origin")
    void cellUtilitiesShouldComputeIndicesAndOrigin() {
        GridService service = new GridService(20.0);

        assertThat(service.getCellIndices(39.9, 40.0)).containsExactly(1, 2);
        assertThat(service.getCellOrigin(3, 4)).containsExactly(60.0, 80.0);
    }

    @Test
    @DisplayName("toggle operations should invert state")
    void toggleOperationsShouldInvertState() {
        GridService service = new GridService();

        boolean initialShow = service.isShowGrid();
        boolean initialSnap = service.isSnapToGrid();

        service.toggleShowGrid();
        service.toggleSnapToGrid();

        assertThat(service.isShowGrid()).isEqualTo(!initialShow);
        assertThat(service.isSnapToGrid()).isEqualTo(!initialSnap);
    }

    @Test
    @DisplayName("setGridSize should ignore non-positive values")
    void setGridSizeShouldIgnoreNonPositiveValues() {
        GridService service = new GridService(15.0);

        service.setGridSize(0);
        assertThat(service.getGridSize()).isEqualTo(15.0);

        service.setGridSize(-10);
        assertThat(service.getGridSize()).isEqualTo(15.0);

        service.setGridSize(25);
        assertThat(service.getGridSize()).isEqualTo(25.0);
    }

    @Test
    @DisplayName("setters and property accessors should stay in sync")
    void settersAndPropertyAccessorsShouldStayInSync() {
        GridService service = new GridService();

        service.setShowGrid(false);
        service.setSnapToGrid(false);
        service.gridSizeProperty().set(30.0);

        assertThat(service.showGridProperty().get()).isFalse();
        assertThat(service.snapToGridProperty().get()).isFalse();
        assertThat(service.getGridSize()).isEqualTo(30.0);
    }

    @Test
    @DisplayName("grid line count and offset should match expected math")
    void gridLineCountAndOffsetShouldMatchExpectedMath() {
        GridService service = new GridService(20.0);

        assertThat(service.getGridLineCount(100.0)).isEqualTo(6);
        assertThat(service.getGridOffset(5.0)).isEqualTo(5.0);
        assertThat(service.getGridOffset(-5.0)).isEqualTo(15.0);
        assertThat(service.getGridOffset(40.0)).isEqualTo(0.0);
    }
}
