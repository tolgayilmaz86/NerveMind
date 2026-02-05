# NerveMind Testing Infrastructure

> **Comprehensive Guide to Testing Strategy and Framework**  
> **Last Updated:** February 5, 2026

---

## Table of Contents

1. [Overview](#1-overview)
2. [Testing Stack](#2-testing-stack)
3. [Test Coverage Summary](#3-test-coverage-summary)
4. [Unit Testing](#4-unit-testing)
   - [ViewModel Unit Tests](#41-viewmodel-unit-tests)
   - [Service Unit Tests](#42-service-unit-tests)
5. [UI Testing Strategy](#5-ui-testing-strategy)
   - [MVVM Testing Benefits](#51-mvvm-testing-benefits)
   - [Integration Tests with TestFX](#52-integration-tests-with-testfx)
6. [Sample Workflow Integration Tests](#6-sample-workflow-integration-tests)
   - [Prerequisites](#61-prerequisites)
   - [Running Integration Tests](#62-running-integration-tests)
   - [Test Categories](#63-test-categories)
   - [Test Reports](#64-test-reports)
   - [Troubleshooting](#65-troubleshooting)
7. [Architecture Tests](#7-architecture-tests)
8. [Planned Testing Improvements](#8-planned-testing-improvements)

---

## 1. Overview

NerveMind employs a multi-layered testing strategy to ensure reliability:

- **Unit Tests**: Test individual components in isolation
- **Integration Tests**: Validate complete workflow execution
- **Architecture Tests**: Enforce code structure and patterns
- **UI Tests**: Verify JavaFX UI behavior

---

## 2. Testing Stack

| Category | Framework | Version | Purpose |
|----------|-----------|---------|---------|
| **Unit Testing** | JUnit | 6.0.1 | Modern testing framework |
| **Assertions** | AssertJ | 3.27+ | Fluent assertion library |
| **Mocking** | Mockito | 5.x | Mock dependencies |
| **Architecture Tests** | ArchUnit | 1.4.1 | Code structure validation |
| **UI Testing** | TestFX | 4.0.18 | JavaFX UI testing |
| **Coverage** | JaCoCo | — | Code coverage reporting |

### Why This Testing Stack?

- **JUnit 6** - Modern, powerful testing framework with excellent IDE support
- **AssertJ** - Readable, fluent assertions that improve test clarity
- **Mockito** - Industry-standard mocking for unit tests
- **ArchUnit** - Enforces architectural rules and patterns automatically
- **TestFX** - Specialized testing for JavaFX UI components

```java
// Unit tests with JUnit 6
@Test
void shouldExecuteWorkflow() {
    // Given
    var workflow = createTestWorkflow();
    
    // When
    var result = executionService.execute(workflow);
    
    // Then
    assertThat(result.getStatus()).isEqualTo(COMPLETED);
}

// Architecture tests with ArchUnit
@ArchTest
static final ArchRule services_should_be_package_private =
    classes().that().haveSimpleNameEndingWith("Service")
        .should().bePackagePrivate();
```

---

## 3. Test Coverage Summary

**Current Coverage (February 2026):**

| Module | Coverage | Notes |
|--------|----------|-------|
| **app** | 22% | Services at 40%, Executors need more tests |
| **ui** | 30% | ViewModels at 87-97%, Canvas/Views need JavaFX tests |
| **common** | — | Shared models (low logic) |

**High Coverage Areas (>80%):**

| Package | Coverage | Description |
|---------|----------|-------------|
| `ai.nervemind.ui.viewmodel.console` | 97% | Console output handling |
| `ai.nervemind.ui.canvas.validation` | 99% | Workflow validation |
| `ai.nervemind.ui.canvas.layout` | 98% | Auto-layout engine |
| `ai.nervemind.ui.viewmodel.editor` | 95% | Code editor VM |
| `ai.nervemind.ui.viewmodel.dialog` | 90% | Dialog ViewModels |
| `ai.nervemind.ui.viewmodel.canvas` | 89% | Canvas ViewModel |
| `ai.nervemind.ui.viewmodel` | 87% | Base ViewModel |

### Running Coverage Reports

```powershell
# Generate coverage report
.\gradlew.bat test jacocoTestReport

# View HTML report
start app/build/reports/jacoco/test/html/index.html
```

---

## 4. Unit Testing

### 4.1 ViewModel Unit Tests

The MVVM architecture enables comprehensive testing **without JavaFX runtime**. ViewModels contain pure business logic that can be tested with standard JUnit:

```java
@ExtendWith(MockitoExtension.class)
class SettingsDialogViewModelTest {
    
    @Mock
    private SettingsServiceInterface settingsService;
    
    private SettingsDialogViewModel viewModel;
    
    @BeforeEach
    void setUp() {
        viewModel = new SettingsDialogViewModel(settingsService);
    }
    
    @Test
    void shouldLoadSettingsForCategory() {
        // Given
        when(settingsService.findByCategory(SettingCategory.GENERAL))
            .thenReturn(List.of(testSetting()));
        
        // When
        viewModel.loadCategory(SettingCategory.GENERAL);
        
        // Then
        assertThat(viewModel.getSettings()).hasSize(1);
        assertThat(viewModel.loadingProperty().get()).isFalse();
    }
    
    @Test
    void shouldTrackPendingChanges() {
        // Given
        viewModel.loadCategory(SettingCategory.GENERAL);
        
        // When
        viewModel.markChanged();
        
        // Then
        assertThat(viewModel.hasChangesProperty().get()).isTrue();
    }
}
```

### 4.2 Service Unit Tests

Service tests mock dependencies and verify business logic:

```java
@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {
    
    @Mock
    private WorkflowRepository repository;
    
    @InjectMocks
    private WorkflowService service;
    
    @Test
    void shouldCreateWorkflowWithDefaultValues() {
        // Given
        WorkflowDTO dto = new WorkflowDTO(null, "Test", "Desc", 
            List.of(), List.of(), Map.of(), true, TriggerType.MANUAL, 
            null, null, null, null, 0);
        
        // When
        when(repository.save(any())).thenReturn(createEntity(dto));
        WorkflowDTO result = service.create(dto);
        
        // Then
        assertThat(result.name()).isEqualTo("Test");
        verify(repository).save(any());
    }
}
```

---

## 5. UI Testing Strategy

### 5.1 MVVM Testing Benefits

The MVVM architecture provides significant testing advantages:

| Layer | Testability | Framework |
|-------|-------------|-----------|
| **Model** | Full | JUnit + Mockito |
| **ViewModel** | Full (no JavaFX) | JUnit + Mockito |
| **View** | Requires UI | TestFX |

ViewModels expose Observable properties that can be verified without running JavaFX:

```java
// Testing property changes without UI
@Test
void shouldUpdateStatusProperty() {
    viewModel.setStatus("Processing");
    assertThat(viewModel.statusProperty().get()).isEqualTo("Processing");
}
```

### 5.2 Integration Tests with TestFX

For full UI testing, use TestFX to interact with JavaFX components:

```java
@ExtendWith(ApplicationExtension.class)
class SettingsDialogIT {
    
    @Start
    void start(Stage stage) {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/ai/nervemind/ui/view/dialog/SettingsDialog.fxml"));
        stage.setScene(new Scene(loader.load()));
        stage.show();
    }
    
    @Test
    void shouldDisplayCategoriesAndAllowSelection(FxRobot robot) {
        robot.lookup(".settings-dialog__category-list").queryListView()
            .should(hasItems(greaterThan(0)));
    }
}
```

---

## 6. Sample Workflow Integration Tests

The `SampleWorkflowIntegrationTest` runs all 19 sample workflows from `app/src/main/resources/samples/` against a real database with real API credentials. These tests serve as system-wide smoke tests to ensure the entire workflow execution pipeline works correctly.

### 6.1 Prerequisites

#### Set Up API Credentials

**Option A: Auto-Import from .env (Recommended)**

Create a `.env` file in the project root:

```env
OPENAI_API_KEY=sk-proj-...
GOOGLE_API_KEY=AIza...
OPEN_WEATHER_API_KEY=...
```

**Integration tests automatically import credentials from this file** on first run. Credentials are stored encrypted in the integration test database.

**Option B: Manual Setup via Application**

1. **Start the application** (using `Run Application (bootRun)` task)
2. Navigate to **Settings** → **Credentials**
3. Add the following credentials as needed:

| Credential Name | Required For |
|-----------------|--------------|
| `OPENAI_API_KEY` | OpenAI-based samples (07, 10, 15, 16, 17, 18) |
| `OPEN_WEATHER_API_KEY` | Weather API samples (01) |
| `GOOGLE_API_KEY` | Google Gemini samples (08) |

**Note:** Samples 00, 02, 03, 04, 05, 06, 09, 11, 12, 13, 14 run without any API keys.

### 6.2 Running Integration Tests

#### Option 1: VS Code Task

1. Press `Ctrl+Shift+P`
2. Type "Tasks: Run Task"
3. Select "Run Integration Tests (Samples)"

#### Option 2: Command Line

```powershell
# Windows - Full integration tests
.\gradlew.bat :app:integrationTest --no-daemon

# Linux/Mac
./gradlew :app:integrationTest --no-daemon
```

#### Option 3: Run Specific Test Categories

```powershell
# Credential sanity checks only
.\gradlew.bat :app:integrationTest --no-daemon --tests "*CredentialSanityChecks*"

# Run only no-credential samples (quick validation)
.\gradlew.bat :app:integrationTest --no-daemon --tests "*NoCredentialSamples*"

# Run only OpenAI samples
.\gradlew.bat :app:integrationTest --no-daemon --tests "*OpenAISamples*"

# Run only Weather API samples
.\gradlew.bat :app:integrationTest --no-daemon --tests "*WeatherAPISamples*"

# Run only Gemini samples
.\gradlew.bat :app:integrationTest --no-daemon --tests "*GeminiAPISamples*"
```

### 6.3 Test Categories

Tests are organized into nested test classes by credential requirements:

#### 0. Credential Sanity Checks
Runs first to validate API keys are properly stored and functional:
- List all stored credentials
- Verify required credentials exist
- Test API connectivity for each credential type
- Generate credential readiness report

#### 1. No-Credential Samples
- `00-weather-alert-workflow-no-apikey.json` - Demo version without real API
- `03-data-processing-pipeline.json` - Data transformation
- `05-error-handling-demo.json` - Error handling patterns
- `11-system-health-monitor.json` - System monitoring
- `12-resilient-data-scraper.json` - Web scraping with retries
- `13-python-data-transform.json` - Python code execution
- `14-python-text-analyzer.json` - Python text analysis

#### 2. OpenAI Samples (require `OPENAI_API_KEY`)
- `02-ai-content-generator.json` - AI content generation
- `07-iracing-setup-advisor.json` - Interactive iRacing advisor
- `09-local-knowledge-base-rag.json` - Local RAG system
- `10-support-ticket-router.json` - AI-powered ticket routing
- `15-security-incident-responder.json` - Security incident analysis
- `16-invoice-processing-erp.json` - Invoice processing with AI
- `17-assignment-auto-grader.json` - Automated grading
- `18-research-paper-summarizer.json` - Research paper analysis

#### 3. Weather API Samples (require `OPEN_WEATHER_API_KEY`)
- `01-weather-alert-workflow.json` - Real weather alerts
- `04-multi-api-integration.json` - Multi-API orchestration

#### 4. Gemini Samples (require `GOOGLE_API_KEY`)
- `08-gemini-ai-assistant.json` - Google Gemini integration

#### 5. File Watcher Samples (special handling)
- `06-file-watcher-workflow.json` - File system monitoring (validation only, requires file events)

### 6.4 Test Reports

After running integration tests, reports are available at:

| Report Type | Location |
|-------------|----------|
| **HTML Report** | `app/build/reports/tests/integrationTest/index.html` |
| **JUnit XML** | `app/build/test-results/integrationTest/` |
| **Console Summary** | Printed at test completion |

### 6.5 Troubleshooting

#### Tests Skipped Due to Missing Credentials

Tests are automatically skipped when:

1. **Missing Credentials**: If `OPENAI_API_KEY` is not in the credential store, OpenAI samples are skipped
2. **File Watcher Samples**: These require special setup and are validated structurally only

**Fix:** Ensure credentials are properly stored:
1. Check `.env` file exists in project root
2. Verify credential names match exactly (e.g., `OPENAI_API_KEY` not `OpenAI_Key`)
3. Run credential sanity checks: `.\gradlew.bat :app:integrationTest --tests "*CredentialSanityChecks*"`

#### Tests Failing with API Errors

Some samples may fail due to:
- Invalid/expired API keys
- Rate limiting from API providers
- Network connectivity issues

**Fix:** Run credential sanity checks to verify API connectivity:
```powershell
.\gradlew.bat :app:integrationTest --tests "*CredentialSanityChecks*"
```

#### Tests Timing Out

Some samples may take longer due to:
- AI API response times
- Network latency
- Complex workflow execution

The default timeout is 2 minutes per workflow.

#### Database Issues

Integration tests use a file-based H2 database at `./data/integration-test-db` to persist credentials between test runs.

**Clear database if encountering issues:**
```powershell
Remove-Item -Recurse -Force .\data\integration-test-db*
```

---

## 7. Architecture Tests

ArchUnit tests enforce code structure and patterns automatically:

```java
@AnalyzeClasses(packages = "ai.nervemind")
class ArchitectureTest {
    
    @ArchTest
    static final ArchRule services_should_not_depend_on_ui =
        noClasses().that().resideInAPackage("..service..")
            .should().dependOnClassesThat().resideInAPackage("..ui..");
    
    @ArchTest
    static final ArchRule executors_should_implement_interface =
        classes().that().haveSimpleNameEndingWith("Executor")
            .should().implement(NodeExecutor.class);
}
```

---

## 8. Planned Testing Improvements

### Completed
- ✅ Unit tests for SettingsService (40 tests)
- ✅ Integration tests for workflow execution
- ✅ Test coverage reporting (JaCoCo)
- ✅ Sample workflow integration tests

### Remaining
- [ ] UI tests for critical paths (requires TestFX)
- [ ] Increase executor test coverage (currently 11%)
- [ ] Increase service test coverage (currently 40%)
- [ ] Performance benchmarks for workflow execution
- [ ] Stress tests for concurrent workflow execution

---

## Related Documentation

- [Architecture Guide](ARCHITECTURE.md) - System architecture overview
- [Samples Guide](SAMPLES_GUIDE.md) - Sample workflow documentation
