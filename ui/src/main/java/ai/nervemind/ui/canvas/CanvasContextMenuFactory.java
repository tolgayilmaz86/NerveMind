/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.canvas;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignB;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignD;
import org.kordamp.ikonli.materialdesign2.MaterialDesignE;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignG;
import org.kordamp.ikonli.materialdesign2.MaterialDesignH;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;
import org.kordamp.ikonli.materialdesign2.MaterialDesignM;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignR;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;
import org.kordamp.ikonli.materialdesign2.MaterialDesignT;
import org.kordamp.ikonli.materialdesign2.MaterialDesignU;
import org.kordamp.ikonli.materialdesign2.MaterialDesignV;
import org.kordamp.ikonli.materialdesign2.MaterialDesignW;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

/**
 * Factory for creating context menus used on the workflow canvas.
 *
 * <p>
 * Provides factory methods for creating context menus for:
 * <ul>
 * <li>Individual nodes</li>
 * <li>Multiple selected nodes</li>
 * <li>Connections</li>
 * <li>Canvas background</li>
 * </ul>
 *
 * <p>
 * This factory extracts context menu creation logic from view classes
 * to improve code organization and testability.
 */
public final class CanvasContextMenuFactory {

    private static final int ICON_SIZE = 14;

    private CanvasContextMenuFactory() {
        // Utility class - prevent instantiation
    }

    // ===== Node Context Menu =====

    /**
     * Creates a context menu for a single node.
     *
     * @param callbacks the callbacks for menu actions
     * @param disabled  whether the node is currently disabled
     * @return the context menu
     */
    public static ContextMenu createNodeContextMenu(NodeContextMenuCallbacks callbacks, boolean disabled) {
        ContextMenu menu = new ContextMenu();

        // Edit actions
        MenuItem editItem = createMenuItem("Open Editor", MaterialDesignP.PENCIL, callbacks::onOpenEditor);
        MenuItem executeItem = createMenuItem("Execute Node", MaterialDesignP.PLAY, callbacks::onExecute);
        MenuItem debugItem = createMenuItem("Debug View...", MaterialDesignB.BUG_OUTLINE, callbacks::onDebugView);

        // Organization
        MenuItem duplicateItem = createMenuItem("Duplicate", MaterialDesignC.CONTENT_COPY, callbacks::onDuplicate);
        MenuItem toggleItem = createMenuItem(
                disabled ? "Enable" : "Disable",
                disabled ? MaterialDesignE.EYE : MaterialDesignE.EYE_OFF,
                callbacks::onToggleEnabled);
        MenuItem renameItem = createMenuItem("Rename", MaterialDesignR.RENAME_BOX, callbacks::onRename);
        MenuItem changeIconItem = createMenuItem("Change Icon...", MaterialDesignP.PALETTE, callbacks::onChangeIcon);

        // Connections
        MenuItem deleteConnectionsItem = createMenuItem("Delete All Connections",
                MaterialDesignL.LINK_OFF, callbacks::onDeleteConnections);

        // Delete
        MenuItem deleteItem = createMenuItem("Delete", MaterialDesignD.DELETE, callbacks::onDelete);
        deleteItem.setStyle("-fx-text-fill: #fa5252;");

        menu.getItems().addAll(
                editItem,
                executeItem,
                debugItem,
                new SeparatorMenuItem(),
                duplicateItem,
                toggleItem,
                renameItem,
                changeIconItem,
                new SeparatorMenuItem(),
                deleteConnectionsItem,
                deleteItem);

        return menu;
    }

