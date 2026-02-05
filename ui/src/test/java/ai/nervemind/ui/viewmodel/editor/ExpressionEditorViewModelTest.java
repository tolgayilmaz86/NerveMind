/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.editor;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ai.nervemind.ui.viewmodel.ViewModelTestBase;

/**
 * Unit tests for {@link ExpressionEditorViewModel}.
 * Tests all ViewModel logic without requiring JavaFX runtime.
 */
class ExpressionEditorViewModelTest extends ViewModelTestBase {

    private ExpressionEditorViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new ExpressionEditorViewModel();
    }

    @Nested
    @DisplayName("Initialization Tests")
    class InitializationTests {

        @Test
        @DisplayName("Should initialize with empty expression")
        void shouldInitializeWithEmptyExpression() {
            assertEquals("", viewModel.getExpression());
        }

        @Test
        @DisplayName("Should be valid initially")
        void shouldBeValidInitially() {
            assertTrue(viewModel.isValid());
        }

        @Test
        @DisplayName("Should not show validation initially")
        void shouldNotShowValidationInitially() {
            assertFalse(viewModel.isShowValidation());
        }

        @Test
        @DisplayName("Should have empty variables list initially")
        void shouldHaveEmptyVariablesListInitially() {
            assertTrue(viewModel.getAvailableVariables().isEmpty());
        }

        @Test
        @DisplayName("Should have suggestions populated with functions")
        void shouldHaveSuggestionsPopulatedWithFunctions() {
            assertFalse(viewModel.getAllSuggestions().isEmpty());
            assertTrue(viewModel.getAllSuggestions().stream()
                    .anyMatch(s -> s.contains("if(")));
        }

        @Test
        @DisplayName("Should have default prompt text")
        void shouldHaveDefaultPromptText() {
            assertNotNull(viewModel.getPromptText());
            assertTrue(viewModel.getPromptText().contains("Ctrl+Space"));
        }

        @Test
        @DisplayName("Should have default row count")
        void shouldHaveDefaultRowCount() {
            assertEquals(3, viewModel.getPrefRowCount());
        }
    }

    @Nested
    @DisplayName("Expression Binding Tests")
    class ExpressionBindingTests {

        @Test
        @DisplayName("Should update expression via property")
        void shouldUpdateExpressionViaProperty() {
            viewModel.setExpression("Hello ${name}");
            assertEquals("Hello ${name}", viewModel.getExpression());
        }

        @Test
        @DisplayName("Should handle null expression")
        void shouldHandleNullExpression() {
            viewModel.setExpression(null);
            assertEquals("", viewModel.getExpression());
        }

        @Test
        @DisplayName("Expression property should be bindable")
        void expressionPropertyShouldBeBindable() {
            var tracker = new PropertyChangeTracker<String>();
            viewModel.expressionProperty().addListener(tracker::onChanged);

            viewModel.setExpression("Test");

            assertEquals(1, tracker.getChangeCount());
            assertEquals("Test", tracker.getLastNewValue());
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Empty expression should be valid")
        void emptyExpressionShouldBeValid() {
            viewModel.setExpression("");
            assertTrue(viewModel.isValid());
        }

        @Test
        @DisplayName("Blank expression should be valid")
        void blankExpressionShouldBeValid() {
            viewModel.setExpression("   ");
            assertTrue(viewModel.isValid());
        }

        @Test
        @DisplayName("Simple text should be valid")
        void simpleTextShouldBeValid() {
            viewModel.setExpression("Hello World");
            assertTrue(viewModel.isValid());
        }

        @Test
        @DisplayName("Valid variable reference should show success")
        void validVariableReferenceShouldShowSuccess() {
            viewModel.setAvailableVariables(List.of("name", "age"));
            viewModel.setExpression("Hello ${name}");

            assertTrue(viewModel.isValid());
            assertTrue(viewModel.isShowValidation());
            assertTrue(viewModel.getValidationMessage().contains("✓"));
        }

        @Test
        @DisplayName("Valid function call should be valid")
        void validFunctionCallShouldBeValid() {
            viewModel.setExpression("concat(a, b)");
            assertTrue(viewModel.isValid());
        }

        @Nested
        @DisplayName("Unclosed Variable Reference")
        class UnclosedVariableReferenceTests {

            @Test
            @DisplayName("Should detect unclosed variable reference")
            void shouldDetectUnclosedVariableReference() {
                viewModel.setExpression("Hello ${name");

                assertFalse(viewModel.isValid());
                assertTrue(viewModel.getValidationMessage().contains("missing '}'"));
            }

            @Test
            @DisplayName("Should detect multiple unclosed references")
            void shouldDetectMultipleUnclosedReferences() {
                viewModel.setExpression("${a ${b");

                assertFalse(viewModel.isValid());
            }

            @Test
            @DisplayName("Nested braces should work correctly")
            void nestedBracesShouldWorkCorrectly() {
                viewModel.setAvailableVariables(List.of("obj"));
                viewModel.setExpression("${obj}");

                assertTrue(viewModel.isValid());
            }
        }

        @Nested
        @DisplayName("Balanced Parentheses")
        class BalancedParenthesesTests {

            @Test
            @DisplayName("Should detect missing closing parenthesis")
            void shouldDetectMissingClosingParenthesis() {
                viewModel.setExpression("concat(a, b");

                assertFalse(viewModel.isValid());
                assertTrue(viewModel.getValidationMessage().contains("missing ')'"));
            }

            @Test
            @DisplayName("Should detect extra closing parenthesis")
            void shouldDetectExtraClosingParenthesis() {
                viewModel.setExpression("concat(a, b))");

                assertFalse(viewModel.isValid());
                assertTrue(viewModel.getValidationMessage().contains("extra ')'"));
            }

            @Test
            @DisplayName("Balanced parentheses should be valid")
            void balancedParenthesesShouldBeValid() {
                viewModel.setExpression("concat(upper(a), lower(b))");
                assertTrue(viewModel.isValid());
            }
        }

        @Nested
        @DisplayName("Empty Variable Reference")
        class EmptyVariableReferenceTests {

            @Test
            @DisplayName("Should detect empty variable reference")
            void shouldDetectEmptyVariableReference() {
                viewModel.setExpression("Hello ${}");

                assertFalse(viewModel.isValid());
                assertTrue(viewModel.getValidationMessage().contains("Empty variable reference"));
            }

            @Test
            @DisplayName("Should detect whitespace-only variable reference")
            void shouldDetectWhitespaceOnlyVariableReference() {
                viewModel.setExpression("Hello ${  }");

                assertFalse(viewModel.isValid());
            }
        }

        @Nested
        @DisplayName("Undefined Variables")
        class UndefinedVariableTests {

            @Test
            @DisplayName("Should detect undefined variable")
            void shouldDetectUndefinedVariable() {
                viewModel.setAvailableVariables(List.of("name"));
                viewModel.setExpression("Hello ${age}");

                assertFalse(viewModel.isValid());
                assertTrue(viewModel.getValidationMessage().contains("Unknown variable: age"));
            }

            @Test
            @DisplayName("Defined variable should be valid")
            void definedVariableShouldBeValid() {
                viewModel.setAvailableVariables(List.of("name", "age"));
                viewModel.setExpression("${name} is ${age}");

                assertTrue(viewModel.isValid());
            }

            @Test
            @DisplayName("Should re-validate when variables change")
            void shouldRevalidateWhenVariablesChange() {
                viewModel.setExpression("Hello ${name}");
                assertFalse(viewModel.isValid()); // name not defined

                viewModel.setAvailableVariables(List.of("name"));
                assertTrue(viewModel.isValid()); // now defined
            }
        }

        @Nested
        @DisplayName("Undefined Functions")
        class UndefinedFunctionTests {

            @Test
            @DisplayName("Should detect undefined function")
            void shouldDetectUndefinedFunction() {
                viewModel.setExpression("unknownFunc(a, b)");

                assertFalse(viewModel.isValid());
                assertTrue(viewModel.getValidationMessage().contains("Unknown function: unknownFunc"));
            }

            @Test
            @DisplayName("Built-in functions should be valid")
            void builtInFunctionsShouldBeValid() {
                viewModel.setExpression("if(eq(a, b), upper(c), lower(d))");
                assertTrue(viewModel.isValid());
            }

            @Test
            @DisplayName("All built-in functions should be recognized")
            void allBuiltInFunctionsShouldBeRecognized() {
                List<String> functions = List.of(
                        "if", "and", "or", "not", "eq", "neq", "gt", "lt", "gte", "lte",
                        "concat", "substring", "length", "upper", "lower", "trim", "replace",
                        "split", "join", "contains", "startsWith", "endsWith",
                        "now", "format", "parse", "round", "abs", "random");

                for (String func : functions) {
                    viewModel.setExpression(func + "()");
                    assertTrue(viewModel.isValid(),
                            "Function '" + func + "' should be recognized");
                }
            }
        }
    }

    @Nested
    @DisplayName("Variable Management Tests")
    class VariableManagementTests {

        @Test
        @DisplayName("Should set available variables")
        void shouldSetAvailableVariables() {
            viewModel.setAvailableVariables(List.of("var1", "var2", "var3"));

            assertEquals(3, viewModel.getAvailableVariables().size());
            assertTrue(viewModel.getAvailableVariables().contains("var1"));
        }

        @Test
        @DisplayName("Should handle null variables list")
        void shouldHandleNullVariablesList() {
            viewModel.setAvailableVariables(List.of("a", "b"));
            viewModel.setAvailableVariables(null);

            assertTrue(viewModel.getAvailableVariables().isEmpty());
        }

        @Test
        @DisplayName("Should update suggestions when variables change")
        void shouldUpdateSuggestionsWhenVariablesChange() {
            int initialSize = viewModel.getAllSuggestions().size();

            viewModel.setAvailableVariables(List.of("name", "age"));

            assertEquals(initialSize + 2, viewModel.getAllSuggestions().size());
            assertTrue(viewModel.getAllSuggestions().stream()
                    .anyMatch(s -> s.equals("${name}")));
        }

        @Test
        @DisplayName("Should report has available variables")
        void shouldReportHasAvailableVariables() {
            assertFalse(viewModel.hasAvailableVariables());

            viewModel.setAvailableVariables(List.of("test"));

            assertTrue(viewModel.hasAvailableVariables());
        }
    }

    @Nested
    @DisplayName("Suggestion Tests")
    class SuggestionTests {

        @BeforeEach
        void setUpSuggestions() {
            viewModel.setAvailableVariables(List.of("username", "password", "email"));
        }

        @Test
        @DisplayName("Should show suggestions")
        void shouldShowSuggestions() {
            assertFalse(viewModel.isSuggestionsVisible());

            viewModel.showSuggestions();

            assertTrue(viewModel.isSuggestionsVisible());
        }

        @Test
        @DisplayName("Should hide suggestions")
        void shouldHideSuggestions() {
            viewModel.showSuggestions();
            viewModel.hideSuggestions();

            assertFalse(viewModel.isSuggestionsVisible());
        }

        @Test
        @DisplayName("Should filter suggestions")
        void shouldFilterSuggestions() {
            int totalSize = viewModel.getAllSuggestions().size();

            viewModel.setSuggestionFilter("user");

            assertTrue(viewModel.getFilteredSuggestions().size() < totalSize);
            assertTrue(viewModel.getFilteredSuggestions().stream()
                    .allMatch(s -> s.toLowerCase().contains("user")));
        }

        @Test
        @DisplayName("Empty filter should show all suggestions")
        void emptyFilterShouldShowAllSuggestions() {
            viewModel.setSuggestionFilter("test");
            viewModel.setSuggestionFilter("");

            assertEquals(viewModel.getAllSuggestions().size(),
                    viewModel.getFilteredSuggestions().size());
        }

        @Test
        @DisplayName("Should filter case-insensitively")
        void shouldFilterCaseInsensitively() {
            viewModel.setSuggestionFilter("USER");

            assertTrue(viewModel.getFilteredSuggestions().stream()
                    .anyMatch(s -> s.contains("username")));
        }
    }

    @Nested
    @DisplayName("Insertion Tests")
    class InsertionTests {

        @BeforeEach
        void setUpInsertion() {
            viewModel.setExpression("Hello ");
            viewModel.setCaretPosition(6);
            viewModel.setAvailableVariables(List.of("name"));
        }

        @Test
        @DisplayName("Should insert variable at caret position")
        void shouldInsertVariableAtCaretPosition() {
            String result = viewModel.insertVariable("name");

            assertEquals("Hello ${name}", result);
        }

        @Test
        @DisplayName("Should insert variable in middle of text")
        void shouldInsertVariableInMiddleOfText() {
            viewModel.setExpression("Hello World");
            viewModel.setCaretPosition(6);

            String result = viewModel.insertVariable("name");

            assertEquals("Hello ${name}World", result);
        }

        @Test
        @DisplayName("Should insert function at caret position")
        void shouldInsertFunctionAtCaretPosition() {
            String result = viewModel.insertFunction("upper(text)");

            assertEquals("Hello upper(text)", result);
        }

        @Test
        @DisplayName("Should insert selected suggestion")
        void shouldInsertSelectedSuggestion() {
            viewModel.setSelectedSuggestion("${name}");

            String result = viewModel.insertSelectedSuggestion();

            assertEquals("Hello ${name}", result);
            assertFalse(viewModel.isSuggestionsVisible());
        }

        @Test
        @DisplayName("Should return null when no suggestion selected")
        void shouldReturnNullWhenNoSuggestionSelected() {
            viewModel.setSelectedSuggestion(null);

            String result = viewModel.insertSelectedSuggestion();

            assertNull(result);
        }
    }

    @Nested
    @DisplayName("Display Property Tests")
    class DisplayPropertyTests {

        @Test
        @DisplayName("Should update prompt text")
        void shouldUpdatePromptText() {
            viewModel.setPromptText("Custom prompt");

            assertEquals("Custom prompt", viewModel.getPromptText());
        }

        @Test
        @DisplayName("Should update pref row count")
        void shouldUpdatePrefRowCount() {
            viewModel.setPrefRowCount(5);

            assertEquals(5, viewModel.getPrefRowCount());
        }
    }

    @Nested
    @DisplayName("Available Functions Tests")
    class AvailableFunctionsTests {

        @Test
        @DisplayName("Should return available functions list")
        void shouldReturnAvailableFunctionsList() {
            List<String> functions = viewModel.getAvailableFunctions();

            assertNotNull(functions);
            assertFalse(functions.isEmpty());
        }

        @Test
        @DisplayName("Functions should include signatures")
        void functionsShouldIncludeSignatures() {
            List<String> functions = viewModel.getAvailableFunctions();

            assertTrue(functions.stream()
                    .anyMatch(f -> f.equals("if(condition, then, else)")));
            assertTrue(functions.stream()
                    .anyMatch(f -> f.equals("concat(a, b, ...)")));
        }
    }

    @Nested
    @DisplayName("Help Text Tests")
    class HelpTextTests {

        @Test
        @DisplayName("Should return help text")
        void shouldReturnHelpText() {
            String help = viewModel.getHelpText();

            assertNotNull(help);
            assertTrue(help.contains("Variables"));
            assertTrue(help.contains("Functions"));
            assertTrue(help.contains("Ctrl+Space"));
        }
    }

    @Nested
    @DisplayName("Caret Position Tests")
    class CaretPositionTests {

        @Test
        @DisplayName("Should track caret position")
        void shouldTrackCaretPosition() {
            viewModel.setCaretPosition(10);

            assertEquals(10, viewModel.getCaretPosition());
        }

        @Test
        @DisplayName("Caret position property should be bindable")
        void caretPositionPropertyShouldBeBindable() {
            var tracker = new PropertyChangeTracker<Number>();
            viewModel.caretPositionProperty().addListener(tracker::onChanged);

            viewModel.setCaretPosition(5);

            assertEquals(1, tracker.getChangeCount());
            assertEquals(5, tracker.getLastNewValue().intValue());
        }
    }

    @Nested
    @DisplayName("Validation Method Tests")
    class ValidationMethodTests {

        @Test
        @DisplayName("checkUnclosedVariableReferences should work correctly")
        void checkUnclosedVariableReferencesShouldWorkCorrectly() {
            assertNull(viewModel.checkUnclosedVariableReferences("${valid}"));
            assertNotNull(viewModel.checkUnclosedVariableReferences("${invalid"));
            assertNull(viewModel.checkUnclosedVariableReferences("no variables"));
        }

        @Test
        @DisplayName("checkBalancedParentheses should work correctly")
        void checkBalancedParenthesesShouldWorkCorrectly() {
            assertNull(viewModel.checkBalancedParentheses("func()"));
            assertNull(viewModel.checkBalancedParentheses("func(a, func2(b))"));
            assertNotNull(viewModel.checkBalancedParentheses("func("));
            assertNotNull(viewModel.checkBalancedParentheses("func)"));
        }

        @Test
        @DisplayName("checkEmptyVariableReferences should work correctly")
        void checkEmptyVariableReferencesShouldWorkCorrectly() {
            assertNull(viewModel.checkEmptyVariableReferences("${valid}"));
            assertNotNull(viewModel.checkEmptyVariableReferences("${}"));
            assertNotNull(viewModel.checkEmptyVariableReferences("${  }"));
        }

        @Test
        @DisplayName("checkUndefinedVariables should work correctly")
        void checkUndefinedVariablesShouldWorkCorrectly() {
            viewModel.setAvailableVariables(List.of("defined"));

            assertNull(viewModel.checkUndefinedVariables("${defined}"));
            assertNotNull(viewModel.checkUndefinedVariables("${undefined}"));
        }

        @Test
        @DisplayName("checkUndefinedFunctions should work correctly")
        void checkUndefinedFunctionsShouldWorkCorrectly() {
            assertNull(viewModel.checkUndefinedFunctions("concat()"));
            assertNull(viewModel.checkUndefinedFunctions("if()"));
            assertNotNull(viewModel.checkUndefinedFunctions("unknownFunc()"));
        }
    }
}
