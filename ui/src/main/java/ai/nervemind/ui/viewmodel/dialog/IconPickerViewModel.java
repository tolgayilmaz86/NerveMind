/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.dialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeBrands;
import org.kordamp.ikonli.fontawesome5.FontAwesomeRegular;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignB;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignD;
import org.kordamp.ikonli.materialdesign2.MaterialDesignE;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignG;
import org.kordamp.ikonli.materialdesign2.MaterialDesignH;
import org.kordamp.ikonli.materialdesign2.MaterialDesignI;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;
import org.kordamp.ikonli.materialdesign2.MaterialDesignM;
import org.kordamp.ikonli.materialdesign2.MaterialDesignN;
import org.kordamp.ikonli.materialdesign2.MaterialDesignO;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignR;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;
import org.kordamp.ikonli.materialdesign2.MaterialDesignT;
import org.kordamp.ikonli.materialdesign2.MaterialDesignU;
import org.kordamp.ikonli.materialdesign2.MaterialDesignV;
import org.kordamp.ikonli.materialdesign2.MaterialDesignW;

import ai.nervemind.common.enums.IconCategory;
import ai.nervemind.ui.viewmodel.BaseViewModel;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * ViewModel for the Icon Picker dialog.
 * 
 * <p>
 * Manages icon listing, filtering, and selection.
 * Does not depend on any JavaFX UI classes - only uses javafx.beans and
 * javafx.collections.
 */
public class IconPickerViewModel extends BaseViewModel {

    /**
     * Icon entry record for the icon list.
     *
     * @param code     The icon identifier code (e.g., "mdi2c-cog")
     * @param ikon     The Ikon enum instance from Ikonli
     * @param category The library/category this icon belongs to
     */
    public record IconEntry(String code, Ikon ikon, IconCategory category) {
    }

    // All available icons
    private static final List<IconEntry> ALL_ICONS = new ArrayList<>();