    /**
     * Creates a context menu for multiple selected nodes.
     *
     * @param callbacks the callbacks for menu actions
     * @param nodeCount the number of selected nodes
     * @return the context menu
     */
    public static ContextMenu createMultiNodeContextMenu(MultiNodeContextMenuCallbacks callbacks, int nodeCount) {
        ContextMenu menu = new ContextMenu();

        // Alignment submenu
        Menu alignMenu = new Menu("Align");
        alignMenu.setGraphic(FontIcon.of(MaterialDesignA.ALIGN_HORIZONTAL_LEFT, ICON_SIZE));

        MenuItem alignLeftItem = createMenuItem("Align Left", MaterialDesignA.ALIGN_HORIZONTAL_LEFT,
                callbacks::onAlignLeft);
        MenuItem alignRightItem = createMenuItem("Align Right", MaterialDesignA.ALIGN_HORIZONTAL_RIGHT,
                callbacks::onAlignRight);
        MenuItem alignTopItem = createMenuItem("Align Top", MaterialDesignA.ALIGN_VERTICAL_TOP,
                callbacks::onAlignTop);
        MenuItem alignBottomItem = createMenuItem("Align Bottom", MaterialDesignA.ALIGN_VERTICAL_BOTTOM,
                callbacks::onAlignBottom);
        MenuItem alignHCenterItem = createMenuItem("Align Horizontal Center", MaterialDesignA.ALIGN_HORIZONTAL_CENTER,
                callbacks::onAlignHorizontalCenter);
        MenuItem alignVCenterItem = createMenuItem("Align Vertical Center", MaterialDesignA.ALIGN_VERTICAL_CENTER,
                callbacks::onAlignVerticalCenter);

        alignMenu.getItems().addAll(alignLeftItem, alignRightItem, alignTopItem, alignBottomItem,
                new SeparatorMenuItem(), alignHCenterItem, alignVCenterItem);

        // Distribute submenu
        Menu distributeMenu = new Menu("Distribute");
        distributeMenu.setGraphic(FontIcon.of(MaterialDesignF.FORMAT_HORIZONTAL_ALIGN_CENTER, ICON_SIZE));

        MenuItem distributeHItem = createMenuItem("Distribute Horizontally",
                MaterialDesignF.FORMAT_HORIZONTAL_ALIGN_CENTER, callbacks::onDistributeHorizontally);
        MenuItem distributeVItem = createMenuItem("Distribute Vertically",
                MaterialDesignF.FORMAT_VERTICAL_ALIGN_CENTER, callbacks::onDistributeVertically);

        distributeMenu.getItems().addAll(distributeHItem, distributeVItem);

        // Group actions
        MenuItem duplicateItem = createMenuItem("Duplicate " + nodeCount + " Nodes",
                MaterialDesignC.CONTENT_COPY, callbacks::onDuplicateAll);
        MenuItem enableAllItem = createMenuItem("Enable All", MaterialDesignE.EYE, callbacks::onEnableAll);
        MenuItem disableAllItem = createMenuItem("Disable All", MaterialDesignE.EYE_OFF, callbacks::onDisableAll);

        // Delete
        MenuItem deleteItem = createMenuItem("Delete " + nodeCount + " Nodes", MaterialDesignD.DELETE,
                callbacks::onDeleteAll);
        deleteItem.setStyle("-fx-text-fill: #fa5252;");

        menu.getItems().addAll(
                alignMenu,
                distributeMenu,
                new SeparatorMenuItem(),
                duplicateItem,
                enableAllItem,
                disableAllItem,
                new SeparatorMenuItem(),
                deleteItem);

        return menu;
    }

    // ===== Connection Context Menu =====

    /**
     * Creates a context menu for a connection.
     *
     * @param callbacks the callbacks for menu actions
     * @return the context menu
     */
    public static ContextMenu createConnectionContextMenu(ConnectionContextMenuCallbacks callbacks) {
        ContextMenu menu = new ContextMenu();

        MenuItem selectSourceItem = createMenuItem("Select Source Node", MaterialDesignA.ARROW_LEFT,
                callbacks::onSelectSource);
        MenuItem selectTargetItem = createMenuItem("Select Target Node", MaterialDesignA.ARROW_RIGHT,
                callbacks::onSelectTarget);

        MenuItem deleteItem = createMenuItem("Delete Connection", MaterialDesignD.DELETE, callbacks::onDelete);
        deleteItem.setStyle("-fx-text-fill: #fa5252;");

        menu.getItems().addAll(
                selectSourceItem,
                selectTargetItem,
                new SeparatorMenuItem(),
                deleteItem);

        return menu;
    }

    // ===== Canvas Background Context Menu =====

