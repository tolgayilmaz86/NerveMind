package ai.nervemind.app.executor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ai.nervemind.app.service.ExecutionLogger;
import ai.nervemind.app.service.ExecutionService;
import ai.nervemind.common.domain.Node;

/**
 * Unit tests for MergeExecutor.
 * 
 * <p>
 * Tests merge node functionality including:
 * - Different merge modes (waitAll, waitAny, append, merge, passThrough)
 * - Thread synchronization with multiple inputs
 * - Primary thread selection in passThrough mode
 * - Timeout handling
 * </p>
 */
@DisplayName("MergeExecutor")
class MergeExecutorTest {

    private MergeExecutor mergeExecutor;

    @BeforeEach
    void setUp() {
        mergeExecutor = new MergeExecutor();
    }

    private Node createMergeNode(String mode, int inputCount) {
        Map<String, Object> params = new HashMap<>();
        params.put("mode", mode);
        params.put("inputCount", inputCount);
        params.put("timeout", 10); // 10 second timeout for tests
        params.put("outputKey", "merged");

        return new Node(
                "merge-1",
                "merge",
                "Merge Test",
                new Node.Position(100.0, 100.0),
                params,
                null,
                false,
                null);
    }

    private Node createExclusiveMergeNode(String mode, int inputCount) {
        Map<String, Object> params = new HashMap<>();
        params.put("mode", mode);
        params.put("inputCount", inputCount);
        params.put("timeout", 10);
        params.put("outputKey", "merged");
        params.put("waitForAll", false); // Exclusive/conditional branches

        return new Node(
                "merge-exclusive-1",
                "merge",
                "Exclusive Merge Test",
                new Node.Position(100.0, 100.0),
                params,
                null,
                false,
                null);
    }

    private ExecutionService.ExecutionContext createContext(long executionId) {
        return new ExecutionService.ExecutionContext(
                executionId,
                null,
                Map.of(),
                null,
                new ExecutionLogger());
    }

    @Nested
    @DisplayName("Node Type")
    class NodeType {

        @Test
        @DisplayName("should return 'merge' as node type")
        void shouldReturnMergeNodeType() {
            assertThat(mergeExecutor.getNodeType()).isEqualTo("merge");
        }
    }

    @Nested
    @DisplayName("WaitAny Mode")
    class WaitAnyMode {