    static {
        // Material Design - Common workflow icons
        addIcon(MaterialDesignA.ACCOUNT, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignA.ACCOUNT_CIRCLE, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignA.ALERT, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignA.ALERT_CIRCLE, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignA.API, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignA.ARROW_RIGHT, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignA.ATTACHMENT, IconCategory.MATERIAL_DESIGN);

        addIcon(MaterialDesignB.BELL, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignB.BOOK, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignB.BOOK_SEARCH, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignB.BRIGHTNESS_AUTO, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignB.BUG, IconCategory.MATERIAL_DESIGN);

        addIcon(MaterialDesignC.CALENDAR, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignC.CALL_MERGE, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignC.CALL_SPLIT, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignC.CAMERA, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignC.CHART_BAR, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignC.CHART_LINE, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignC.CHAT, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignC.CHECK, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignC.CHECK_CIRCLE, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignC.CLOCK, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignC.CLOCK_OUTLINE, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignC.CLOUD, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignC.CLOUD_DOWNLOAD, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignC.CLOUD_UPLOAD, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignC.CODE_BRACES, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignC.CODE_TAGS, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignC.COG, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignC.CONSOLE, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignC.CONTENT_COPY, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignC.CUBE_OUTLINE, IconCategory.MATERIAL_DESIGN);

        addIcon(MaterialDesignD.DATABASE, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignD.DELETE, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignD.DOWNLOAD, IconCategory.MATERIAL_DESIGN);

        addIcon(MaterialDesignE.EARTH, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignE.EMAIL, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignE.EMAIL_OUTLINE, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignE.EYE, IconCategory.MATERIAL_DESIGN);

        addIcon(MaterialDesignF.FILE, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignF.FILE_DOCUMENT, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignF.FILE_EYE, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignF.FILE_SEND, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignF.FILTER, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignF.FILTER_OUTLINE, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignF.FLASH, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignF.FOLDER, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignF.FORMAT_ALIGN_JUSTIFY, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignF.FUNCTION, IconCategory.MATERIAL_DESIGN);

        addIcon(MaterialDesignG.GIT, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignG.GITHUB, IconCategory.MATERIAL_DESIGN);

        addIcon(MaterialDesignH.HEART, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignH.HOME, IconCategory.MATERIAL_DESIGN);

        addIcon(MaterialDesignI.IMAGE, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignI.INFORMATION, IconCategory.MATERIAL_DESIGN);

        addIcon(MaterialDesignL.LANGUAGE_JAVASCRIPT, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignL.LANGUAGE_PYTHON, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignL.LINK, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignL.LOCK, IconCategory.MATERIAL_DESIGN);

        addIcon(MaterialDesignM.MAGNIFY, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignM.MAP_MARKER, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignM.MESSAGE, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignM.MICROPHONE, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignM.MINUS_CIRCLE, IconCategory.MATERIAL_DESIGN);

        addIcon(MaterialDesignN.NOTE, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignN.NUMERIC, IconCategory.MATERIAL_DESIGN);

        addIcon(MaterialDesignO.OPEN_IN_NEW, IconCategory.MATERIAL_DESIGN);

        addIcon(MaterialDesignP.PENCIL, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignP.PHONE, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignP.PLAY, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignP.PLAY_CIRCLE, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignP.PLUS, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignP.PLUS_CIRCLE, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignP.POWER, IconCategory.MATERIAL_DESIGN);

        addIcon(MaterialDesignR.REFRESH, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignR.REPEAT, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignR.ROBOT, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignR.ROCKET, IconCategory.MATERIAL_DESIGN);

        addIcon(MaterialDesignS.SEND, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignS.SERVER, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignS.SHIELD_CHECK, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignS.SITEMAP, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignS.SLACK, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignS.SORT_ASCENDING, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignS.SPEEDOMETER, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignS.STAR, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignS.STOP, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignS.SWAP_HORIZONTAL, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignS.SYNC, IconCategory.MATERIAL_DESIGN);

        addIcon(MaterialDesignT.TABLE, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignT.TAG, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignT.TAG_TEXT_OUTLINE, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignT.TEXT, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignT.TIMER, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignT.TRANSLATE, IconCategory.MATERIAL_DESIGN);

        addIcon(MaterialDesignU.UPLOAD, IconCategory.MATERIAL_DESIGN);

        addIcon(MaterialDesignV.VECTOR_BEZIER, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignV.VIDEO, IconCategory.MATERIAL_DESIGN);

        addIcon(MaterialDesignW.WEB, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignW.WEBHOOK, IconCategory.MATERIAL_DESIGN);
        addIcon(MaterialDesignW.WRENCH, IconCategory.MATERIAL_DESIGN);

        // FontAwesome Solid
        addIcon(FontAwesomeSolid.BOLT, IconCategory.FONTAWESOME);
        addIcon(FontAwesomeSolid.BRAIN, IconCategory.FONTAWESOME);
        addIcon(FontAwesomeSolid.CHART_PIE, IconCategory.FONTAWESOME);
        addIcon(FontAwesomeSolid.COINS, IconCategory.FONTAWESOME);
        addIcon(FontAwesomeSolid.COMPRESS, IconCategory.FONTAWESOME);
        addIcon(FontAwesomeSolid.EXPAND, IconCategory.FONTAWESOME);
        addIcon(FontAwesomeSolid.FIRE, IconCategory.FONTAWESOME);
        addIcon(FontAwesomeSolid.GLOBE, IconCategory.FONTAWESOME);
        addIcon(FontAwesomeSolid.LAYER_GROUP, IconCategory.FONTAWESOME);
        addIcon(FontAwesomeSolid.MAGIC, IconCategory.FONTAWESOME);
        addIcon(FontAwesomeSolid.NETWORK_WIRED, IconCategory.FONTAWESOME);
        addIcon(FontAwesomeSolid.PROJECT_DIAGRAM, IconCategory.FONTAWESOME);
        addIcon(FontAwesomeSolid.RANDOM, IconCategory.FONTAWESOME);
        addIcon(FontAwesomeSolid.ROBOT, IconCategory.FONTAWESOME);
        addIcon(FontAwesomeSolid.SCROLL, IconCategory.FONTAWESOME);
        addIcon(FontAwesomeSolid.TASKS, IconCategory.FONTAWESOME);
        addIcon(FontAwesomeSolid.TERMINAL, IconCategory.FONTAWESOME);
        addIcon(FontAwesomeSolid.THUMBS_UP, IconCategory.FONTAWESOME);
        addIcon(FontAwesomeSolid.TOOLS, IconCategory.FONTAWESOME);
        addIcon(FontAwesomeSolid.TROPHY, IconCategory.FONTAWESOME);

        // FontAwesome Regular
        addIcon(FontAwesomeRegular.BELL, IconCategory.FONTAWESOME);
        addIcon(FontAwesomeRegular.CALENDAR, IconCategory.FONTAWESOME);
        addIcon(FontAwesomeRegular.COMMENT, IconCategory.FONTAWESOME);
        addIcon(FontAwesomeRegular.ENVELOPE, IconCategory.FONTAWESOME);
        addIcon(FontAwesomeRegular.FILE, IconCategory.FONTAWESOME);
        addIcon(FontAwesomeRegular.FOLDER, IconCategory.FONTAWESOME);
        addIcon(FontAwesomeRegular.HEART, IconCategory.FONTAWESOME);
        addIcon(FontAwesomeRegular.LIGHTBULB, IconCategory.FONTAWESOME);
        addIcon(FontAwesomeRegular.STAR, IconCategory.FONTAWESOME);

        // FontAwesome Brands
        addIcon(FontAwesomeBrands.AWS, IconCategory.BRANDS);
        addIcon(FontAwesomeBrands.DISCORD, IconCategory.BRANDS);
        addIcon(FontAwesomeBrands.DOCKER, IconCategory.BRANDS);
        addIcon(FontAwesomeBrands.DROPBOX, IconCategory.BRANDS);
        addIcon(FontAwesomeBrands.GITHUB, IconCategory.BRANDS);
        addIcon(FontAwesomeBrands.GOOGLE, IconCategory.BRANDS);
        addIcon(FontAwesomeBrands.GOOGLE_DRIVE, IconCategory.BRANDS);
        addIcon(FontAwesomeBrands.LINKEDIN, IconCategory.BRANDS);
        addIcon(FontAwesomeBrands.MICROSOFT, IconCategory.BRANDS);
        addIcon(FontAwesomeBrands.PYTHON, IconCategory.BRANDS);
        addIcon(FontAwesomeBrands.SLACK, IconCategory.BRANDS);
        addIcon(FontAwesomeBrands.TWITTER, IconCategory.BRANDS);
    }

