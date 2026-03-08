/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.view.dialog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ai.nervemind.common.domain.SampleWorkflow;
import ai.nervemind.common.domain.SampleWorkflow.Difficulty;
import ai.nervemind.common.domain.SampleWorkflow.Guide;
import ai.nervemind.common.domain.SampleWorkflow.GuideStep;
import ai.nervemind.ui.integration.FxTestBase;
import ai.nervemind.ui.viewmodel.dialog.SamplesBrowserViewModel;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

@ExtendWith(MockitoExtension.class)
@DisplayName("SamplesBrowserController")
class SamplesBrowserControllerTest extends FxTestBase {

    @Mock
    private Stage stage;

    @Test
    @DisplayName("initialize should bind filters and import selected sample")
    void initializeShouldBindFiltersAndImportSelectedSample() throws Exception {
        SamplesBrowserController controller = runOnFxThreadAndWait(() -> {
            SamplesBrowserController c = new SamplesBrowserController();
            injectFields(c);
            return c;
        });

        SampleWorkflow sample = sample("s1", "Hello", Guide.empty());
        SamplesBrowserViewModel vm = new SamplesBrowserViewModel(
                List.of(sample),
                List.of("General"),
                List.of("javascript"));

        runOnFxThread(() -> controller.initialize(vm, stage));
        runOnFxThread(() -> vm.selectedSampleProperty().set(sample));
        runOnFxThread(() -> invokePrivate(controller, "onImport"));

        assertThat(controller.getResult()).isEqualTo(sample);
        assertThat(controller.getViewModel()).isSameAs(vm);
        verify(stage).close();
    }

    @Test
    @DisplayName("view guide should close dialog and trigger guide callback")
    void viewGuideShouldCloseDialogAndTriggerGuideCallback() throws Exception {
        SamplesBrowserController controller = runOnFxThreadAndWait(() -> {
            SamplesBrowserController c = new SamplesBrowserController();
            injectFields(c);
            return c;
        });

        SampleWorkflow sample = sample("s2", "Guide Sample",
                new Guide(List.of(new GuideStep("Step 1", "Do this"))));

        SamplesBrowserViewModel vm = new SamplesBrowserViewModel(
                List.of(sample),
                List.of("General"),
                List.of("javascript"));

        AtomicReference<SampleWorkflow> guideTriggered = new AtomicReference<>();
        vm.setOnViewGuide(guideTriggered::set);

        runOnFxThread(() -> controller.initialize(vm, stage));
        runOnFxThread(() -> vm.selectedSampleProperty().set(sample));
        runOnFxThread(() -> invokePrivate(controller, "onViewGuide"));

        assertThat(guideTriggered.get()).isEqualTo(sample);
        verify(stage).close();
    }

    @Test
    @DisplayName("close should clear result and close stage")
    void closeShouldClearResultAndCloseStage() throws Exception {
        SamplesBrowserController controller = runOnFxThreadAndWait(() -> {
            SamplesBrowserController c = new SamplesBrowserController();
            injectFields(c);
            setField(c, "result", sample("s3", "Temp", Guide.empty()));
            setField(c, "dialogStage", stage);
            return c;
        });

        runOnFxThread(() -> invokePrivate(controller, "onClose"));

        assertThat(controller.getResult()).isNull();
        verify(stage).close();
    }

    private static SampleWorkflow sample(String id, String name, Guide guide) {
        return new SampleWorkflow(
                id,
                name,
                "desc",
                "General",
                Difficulty.BEGINNER,
                "javascript",
                List.of("tag"),
                "author",
                "1.0",
                guide,
                null,
                List.of(),
                List.of(),
                "sample.json");
    }

    private static void injectFields(SamplesBrowserController controller) {
        setField(controller, "searchField", new TextField());
        setField(controller, "categoryFilter", new ComboBox<String>());
        setField(controller, "languageFilter", new ComboBox<String>());
        setField(controller, "difficultyFilter", new ComboBox<String>());
        setField(controller, "sampleListView", new ListView<SampleWorkflow>());
        setField(controller, "statusLabel", new Label());
        setField(controller, "viewGuideButton", new Button());
        setField(controller, "importButton", new Button());
        setField(controller, "closeButton", new Button());
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
