/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.dialog;

import ai.nervemind.ui.viewmodel.BaseViewModel;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;

/**
 * ViewModel for the About dialog.
 * 
 * <p>
 * Contains application metadata that is displayed in the dialog.
 * This is a read-only ViewModel since the dialog only displays information.
 */
public class AboutDialogViewModel extends BaseViewModel {

    private static final String APP_NAME = "NerveMind";
    private static final String APP_VERSION = "0.1.0";
    private static final String AUTHOR = "Tolga Yilmaz";
    private static final String LICENSE = "MIT License";
    private static final String PROJECT_URL = "https://github.com/tolgayilmaz86/NerveMind";
    private static final String DESCRIPTION = "Visual Workflow Automation for Everyone";

    private final ReadOnlyStringWrapper appName = new ReadOnlyStringWrapper(APP_NAME);
    private final ReadOnlyStringWrapper appVersion = new ReadOnlyStringWrapper("Version " + APP_VERSION);
    private final ReadOnlyStringWrapper authorText = new ReadOnlyStringWrapper("Created by: " + AUTHOR);
    private final ReadOnlyStringWrapper licenseText = new ReadOnlyStringWrapper("License: " + LICENSE);
    private final ReadOnlyStringWrapper projectUrl = new ReadOnlyStringWrapper(PROJECT_URL);
    private final ReadOnlyStringWrapper appDescription = new ReadOnlyStringWrapper(DESCRIPTION);

    /**
     * Creates a new AboutDialogViewModel.
     */
    public AboutDialogViewModel() {
        // Read-only ViewModel - no initialization needed
    }

    // ===== Property Accessors =====

    /**
     * Gets the application name property.
     * 
     * @return the application name property
     */
    public ReadOnlyStringProperty appNameProperty() {
        return appName.getReadOnlyProperty();
    }

    public String getAppName() {
        return appName.get();
    }

    /**
     * Gets the application version property.
     * 
     * @return the application version property
     */
    public ReadOnlyStringProperty appVersionProperty() {
        return appVersion.getReadOnlyProperty();
    }

    public String getAppVersion() {
        return appVersion.get();
    }

    /**
     * Gets the author text property.
     * 
     * @return the author text property
     */
    public ReadOnlyStringProperty authorTextProperty() {
        return authorText.getReadOnlyProperty();
    }

    public String getAuthorText() {
        return authorText.get();
    }

    /**
     * Gets the license text property.
     * 
     * @return the license text property
     */
    public ReadOnlyStringProperty licenseTextProperty() {
        return licenseText.getReadOnlyProperty();
    }

    public String getLicenseText() {
        return licenseText.get();
    }

    /**
     * Gets the project URL property.
     * 
     * @return the project URL property
     */
    public ReadOnlyStringProperty projectUrlProperty() {
        return projectUrl.getReadOnlyProperty();
    }

    public String getProjectUrl() {
        return projectUrl.get();
    }

    /**
     * Gets the application description property.
     * 
     * @return the application description property
     */
    public ReadOnlyStringProperty descriptionProperty() {
        return appDescription.getReadOnlyProperty();
    }

    public String getDescription() {
        return appDescription.get();
    }
}