        @Test
        @DisplayName("should immediately return first input in waitAny mode")
        void shouldReturnFirstInput() {
            Node node = createMergeNode("waitAny", 3);
            ExecutionService.ExecutionContext context = createContext(1L);

            Map<String, Object> input = Map.of("value", "first");
            Map<String, Object> result = mergeExecutor.execute(node, input, context);

            assertThat(result).containsKey("merged");
            assertThat(result.get("_mergeMode")).isEqualTo("waitAny");
            assertThat(result.get("_inputsReceived")).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("PassThrough Mode")
    class PassThroughMode {

        @Test
        @DisplayName("should merge inputs directly to output without nesting")
        void shouldMergeDirectlyToOutput() throws Exception {
            Node node = createMergeNode("passThrough", 2);
            // Use unique execution ID for test isolation
            long executionId = System.nanoTime();
            ExecutionService.ExecutionContext context = createContext(executionId);

            CountDownLatch allDone = new CountDownLatch(2);
            AtomicInteger primaryCount = new AtomicInteger(0);
            AtomicInteger stopCount = new AtomicInteger(0);

            try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
                executor.submit(() -> {
                    Map<String, Object> input1 = Map.of("branch1", "value1");
                    Map<String, Object> result = mergeExecutor.execute(node, input1, context);
                    if (Boolean.TRUE.equals(result.get("_stopExecution"))) {
                        stopCount.incrementAndGet();
                    } else {
                        primaryCount.incrementAndGet();
                    }
                    allDone.countDown();
                });

                executor.submit(() -> {
                    Map<String, Object> input2 = Map.of("branch2", "value2");
                    Map<String, Object> result = mergeExecutor.execute(node, input2, context);
                    if (Boolean.TRUE.equals(result.get("_stopExecution"))) {
                        stopCount.incrementAndGet();
                    } else {
                        primaryCount.incrementAndGet();
                    }
                    allDone.countDown();
                });

                boolean completed = allDone.await(5, TimeUnit.SECONDS);
                assertThat(completed).isTrue();
            }

            // Exactly one thread should be primary
            assertThat(primaryCount.get()).isEqualTo(1);
            // Exactly one thread should receive stop
            assertThat(stopCount.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("should return merged data for primary thread")
        void shouldReturnMergedDataForPrimary() throws Exception {
            Node node = createMergeNode("passThrough", 2);
            long executionId = System.nanoTime();
            ExecutionService.ExecutionContext context = createContext(executionId);

            Map<String, Object> primaryResult = new HashMap<>();
            CountDownLatch allDone = new CountDownLatch(2);

            try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
                executor.submit(() -> {
                    Map<String, Object> result = mergeExecutor.execute(node,
                            Map.of("key1", "value1"), context);
                    if (!Boolean.TRUE.equals(result.get("_stopExecution"))) {
                        synchronized (primaryResult) {
                            primaryResult.putAll(result);
                        }
                    }
                    allDone.countDown();
                });

                executor.submit(() -> {
                    Map<String, Object> result = mergeExecutor.execute(node,
                            Map.of("key2", "value2"), context);
                    if (!Boolean.TRUE.equals(result.get("_stopExecution"))) {
                        synchronized (primaryResult) {
                            primaryResult.putAll(result);
                        }
                    }
                    allDone.countDown();
                });

                allDone.await(5, TimeUnit.SECONDS);
            }

            // Primary result should contain merged data from both inputs
            assertThat(primaryResult).containsEntry("_mergeMode", "passThrough");
            assertThat(primaryResult).containsKey("key1");
            assertThat(primaryResult).containsKey("key2");
        }

        @Test
        @DisplayName("should handle single input passThrough")
        void shouldHandleSingleInput() {
            Node node = createMergeNode("passThrough", 1);
            long executionId = System.nanoTime();
            ExecutionService.ExecutionContext context = createContext(executionId);

            Map<String, Object> input = Map.of("only", "input");
            Map<String, Object> result = mergeExecutor.execute(node, input, context);

            // Single input means this thread is primary
            assertThat(result).doesNotContainKey("_stopExecution");
            assertThat(result).containsEntry("only", "input");
            assertThat(result).containsEntry("_mergeMode", "passThrough");
        }

        @Test
        @DisplayName("should handle three inputs with one primary")
        void shouldHandleThreeInputsOnePrimary() throws Exception {
            Node node = createMergeNode("passThrough", 3);
            long executionId = System.nanoTime();
            ExecutionService.ExecutionContext context = createContext(executionId);

            CountDownLatch allDone = new CountDownLatch(3);
            AtomicInteger primaryCount = new AtomicInteger(0);
            AtomicInteger stopCount = new AtomicInteger(0);

            try (ExecutorService executor = Executors.newFixedThreadPool(3)) {
                for (int i = 0; i < 3; i++) {
                    final int branchNum = i;
                    executor.submit(() -> {
                        Map<String, Object> input = Map.of("branch" + branchNum, "value" + branchNum);
                        Map<String, Object> result = mergeExecutor.execute(node, input, context);
                        if (Boolean.TRUE.equals(result.get("_stopExecution"))) {
                            stopCount.incrementAndGet();
                        } else {
                            primaryCount.incrementAndGet();
                        }
                        allDone.countDown();
                    });
                }

                boolean completed = allDone.await(5, TimeUnit.SECONDS);
                assertThat(completed).isTrue();
            }

            // Exactly one primary, two stopped
            assertThat(primaryCount.get()).isEqualTo(1);
            assertThat(stopCount.get()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Exclusive Mode (waitForAll=false)")
    class ExclusiveMode {

        @Test
        @DisplayName("should proceed immediately with first input when waitForAll=false")
        void shouldProceedImmediatelyWithFirstInput() {
            Node node = createExclusiveMergeNode("passThrough", 2);
            long executionId = System.nanoTime();
            ExecutionService.ExecutionContext context = createContext(executionId);

            // Only one input comes in (simulating IF node where only one branch fires)
            Map<String, Object> input = Map.of("branch", "true", "data", "value");
            Map<String, Object> result = mergeExecutor.execute(node, input, context);

            // Should proceed immediately without waiting
            assertThat(result).doesNotContainKey("_stopExecution");
            assertThat(result).containsEntry("_mergeMode", "passThrough");
            assertThat(result).containsEntry("_exclusive", true);
            assertThat(result).containsEntry("_inputsReceived", 1);
            assertThat(result).containsEntry("branch", "true");
            assertThat(result).containsEntry("data", "value");
        }

        @Test
        @DisplayName("should pass through input data directly in exclusive mode")
        void shouldPassThroughInputDataDirectly() {
            Node node = createExclusiveMergeNode("passThrough", 5);
            long executionId = System.nanoTime();
            ExecutionService.ExecutionContext context = createContext(executionId);

            Map<String, Object> input = Map.of(
                    "status", "SUCCESS",
                    "car", "Porsche",
                    "issueCount", 2);
            Map<String, Object> result = mergeExecutor.execute(node, input, context);

            // All input data should be in output
            assertThat(result).containsEntry("status", "SUCCESS");
            assertThat(result).containsEntry("car", "Porsche");
            assertThat(result).containsEntry("issueCount", 2);
            assertThat(result).containsEntry("_exclusive", true);
        }

        @Test
        @DisplayName("should work for conditional IF branches where only one fires")
        void shouldWorkForConditionalBranches() {
            // Simulates merge after IF node - only true OR false branch fires, not both
            Node node = createExclusiveMergeNode("passThrough", 2);
            long executionId = System.nanoTime();
            ExecutionService.ExecutionContext context = createContext(executionId);

            // Simulate the "true" branch completing
            Map<String, Object> trueBranchOutput = Map.of(
                    "conditionResult", true,
                    "recommendations", "Setup changes here");

            Map<String, Object> result = mergeExecutor.execute(node, trueBranchOutput, context);

            // Should NOT wait for the false branch
            assertThat(result).containsEntry("conditionResult", true);
            assertThat(result).containsEntry("recommendations", "Setup changes here");
            assertThat(result.get("_inputsReceived")).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("WaitAll Mode")
    class WaitAllMode {

        @Test
        @DisplayName("should wait for all inputs before returning")
        void shouldWaitForAllInputs() throws Exception {
            Node node = createMergeNode("waitAll", 2);
            long executionId = System.nanoTime();
            ExecutionService.ExecutionContext context = createContext(executionId);

            Map<String, Object> result1 = new HashMap<>();
            Map<String, Object> result2 = new HashMap<>();
            CountDownLatch allDone = new CountDownLatch(2);

            try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
                executor.submit(() -> {
                    Map<String, Object> r = mergeExecutor.execute(node,
                            Map.of("from", "branch1"), context);
                    synchronized (result1) {
                        result1.putAll(r);
                    }
                    allDone.countDown();
                });

                // Small delay to test waiting
                Thread.sleep(50);

                executor.submit(() -> {
                    Map<String, Object> r = mergeExecutor.execute(node,
                            Map.of("from", "branch2"), context);
                    synchronized (result2) {
                        result2.putAll(r);
                    }
                    allDone.countDown();
                });

                allDone.await(5, TimeUnit.SECONDS);
            }

            // Both should have received merged data
            assertThat(result1).containsEntry("_mergeMode", "waitAll");
            assertThat(result2).containsEntry("_mergeMode", "waitAll");
        }
    }

    @Nested
    @DisplayName("Append Mode")
    class AppendMode {

        @Test
        @DisplayName("should append all inputs to a list")
        void shouldAppendToList() throws Exception {
            Node node = createMergeNode("append", 2);
            long executionId = System.nanoTime();
            ExecutionService.ExecutionContext context = createContext(executionId);

            Map<String, Object> finalResult = new HashMap<>();
            CountDownLatch allDone = new CountDownLatch(2);

            try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
                executor.submit(() -> {
                    mergeExecutor.execute(node, Map.of("item", "first"), context);
                    allDone.countDown();
                });

                executor.submit(() -> {
                    Map<String, Object> r = mergeExecutor.execute(node,
                            Map.of("item", "second"), context);
                    synchronized (finalResult) {
                        finalResult.putAll(r);
                    }
                    allDone.countDown();
                });

                allDone.await(5, TimeUnit.SECONDS);
            }

            assertThat(finalResult).containsEntry("_mergeMode", "append");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> merged = (List<Map<String, Object>>) finalResult.get("merged");
            assertThat(merged).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Merge Mode")
    class MergeMode {

        @Test
        @DisplayName("should deep merge inputs into single map")
        void shouldDeepMergeInputs() throws Exception {
            Node node = createMergeNode("merge", 2);
            long executionId = System.nanoTime();
            ExecutionService.ExecutionContext context = createContext(executionId);

            Map<String, Object> finalResult = new HashMap<>();
            CountDownLatch allDone = new CountDownLatch(2);

            try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
                executor.submit(() -> {
                    mergeExecutor.execute(node, Map.of("key1", "value1"), context);
                    allDone.countDown();
                });

                executor.submit(() -> {
                    Map<String, Object> r = mergeExecutor.execute(node,
                            Map.of("key2", "value2"), context);
                    synchronized (finalResult) {
                        finalResult.putAll(r);
                    }
                    allDone.countDown();
                });

                allDone.await(5, TimeUnit.SECONDS);
            }

            assertThat(finalResult).containsEntry("_mergeMode", "merge");
            @SuppressWarnings("unchecked")
            Map<String, Object> merged = (Map<String, Object>) finalResult.get("merged");
            assertThat(merged).containsEntry("key1", "value1");
            assertThat(merged).containsEntry("key2", "value2");
        }
    }

    @Nested
    @DisplayName("Default Configuration")
    class DefaultConfiguration {

        @Test
        @DisplayName("should use waitAll mode by default")
        void shouldUseWaitAllByDefault() {
            Map<String, Object> params = new HashMap<>();
            params.put("inputCount", 1);
            params.put("timeout", 10);

            Node node = new Node(
                    "merge-1",
                    "merge",
                    "Merge Test",
                    new Node.Position(100.0, 100.0),
                    params,
                    null,
                    false,
                    null);

            ExecutionService.ExecutionContext context = createContext(System.nanoTime());
            Map<String, Object> result = mergeExecutor.execute(node, Map.of("test", "data"), context);

            assertThat(result).containsEntry("_mergeMode", "waitAll");
        }

        @Test
        @DisplayName("should use 'merged' as default output key")
        void shouldUseMergedAsDefaultKey() {
            Map<String, Object> params = new HashMap<>();
            params.put("inputCount", 1);
            params.put("timeout", 10);

            Node node = new Node(
                    "merge-1",
                    "merge",
                    "Merge Test",
                    new Node.Position(100.0, 100.0),
                    params,
                    null,
                    false,
                    null);

            ExecutionService.ExecutionContext context = createContext(System.nanoTime());
            Map<String, Object> result = mergeExecutor.execute(node, Map.of("test", "data"), context);

            assertThat(result).containsKey("merged");
        }
    }
}
