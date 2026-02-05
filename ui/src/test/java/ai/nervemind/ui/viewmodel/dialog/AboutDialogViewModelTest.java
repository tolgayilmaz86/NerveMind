/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.dialog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ai.nervemind.ui.viewmodel.ViewModelTestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AboutDialogViewModel.
 * 
 * <p>
 * Verifies that the ViewModel exposes the correct application metadata.
 * This is a simple read-only ViewModel test.
 */
class AboutDialogViewModelTest extends ViewModelTestBase {

    private AboutDialogViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new AboutDialogViewModel();
    }

    @Test
    void testAppName() {
        assertEquals("NerveMind", viewModel.getAppName());
        assertNotNull(viewModel.appNameProperty());
        assertEquals("NerveMind", viewModel.appNameProperty().get());
    }

    @Test
    void testAppVersion() {
        assertNotNull(viewModel.getAppVersion());
        assertTrue(viewModel.getAppVersion().startsWith("Version "));
        assertNotNull(viewModel.appVersionProperty());
    }

    @Test
    void testAuthor() {
        assertNotNull(viewModel.getAuthorText());
        assertTrue(viewModel.getAuthorText().contains("Tolga Yilmaz"));
        assertNotNull(viewModel.authorTextProperty());
    }

    @Test
    void testLicense() {
        assertNotNull(viewModel.getLicenseText());
        assertTrue(viewModel.getLicenseText().contains("MIT"));
        assertNotNull(viewModel.licenseTextProperty());
    }

    @Test
    void testProjectUrl() {
        assertNotNull(viewModel.getProjectUrl());
        assertTrue(viewModel.getProjectUrl().contains("github.com"));
        assertNotNull(viewModel.projectUrlProperty());
    }

    @Test
    void testDescription() {
        assertNotNull(viewModel.getDescription());
        assertFalse(viewModel.getDescription().isEmpty());
        assertNotNull(viewModel.descriptionProperty());
    }

    @Test
    void testPropertiesAreReadOnly() {
        // Verify all properties are read-only (no setters available)
        // This is verified by the fact that the properties are ReadOnlyStringProperty
        assertTrue(viewModel.appNameProperty() instanceof javafx.beans.property.ReadOnlyStringProperty);
        assertTrue(viewModel.appVersionProperty() instanceof javafx.beans.property.ReadOnlyStringProperty);
        assertTrue(viewModel.authorTextProperty() instanceof javafx.beans.property.ReadOnlyStringProperty);
        assertTrue(viewModel.licenseTextProperty() instanceof javafx.beans.property.ReadOnlyStringProperty);
        assertTrue(viewModel.projectUrlProperty() instanceof javafx.beans.property.ReadOnlyStringProperty);
        assertTrue(viewModel.descriptionProperty() instanceof javafx.beans.property.ReadOnlyStringProperty);
    }
}