    /**
     * Creates a context menu for the canvas background.
     *
     * @param callbacks    the callbacks for menu actions
     * @param hasClipboard whether the clipboard has content
     * @param canUndo      whether undo is available
     * @param canRedo      whether redo is available
     * @return the context menu
     */
    public static ContextMenu createCanvasContextMenu(CanvasContextMenuCallbacks callbacks,
            boolean hasClipboard, boolean canUndo, boolean canRedo) {
        ContextMenu menu = new ContextMenu();

        // Add node submenu (built-in nodes only - plugins appear in the node palette)
        Menu addNodeMenu = new Menu("Add Node");
        addNodeMenu.setGraphic(FontIcon.of(MaterialDesignP.PLUS, ICON_SIZE));

        Menu triggersMenu = new Menu("Triggers");
        triggersMenu.getItems().addAll(
                createMenuItem("Manual Trigger", MaterialDesignP.PLAY_CIRCLE,
                        () -> callbacks.onAddNode("manualTrigger")),
                createMenuItem("Schedule Trigger", MaterialDesignC.CLOCK_OUTLINE,
                        () -> callbacks.onAddNode("scheduleTrigger")),
                createMenuItem("Webhook Trigger", MaterialDesignW.WEBHOOK,
                        () -> callbacks.onAddNode("webhookTrigger")));

        Menu actionsMenu = new Menu("Actions");
        actionsMenu.getItems().addAll(
                createMenuItem("HTTP Request", MaterialDesignW.WEB, () -> callbacks.onAddNode("httpRequest")),
                createMenuItem("Code", MaterialDesignC.CODE_TAGS, () -> callbacks.onAddNode("code")),
                createMenuItem("Execute Command", MaterialDesignC.CONSOLE,
                        () -> callbacks.onAddNode("executeCommand")));

        Menu flowMenu = new Menu("Flow Control");
        flowMenu.getItems().addAll(
                createMenuItem("If", MaterialDesignH.HELP_RHOMBUS, () -> callbacks.onAddNode("if")),
                createMenuItem("Switch", MaterialDesignS.SOURCE_BRANCH, () -> callbacks.onAddNode("switch")),
                createMenuItem("Merge", MaterialDesignM.MERGE, () -> callbacks.onAddNode("merge")),
                createMenuItem("Loop", MaterialDesignR.REPEAT, () -> callbacks.onAddNode("loop")));

        Menu dataMenu = new Menu("Data");
        dataMenu.getItems().addAll(
                createMenuItem("Set", MaterialDesignV.VARIABLE, () -> callbacks.onAddNode("set")),
                createMenuItem("Filter", MaterialDesignF.FILTER, () -> callbacks.onAddNode("filter")),
                createMenuItem("Sort", MaterialDesignS.SORT, () -> callbacks.onAddNode("sort")));

        Menu aiMenu = new Menu("AI / ML");
        aiMenu.getItems().addAll(
                createMenuItem("LLM Chat", MaterialDesignC.CHAT_OUTLINE, () -> callbacks.onAddNode("llmChat")),
                createMenuItem("Text Classifier", MaterialDesignT.TEXT_BOX_SEARCH_OUTLINE,
                        () -> callbacks.onAddNode("textClassifier")),
                createMenuItem("Embedding", MaterialDesignV.VECTOR_COMBINE, () -> callbacks.onAddNode("embedding")),
                createMenuItem("RAG", MaterialDesignD.DATABASE_SEARCH, () -> callbacks.onAddNode("rag")));

        addNodeMenu.getItems().addAll(triggersMenu, actionsMenu, flowMenu, dataMenu, aiMenu);

        // Edit actions
        MenuItem pasteItem = createMenuItem("Paste", MaterialDesignC.CONTENT_PASTE, callbacks::onPaste);
        pasteItem.setDisable(!hasClipboard);

        MenuItem undoItem = createMenuItem("Undo", MaterialDesignU.UNDO, callbacks::onUndo);
        undoItem.setDisable(!canUndo);

        MenuItem redoItem = createMenuItem("Redo", MaterialDesignR.REDO, callbacks::onRedo);
        redoItem.setDisable(!canRedo);

        MenuItem selectAllItem = createMenuItem("Select All", MaterialDesignS.SELECT_ALL, callbacks::onSelectAll);

        // View actions
        MenuItem fitViewItem = createMenuItem("Fit to View", MaterialDesignF.FIT_TO_PAGE_OUTLINE,
                callbacks::onFitToView);
        MenuItem resetZoomItem = createMenuItem("Reset Zoom", MaterialDesignM.MAGNIFY_SCAN, callbacks::onResetZoom);

        // Grid toggle
        MenuItem toggleGridItem = createMenuItem("Toggle Grid", MaterialDesignG.GRID, callbacks::onToggleGrid);
        MenuItem toggleSnapItem = createMenuItem("Toggle Snap to Grid", MaterialDesignG.GRID_LARGE,
                callbacks::onToggleSnap);

        // Auto-layout
        MenuItem autoLayoutItem = createMenuItem("Auto Layout", MaterialDesignA.AUTO_FIX, callbacks::onAutoLayout);

        menu.getItems().addAll(
                addNodeMenu,
                new SeparatorMenuItem(),
                pasteItem,
                undoItem,
                redoItem,
                selectAllItem,
                new SeparatorMenuItem(),
                fitViewItem,
                resetZoomItem,
                toggleGridItem,
                toggleSnapItem,
                new SeparatorMenuItem(),
                autoLayoutItem);

        return menu;
    }