    private static void addIcon(Ikon ikon, IconCategory category) {
        ALL_ICONS.add(new IconEntry(ikon.getDescription(), ikon, category));
    }

    /** Constant for "All" category filter option. */
    public static final String ALL_CATEGORIES = "All";

    // Observable data
    private final ObservableList<IconEntry> filteredIcons = FXCollections.observableArrayList();
    private final ObservableList<String> categories = FXCollections.observableArrayList();

    // Filter state
    private final StringProperty searchQuery = new SimpleStringProperty("");
    private final StringProperty selectedCategory = new SimpleStringProperty(ALL_CATEGORIES);

    // Selection state
    private final ObjectProperty<IconEntry> selectedIcon = new SimpleObjectProperty<>();
    private final StringProperty selectedIconCode = new SimpleStringProperty();
    private final StringProperty selectedIconLabel = new SimpleStringProperty("No icon selected");

    // Original icon
    private final String originalIcon;

    /**
     * Creates a new IconPickerViewModel.
     * 
     * @param currentIcon the currently selected icon code, or null
     */
    public IconPickerViewModel(String currentIcon) {
        this.originalIcon = currentIcon;

        // Setup categories from enum
        categories.add(ALL_CATEGORIES);
        Arrays.stream(IconCategory.values())
                .map(IconCategory::getDisplayName)
                .forEach(categories::add);

        // Setup filter listeners
        searchQuery.addListener((obs, oldVal, newVal) -> applyFilters());
        selectedCategory.addListener((obs, oldVal, newVal) -> applyFilters());

        // Setup selection listener
        selectedIcon.addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedIconCode.set(newVal.code());
                selectedIconLabel.set(newVal.code());
            } else {
                selectedIconCode.set(null);
                selectedIconLabel.set("No icon selected");
            }
        });

        // Pre-select current icon if any
        if (currentIcon != null) {
            selectedIconCode.set(currentIcon);
            selectedIconLabel.set(currentIcon);
            // Find and select the icon entry
            for (IconEntry entry : ALL_ICONS) {
                if (entry.code().equals(currentIcon)) {
                    selectedIcon.set(entry);
                    break;
                }
            }
        }

        // Initial filter
        applyFilters();
    }

    // ===== Properties =====

    /**
     * Filtered icons list.
     *
     * @return the filtered icons list
     */
    public ObservableList<IconEntry> getFilteredIcons() {
        return filteredIcons;
    }

    /**
     * Available categories.
     *
     * @return the categories list
     */
    public ObservableList<String> getCategories() {
        return categories;
    }

    /**
     * Search query text.
     *
     * @return the search query property
     */
    public StringProperty searchQueryProperty() {
        return searchQuery;
    }

    /**
     * Selected category filter.
     *
     * @return the selected category property
     */
    public StringProperty selectedCategoryProperty() {
        return selectedCategory;
    }

    /**
     * Currently selected icon entry.
     *
     * @return the selected icon property
     */
    public ObjectProperty<IconEntry> selectedIconProperty() {
        return selectedIcon;
    }

    /**
     * Selected icon code.
     *
     * @return the selected icon code property
     */
    public StringProperty selectedIconCodeProperty() {
        return selectedIconCode;
    }

    /**
     * Selected icon label for display.
     *
     * @return the selected icon label property
     */
    public ReadOnlyStringProperty selectedIconLabelProperty() {
        return selectedIconLabel;
    }

    /**
     * Get the original icon code.
     *
     * @return the original icon
     */
    public String getOriginalIcon() {
        return originalIcon;
    }

    // ===== Actions =====

    /**
     * Select an icon.
     *
     * @param entry the icon entry to select
     */
    public void selectIcon(IconEntry entry) {
        selectedIcon.set(entry);
        markDirty();
    }

    /**
     * Clear selection (reset to default).
     */
    public void resetSelection() {
        selectedIcon.set(null);
        selectedIconCode.set(null);
        selectedIconLabel.set("Using default icon");
        markDirty();
    }

    /**
     * Get the result - selected icon code or null for reset.
     *
     * @return the selected icon code or null
     */
    public String getResult() {
        return selectedIconCode.get();
    }

    // ===== Private Methods =====

    private void applyFilters() {
        String search = searchQuery.get() != null ? searchQuery.get().toLowerCase().trim() : "";
        String categoryFilter = selectedCategory.get();
        IconCategory selectedIconCategory = IconCategory.fromDisplayName(categoryFilter);

        List<IconEntry> filtered = ALL_ICONS.stream()
                .filter(entry -> {
                    // Category filter - null means "All"
                    boolean categoryMatch = selectedIconCategory == null
                            || entry.category() == selectedIconCategory;
                    // Search filter
                    boolean searchMatch = search.isEmpty() || entry.code().toLowerCase().contains(search);
                    return categoryMatch && searchMatch;
                })
                .toList();

        filteredIcons.setAll(filtered);
    }
}
