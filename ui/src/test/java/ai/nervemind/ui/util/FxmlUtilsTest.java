/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.io.UncheckedIOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import ai.nervemind.ui.util.fixtures.FxRootTestComponent;
import ai.nervemind.ui.util.fixtures.SimpleViewController;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;

@ExtendWith(MockitoExtension.class)
@DisplayName("FxmlUtils")
class FxmlUtilsTest {

    private static final String BASE = "/ai/nervemind/ui/view/";

    @Mock
    private ApplicationContext applicationContext;

    @AfterEach
    void resetBasePath() {
        FxmlUtils.setFxmlBasePath(BASE);
    }

    @Test
    @DisplayName("set/get base path should round-trip")
    void setGetBasePathShouldRoundTrip() {
        FxmlUtils.setFxmlBasePath("/custom/base/");
        assertThat(FxmlUtils.getFxmlBasePath()).isEqualTo("/custom/base/");
    }

    @Test
    @DisplayName("load with ApplicationContext should use controller from context")
    void loadWithApplicationContextShouldUseControllerFromContext() {
        SimpleViewController controller = new SimpleViewController();
        when(applicationContext.getBean(SimpleViewController.class)).thenReturn(controller);

        Parent root = FxmlUtils.load("test/SimpleView.fxml", applicationContext);

        assertThat(root).isInstanceOf(VBox.class);
    }

    @Test
    @DisplayName("loadWithController and factory should return root and controller")
    void loadWithControllerAndFactoryShouldReturnRootAndController() {
        SimpleViewController controller = new SimpleViewController();

        FxmlUtils.LoadResult<VBox, SimpleViewController> result = FxmlUtils
                .loadWithController("test/SimpleView.fxml", clazz -> controller);

        assertThat(result.root()).isInstanceOf(VBox.class);
        assertThat(result.controller()).isSameAs(controller);
    }

    @Test
    @DisplayName("loadWithController and explicit controller should work with no-controller fxml")
    void loadWithControllerAndExplicitControllerShouldWorkWithNoControllerFxml() {
        Object explicitController = new Object();

        VBox root = FxmlUtils.loadWithController("test/NoControllerView.fxml", explicitController);

        assertThat(root).isInstanceOf(VBox.class);
    }

    @Test
    @DisplayName("loadComponent should load fx:root component")
    void loadComponentShouldLoadFxRootComponent() {
        FxRootTestComponent component = new FxRootTestComponent();

        FxmlUtils.loadComponent(component, "test/FxRootView.fxml");

        assertThat(component).isInstanceOf(VBox.class);
    }

    @Test
    @DisplayName("loadWithController result should support withController chaining")
    void loadWithControllerResultShouldSupportWithControllerChaining() {
        SimpleViewController controller = new SimpleViewController();

        final boolean[] called = new boolean[1];
        FxmlUtils.LoadResult<VBox, SimpleViewController> result = FxmlUtils
                .<VBox, SimpleViewController>loadWithController("test/SimpleView.fxml", clazz -> controller)
                .withController(c -> called[0] = "simple".equals(c.marker()));

        assertThat(result.controller()).isSameAs(controller);
        assertThat(called[0]).isTrue();
    }

    @Test
    @DisplayName("load should fail for missing fxml resource")
    void loadShouldFailForMissingFxmlResource() {
        assertThatThrownBy(() -> FxmlUtils.load("missing/Nope.fxml", clazz -> new Object()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FXML resource not found");
    }

    @Test
    @DisplayName("getFxmlPathForController should follow naming convention")
    void getFxmlPathForControllerShouldFollowNamingConvention() {
        String path = FxmlUtils.getFxmlPathForController(
                ai.nervemind.ui.view.dialog.SettingsDialogController.class);

        assertThat(path).isEqualTo("dialog/SettingsDialog.fxml");
    }

    @Test
    @DisplayName("absolute fxml path should bypass configured base path")
    void absoluteFxmlPathShouldBypassConfiguredBasePath() {
        FxmlUtils.setFxmlBasePath("/this/path/does/not/matter/");

        VBox root = FxmlUtils.load("/ai/nervemind/ui/view/test/NoControllerView.fxml", clazz -> new Object());

        assertThat(root).isInstanceOf(VBox.class);
    }

    @Test
    @DisplayName("loadWithController should wrap io errors from malformed fxml")
    void loadWithControllerShouldWrapIoErrorsFromMalformedFxml() {
        // Reuse base path but point to malformed XML syntax resource.
        FxmlUtils.setFxmlBasePath("/ai/nervemind/ui/view/");

        assertThatThrownBy(() -> FxmlUtils.load("test/Malformed.fxml", clazz -> new Object()))
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("Failed to load FXML");
    }
}