    // ===== Helper Methods =====

    /**
     * Creates a menu item with an icon and action.
     */
    private static MenuItem createMenuItem(String text, org.kordamp.ikonli.Ikon icon, Runnable action) {
        MenuItem item = new MenuItem(text);
        item.setGraphic(FontIcon.of(icon, ICON_SIZE));
        item.setOnAction(e -> action.run());
        return item;
    }

    // ===== Callback Interfaces =====

    /**
     * Callbacks for single node context menu actions.
     */
    public interface NodeContextMenuCallbacks {
        /** Open the node editor. */
        void onOpenEditor();

        /** Execute the node. */
        void onExecute();

        /** Open the debug view. */
        void onDebugView();

        /** Duplicate the node. */
        void onDuplicate();

        /** Toggle node enabled state. */
        void onToggleEnabled();

        /** Rename the node. */
        void onRename();

        /** Change the node icon. */
        void onChangeIcon();

        /** Delete all connections to/from this node. */
        void onDeleteConnections();

        /** Delete the node. */
        void onDelete();
    }

    /**
     * Callbacks for multi-node context menu actions.
     */
    public interface MultiNodeContextMenuCallbacks {
        /** Align nodes to the left. */
        void onAlignLeft();

        /** Align nodes to the right. */
        void onAlignRight();

        /** Align nodes to the top. */
        void onAlignTop();

        /** Align nodes to the bottom. */
        void onAlignBottom();

        /** Align nodes to horizontal center. */
        void onAlignHorizontalCenter();

        /** Align nodes to vertical center. */
        void onAlignVerticalCenter();

        /** Distribute nodes horizontally. */
        void onDistributeHorizontally();

        /** Distribute nodes vertically. */
        void onDistributeVertically();

        /** Duplicate all selected nodes. */
        void onDuplicateAll();

        /** Enable all selected nodes. */
        void onEnableAll();

        /** Disable all selected nodes. */
        void onDisableAll();

        /** Delete all selected nodes. */
        void onDeleteAll();
    }

    /**
     * Callbacks for connection context menu actions.
     */
    public interface ConnectionContextMenuCallbacks {
        /** Select the source node of the connection. */
        void onSelectSource();

        /** Select the target node of the connection. */
        void onSelectTarget();

        /** Delete the connection. */
        void onDelete();
    }

    /**
     * Callbacks for canvas background context menu actions.
     */
    public interface CanvasContextMenuCallbacks {
        /**
         * Add a new node of the given type.
         *
         * @param nodeType The type of node to add
         */
        void onAddNode(String nodeType);

        /** Paste nodes from clipboard. */
        void onPaste();

        /** Undo the last action. */
        void onUndo();

        /** Redo the last undone action. */
        void onRedo();

        /** Select all nodes. */
        void onSelectAll();

        /** Fit the canvas to view all nodes. */
        void onFitToView();

