/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.view.dialog;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ai.nervemind.ui.integration.FxTestBase;
import ai.nervemind.ui.viewmodel.dialog.AboutDialogViewModel;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

@DisplayName("AboutDialogController")
class AboutDialogControllerTest extends FxTestBase {

    @Test
    @DisplayName("initialize should bind labels and set project link text")
    void initializeShouldBindLabelsAndSetProjectLinkText() throws Exception {
        AboutDialogController controller = runOnFxThreadAndWait(() -> {
            AboutDialogController c = new AboutDialogController();
            injectFields(c);
            c.initialize();
            return c;
        });

        runOnFxThread(() -> {
            assertThat(getField(controller, "appNameLabel", Label.class).getText()).isEqualTo("NerveMind");
            assertThat(getField(controller, "versionLabel", Label.class).getText()).contains("Version");
            assertThat(getField(controller, "projectLink", Hyperlink.class).getText()).contains("github.com");
            assertThat(getField(controller, "descriptionLabel", Label.class).getText())
                    .isEqualTo("Visual Workflow Automation for Everyone");
        });
    }

    @Test
    @DisplayName("setViewModel and close should run callback and close window")
    void setViewModelAndCloseShouldRunCallbackAndCloseWindow() throws Exception {
        AboutDialogController controller = runOnFxThreadAndWait(() -> {
            AboutDialogController c = new AboutDialogController();
            injectFields(c);
            c.initialize();
            return c;
        });

        AtomicBoolean callbackCalled = new AtomicBoolean(false);

        runOnFxThread(() -> {
            controller.setViewModel(new AboutDialogViewModel());
            controller.setOnCloseCallback(() -> callbackCalled.set(true));

            Button closeButton = getField(controller, "closeButton", Button.class);
            VBox root = new VBox(closeButton);
            Stage stage = new Stage();
            stage.setScene(new Scene(root));

            invokePrivate(controller, "handleClose");

            assertThat(callbackCalled.get()).isTrue();
        });
    }

    private static void injectFields(AboutDialogController controller) {
        setField(controller, "logoImageView", new ImageView());
        setField(controller, "appNameLabel", new Label());
        setField(controller, "descriptionLabel", new Label());
        setField(controller, "versionLabel", new Label());
        setField(controller, "authorLabel", new Label());
        setField(controller, "licenseLabel", new Label());
        setField(controller, "projectLink", new Hyperlink());
        setField(controller, "closeButton", new Button("Close"));
    }

    private static void invokePrivate(Object target, String methodName) {
        try {
            Method m = target.getClass().getDeclaredMethod(methodName);
            m.setAccessible(true);
            m.invoke(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> T getField(Object target, String fieldName, Class<T> type) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return type.cast(f.get(target));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
