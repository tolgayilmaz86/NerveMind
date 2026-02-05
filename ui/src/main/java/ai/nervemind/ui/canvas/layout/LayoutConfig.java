package ai.nervemind.ui.canvas.layout;

/**
 * Configuration for auto-layout algorithm.
 * 
 * <p>
 * This record provides all configurable parameters for the layout engine.
 * </p>
 *
 * @param startX           starting X position for the first column
 * @param startY           starting Y position for nodes
 * @param baseXSpacing     horizontal spacing between columns
 * @param baseYSpacing     vertical spacing between nodes (normal density)
 * @param compactYSpacing  vertical spacing when column has many nodes
 * @param compactThreshold number of nodes in a column to trigger compact mode
 * @param gridSize         grid size for snapping (0 to disable)
 */
public record LayoutConfig(
        double startX,
        double startY,
        double baseXSpacing,
        double baseYSpacing,
        double compactYSpacing,
        int compactThreshold,
        double gridSize) {

    /**
     * Create a default layout configuration.
     *
     * @return default configuration
     */
    public static LayoutConfig defaults() {
        return new LayoutConfig(
                100, // startX
                100, // startY
                280, // baseXSpacing
                100, // baseYSpacing
                90, // compactYSpacing
                6, // compactThreshold
                20 // gridSize
        );
    }

    /**
     * Create a compact layout configuration for dense workflows.
     *
     * @return compact configuration
     */
    public static LayoutConfig compact() {
        return new LayoutConfig(
                50, // startX
                50, // startY
                220, // baseXSpacing
                80, // baseYSpacing
                70, // compactYSpacing
                4, // compactThreshold
                10 // gridSize
        );
    }

    /**
     * Create a spacious layout configuration for simple workflows.
     *
     * @return spacious configuration
     */
    public static LayoutConfig spacious() {
        return new LayoutConfig(
                150, // startX
                150, // startY
                350, // baseXSpacing
                130, // baseYSpacing
                110, // compactYSpacing
                8, // compactThreshold
                20 // gridSize
        );
    }

    /**
     * Create a builder for custom configuration.
     *
     * @return new builder starting from defaults
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating custom layout configurations.
     */
    public static class Builder {
        private double startX = 100;
        private double startY = 100;
        private double baseXSpacing = 280;
        private double baseYSpacing = 100;
        private double compactYSpacing = 90;

        /**
         * Default constructor for Builder.
         */
        public Builder() {
            // Default constructor
        }

        private int compactThreshold = 6;
        private double gridSize = 20;

        /**
         * Set the start X position.
         *
         * @param startX the start X position
         * @return this builder
         */
        public Builder startX(double startX) {
            this.startX = startX;
            return this;
        }

        /**
         * Set the start Y position.
         *
         * @param startY the start Y position
         * @return this builder
         */
        public Builder startY(double startY) {
            this.startY = startY;
            return this;
        }

        /**
         * Set the base X spacing.
         *
         * @param baseXSpacing the base X spacing
         * @return this builder
         */
        public Builder baseXSpacing(double baseXSpacing) {
            this.baseXSpacing = baseXSpacing;
            return this;
        }

        /**
         * Set the base Y spacing.
         *
         * @param baseYSpacing the base Y spacing
         * @return this builder
         */
        public Builder baseYSpacing(double baseYSpacing) {
            this.baseYSpacing = baseYSpacing;
            return this;
        }

        /**
         * Set the compact Y spacing.
         *
         * @param compactYSpacing the compact Y spacing
         * @return this builder
         */
        public Builder compactYSpacing(double compactYSpacing) {
            this.compactYSpacing = compactYSpacing;
            return this;
        }

        /**
         * Set the compact threshold.
         *
         * @param compactThreshold the compact threshold
         * @return this builder
         */
        public Builder compactThreshold(int compactThreshold) {
            this.compactThreshold = compactThreshold;
            return this;
        }

        /**
         * Set the grid size.
         *
         * @param gridSize the grid size
         * @return this builder
         */
        public Builder gridSize(double gridSize) {
            this.gridSize = gridSize;
            return this;
        }

        /**
         * Build the LayoutConfig.
         *
         * @return the built LayoutConfig
         */
        public LayoutConfig build() {
            return new LayoutConfig(startX, startY, baseXSpacing, baseYSpacing,
                    compactYSpacing, compactThreshold, gridSize);
        }
    }
}