        /** Reset zoom level. */
        void onResetZoom();

        /** Toggle background grid. */
        void onToggleGrid();

        /** Toggle snap to grid. */
        void onToggleSnap();

        /** Auto-layout the nodes. */
        void onAutoLayout();
    }

    /**
     * Builder for creating NodeContextMenuCallbacks.
     */

    public static class NodeContextMenuCallbacksBuilder {
        private Runnable onOpenEditor = () -> {
        };
        private Runnable onExecute = () -> {
        };
        private Runnable onDebugView = () -> {
        };
        private Runnable onDuplicate = () -> {
        };
        private Runnable onToggleEnabled = () -> {
        };
        private Runnable onRename = () -> {
        };
        private Runnable onChangeIcon = () -> {
        };
        private Runnable onDeleteConnections = () -> {
        };
        private Runnable onDelete = () -> {
        };

        /**
         * Create a new builder.
         */
        public NodeContextMenuCallbacksBuilder() {
        }

        /**
         * Set the action for opening the editor.
         *
         * @param action the action to run
         * @return the builder instance
         */
        public NodeContextMenuCallbacksBuilder onOpenEditor(Runnable action) {
            this.onOpenEditor = action;
            return this;
        }

        /**
         * Set the action for executing the node.
         *
         * @param action the action to run
         * @return the builder instance
         */
        public NodeContextMenuCallbacksBuilder onExecute(Runnable action) {
            this.onExecute = action;
            return this;
        }

        /**
         * Set the action for opening the debug view.
         *
         * @param action the action to run
         * @return the builder instance
         */
        public NodeContextMenuCallbacksBuilder onDebugView(Runnable action) {
            this.onDebugView = action;
            return this;
        }

        /**
         * Set the action for duplicating the node.
         *
         * @param action the action to run
         * @return the builder instance
         */
        public NodeContextMenuCallbacksBuilder onDuplicate(Runnable action) {
            this.onDuplicate = action;
            return this;
        }

        /**
         * Set the action for toggling enabled state.
         *
         * @param action the action to run
         * @return the builder instance
         */
        public NodeContextMenuCallbacksBuilder onToggleEnabled(Runnable action) {
            this.onToggleEnabled = action;
            return this;
        }

        /**
         * Set the action for renaming the node.
         *
         * @param action the action to run
         * @return the builder instance
         */
        public NodeContextMenuCallbacksBuilder onRename(Runnable action) {
            this.onRename = action;
            return this;
        }

        /**
         * Set the action for changing the icon.
         *
         * @param action the action to run
         * @return the builder instance
         */
        public NodeContextMenuCallbacksBuilder onChangeIcon(Runnable action) {
            this.onChangeIcon = action;
            return this;
        }

        /**
         * Set the action for deleting connections.
         *
         * @param action the action to run
         * @return the builder instance
         */
        public NodeContextMenuCallbacksBuilder onDeleteConnections(Runnable action) {
            this.onDeleteConnections = action;
            return this;
        }

        /**
         * Set the action for deleting the node.
         *
         * @param action the action to run
         * @return the builder instance
         */
        public NodeContextMenuCallbacksBuilder onDelete(Runnable action) {
            this.onDelete = action;
            return this;
        }

        /**
         * Build the callbacks instance.
         *
         * @return the constructed callbacks
         */
        public NodeContextMenuCallbacks build() {
            return new NodeContextMenuCallbacks() {
                @Override
                public void onOpenEditor() {
                    onOpenEditor.run();
                }

                @Override
                public void onExecute() {
                    onExecute.run();
                }

                @Override
                public void onDebugView() {
                    onDebugView.run();
                }

                @Override
                public void onDuplicate() {
                    onDuplicate.run();
                }

                @Override
                public void onToggleEnabled() {
                    onToggleEnabled.run();
                }

                @Override
                public void onRename() {
                    onRename.run();
                }

                @Override
                public void onChangeIcon() {
                    onChangeIcon.run();
                }

                @Override
                public void onDeleteConnections() {
                    onDeleteConnections.run();
                }

                @Override
                public void onDelete() {
                    onDelete.run();
                }
            };
        }
    }
}
