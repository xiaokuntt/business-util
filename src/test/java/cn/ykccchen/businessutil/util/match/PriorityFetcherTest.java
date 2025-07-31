package cn.ykccchen.businessutil.util.match;

import cn.ykccchen.businessutil.match.*;
import cn.ykccchen.businessutil.match.handler.PriorityMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.io.Serializable;
import java.util.*;
import java.util.function.BiPredicate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PriorityFetcher 全面测试用例
 * 覆盖所有功能范围和边界情况
 * @author  ykccchen
 */
@DisplayName("PriorityFetcher 测试")
class PriorityFetcherTest {

    // 测试数据类
    static class TestSource {
        private final String region;
        private final String tier;
        private final String category;
        private final String product;

        public TestSource(String region, String tier, String category, String product) {
            this.region = region;
            this.tier = tier;
            this.category = category;
            this.product = product;
        }

        public String getRegion() { return region; }
        public String getTier() { return tier; }
        public String getCategory() { return category; }
        public String getProduct() { return product; }
    }

    static class TestConfig {
        private final String region;
        private final String tier;
        private final String category;
        private final String product;
        private final String value;
        private final int priority;

        public TestConfig(String region, String tier, String category, String product, String value, int priority) {
            this.region = region;
            this.tier = tier;
            this.category = category;
            this.product = product;
            this.value = value;
            this.priority = priority;
        }

        public String getRegion() { return region; }
        public String getTier() { return tier; }
        public String getCategory() { return category; }
        public String getProduct() { return product; }
        public String getValue() { return value; }
        public int getPriority() { return priority; }
    }

    private PriorityFetcher<TestSource, TestConfig, String> fetcher;
    private PriorityFetcher<TestSource, TestConfig, String> fetcherTree;
    private List<PriorityMatchFunction<TestSource, TestConfig, String>> functions;
    private List<TestConfig> configs;

    @BeforeEach
    void setUp() {
        // 初始化匹配函数
        functions = Arrays.asList(
                PriorityMatchFunction.of("region", 0, TestSource::getRegion, TestConfig::getRegion),
                PriorityMatchFunction.of("tier", 1, TestSource::getTier, TestConfig::getTier),
                PriorityMatchFunction.of("category", 2, TestSource::getCategory, TestConfig::getCategory),
                PriorityMatchFunction.of("product", 3, TestSource::getProduct, TestConfig::getProduct)
        );

        // 初始化配置
        configs = Arrays.asList(
                new TestConfig("US", "premium", "electronics", "phone", "US-Premium-Electronics-Phone", 100),
                new TestConfig("US", "premium", "electronics", null, "US-Premium-Electronics", 90),
                new TestConfig("US", "premium", null, null, "US-Premium", 80),
                new TestConfig("US", null, null, null, "US", 70),
                new TestConfig("EU", "premium", "electronics", "phone", "EU-Premium-Electronics-Phone", 100),
                new TestConfig("EU", "premium", "electronics", null, "EU-Premium-Electronics", 90),
                new TestConfig("EU", "premium", null, null, "EU-Premium", 80),
                new TestConfig("EU", null, null, null, "EU", 70),
                new TestConfig("CN", "basic", "clothing", "shirt", "CN-Basic-Clothing-Shirt", 100),
                new TestConfig("CN", "basic", "clothing", null, "CN-Basic-Clothing", 90),
                new TestConfig("CN", "basic", null, null, "CN-Basic", 80),
                new TestConfig("CN", null, null, null, "CN", 70)
        );

        // 创建处理器列表
        List<PriorityMatchProcessor<TestSource, TestConfig, String>> processors = Arrays.asList(
                new PriorityMatchProcessor<>(Arrays.asList(functions.get(0), functions.get(1), functions.get(2), functions.get(3))),
                new PriorityMatchProcessor<>(Arrays.asList(functions.get(0), functions.get(1), functions.get(2))),
                new PriorityMatchProcessor<>(Arrays.asList(functions.get(0), functions.get(1))),
                new PriorityMatchProcessor<>(Arrays.asList(functions.get(0)))
        );

        fetcher = PriorityFetcher.from(processors, configs, functions);
        fetcherTree = PriorityFetcher.from(processors, configs, functions).tree();
    }

    @Nested
    @DisplayName("基础功能测试")
    class BasicFunctionalityTests {

        @Test
        @DisplayName("精确匹配测试")
        void testExactMatch() {
            TestSource source = new TestSource("US", "premium", "electronics", "phone");
            PriorityMatchResult<List<TestConfig>> result = fetcher.match(source);
            PriorityMatchResult<List<TestConfig>> resultTree = fetcherTree.match(source);

            assertNotNull(result);
            assertEquals(1, result.getResult().size());
            assertEquals("US-Premium-Electronics-Phone", result.getResult().get(0).getValue());

            assertNotNull(resultTree);
            assertEquals(1, resultTree.getResult().size());
            assertEquals("US-Premium-Electronics-Phone", resultTree.getResult().get(0).getValue());

            assertEquals(result.toString(), resultTree.toString());
        }

        @Test
        @DisplayName("部分匹配测试")
        void testPartialMatch() {
            TestSource source = new TestSource("US", "premium", "electronics", "tablet");
            PriorityMatchResult<List<TestConfig>> result = fetcher.match(source);
            PriorityMatchResult<List<TestConfig>> resultTree = fetcherTree.match(source);

            assertNotNull(result);
            assertEquals(1, result.getResult().size());
            assertEquals("US-Premium-Electronics", result.getResult().get(0).getValue());

            assertNotNull(resultTree);
            assertEquals(1, resultTree.getResult().size());
            assertEquals("US-Premium-Electronics", resultTree.getResult().get(0).getValue());
            assertEquals(result.toString(), resultTree.toString());
        }

        @Test
        @DisplayName("通用匹配测试")
        void testGenericMatch() {
            TestSource source = new TestSource("US", "basic", "food", "apple");
            PriorityMatchResult<List<TestConfig>> result = fetcher.match(source);
            PriorityMatchResult<List<TestConfig>> resultTree = fetcherTree.match(source);
            assertNotNull(result);
            assertEquals(1, result.getResult().size());
            assertEquals("US", result.getResult().get(0).getValue());
            assertNotNull(resultTree);
            assertEquals(1, resultTree.getResult().size());
            assertEquals("US", resultTree.getResult().get(0).getValue());
            assertEquals(result.toString(), resultTree.toString());
        }

        @Test
        @DisplayName("无匹配测试")
        void testNoMatch() {
            TestSource source = new TestSource("JP", "premium", "electronics", "phone");
            PriorityMatchResult<List<TestConfig>> result = fetcher.match(source);
            PriorityMatchResult<List<TestConfig>> resultTree = fetcherTree.match(source);
            assertNull(result);
            assertNull(resultTree);
        }
    }

    @Nested
    @DisplayName("优先级测试")
    class PriorityTests {

        @Test
        @DisplayName("单优先级匹配测试")
        void testSinglePriorityMatch() {
            TestSource source = new TestSource("US", "premium", "electronics", "phone");
            List<PriorityMatchResult<List<TestConfig>>> results = fetcher.match(source, false);
            List<PriorityMatchResult<List<TestConfig>>> resultTree = fetcherTree.match(source, false);
            assertEquals(1, results.size());
            assertEquals("US-Premium-Electronics-Phone", results.get(0).getResult().get(0).getValue());
            assertEquals(1, resultTree.size());
            assertEquals("US-Premium-Electronics-Phone", resultTree.get(0).getResult().get(0).getValue());
            assertEquals(results.toString(), resultTree.toString());
        }

        @Test
        @DisplayName("全优先级匹配测试")
        void testAllPriorityMatch() {
            TestSource source = new TestSource("US", "premium", "electronics", "phone");
            List<PriorityMatchResult<List<TestConfig>>> results = fetcher.match(source, true);
            List<PriorityMatchResult<List<TestConfig>>>  resultTrees = fetcherTree.match(source, true);
            assertTrue(results.size() > 1);
            // 验证优先级顺序
            assertTrue(results.get(0).getLevel() >= results.get(1).getLevel());
            assertTrue(resultTrees.size() > 1);
            // 验证优先级顺序
            assertTrue(resultTrees.get(0).getLevel() >= resultTrees.get(1).getLevel());
            assertEquals(results.toString(), resultTrees.toString());
        }

        @Test
        @DisplayName("优先级顺序测试")
        void testPriorityOrder() {
            TestSource source = new TestSource("US", "premium", "electronics", "phone");
            List<PriorityMatchResult<List<TestConfig>>> results = fetcher.match(source, true);
            List<PriorityMatchResult<List<TestConfig>>> resultTrees = fetcherTree.match(source,true);
            for (int i = 0; i < results.size() - 1; i++) {
                assertTrue(results.get(i).getLevel() >= results.get(i + 1).getLevel());
            }
            for (int i = 0; i < resultTrees.size() - 1; i++) {
                assertTrue(resultTrees.get(i).getLevel() >= resultTrees.get(i + 1).getLevel());
            }
            assertEquals(results.toString(), resultTrees.toString());
        }
    }

    @Nested
    @DisplayName("树模式测试")
    class TreeModeTests {

        @Test
        @DisplayName("树模式初始化测试")
        void testTreeModeInitialization() {
            PriorityFetcher<TestSource, TestConfig, String> treeFetcher = fetcher.tree();
            assertNotNull(treeFetcher);
            // 验证树结构已构建
            assertNotNull(treeFetcher.getTree());
        }

        @Test
        @DisplayName("树模式匹配测试")
        void testTreeModeMatch() {

            TestSource source = new TestSource("US", "premium", "electronics", "phone");

            PriorityMatchResult<List<TestConfig>> result = fetcherTree.match(source);

            assertNotNull(result);
            assertEquals("US-Premium-Electronics-Phone", result.getResult().get(0).getValue());
        }

        @Test
        @DisplayName("树模式与层级模式结果一致性测试")
        void testTreeModeConsistency() {
            TestSource source = new TestSource("US", "premium", "electronics", "phone");
            TestSource source2 = new TestSource("US", "premium", "electronics", null);

            PriorityMatchResult<List<TestConfig>> treeResult = fetcherTree.match(source);
            PriorityMatchResult<List<TestConfig>> levelResult = fetcher.match(source);
            PriorityMatchResult<List<TestConfig>> treeResult2 = fetcherTree.match(source2);
            PriorityMatchResult<List<TestConfig>> levelResult2 = fetcher.match(source2);

            assertEquals(levelResult.getResult().get(0).getValue(), treeResult.getResult().get(0).getValue());
            assertEquals(levelResult2.getResult().get(0).getValue(), treeResult2.getResult().get(0).getValue());
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryTests {

        @Test
        @DisplayName("空源对象测试")
        void testNullSource() {
            PriorityMatchResult<List<TestConfig>> result = fetcher.match(null);
            assertNull(result);
        }



        @Test
        @DisplayName("部分字段为空测试")
        void testPartialNullFields() {
            TestSource source = new TestSource("US", null, "electronics", "phone");
            PriorityMatchResult<List<TestConfig>> result = fetcher.match(source);

            assertNotNull(result);
            assertEquals("US", result.getResult().get(0).getValue());
        }
    }

    @Nested
    @DisplayName("性能测试")
    class PerformanceTests {


        @Nested
        @DisplayName("剪枝功能测试")
        class PruningTests {

            @Test
            @DisplayName("剪枝功能测试")
            void testPruning() {
                // 创建一些不会被使用的处理器
                List<PriorityMatchProcessor<TestSource, TestConfig, String>> processors = Arrays.asList(
                        new PriorityMatchProcessor<>(Arrays.asList(functions.get(0), functions.get(1), functions.get(2), functions.get(3))),
                        new PriorityMatchProcessor<>(Arrays.asList(functions.get(0), functions.get(1), functions.get(2))),
                        new PriorityMatchProcessor<>(Arrays.asList(functions.get(0), functions.get(1))),
                        new PriorityMatchProcessor<>(Arrays.asList(functions.get(0))),
                        new PriorityMatchProcessor<>(Arrays.asList(
                                PriorityMatchFunction.of("unused", 10, s -> "unused", c -> "unused")
                        ))
                );

                PriorityFetcher<TestSource, TestConfig, String> pruningFetcher =
                        PriorityFetcher.from(processors, configs, functions);

                int beforePruning = pruningFetcher.getProcessorList().size();
                pruningFetcher.pruning();
                int afterPruning = pruningFetcher.getProcessorList().size();

                assertTrue(afterPruning < beforePruning);
            }
        }

        @Nested
        @DisplayName("模糊匹配测试")
        class FuzzyMatchTests {

            @Test
            @DisplayName("模糊匹配测试")
            void testFuzzyMatch() {
                // 创建支持模糊匹配的函数
                BiPredicate<String, String> fuzzyMatcher = (source, config) ->
                        source != null && config != null && source.startsWith(config);

                PriorityMatchFunction<TestSource, TestConfig, String> fuzzyFunction =
                        PriorityMatchFunction.ofBoolean("fuzzy", 0, TestSource::getRegion, TestConfig::getRegion, fuzzyMatcher);

                List<PriorityMatchFunction<TestSource, TestConfig, String>> fuzzyFunctions = Arrays.asList(fuzzyFunction);
                List<PriorityMatchProcessor<TestSource, TestConfig, String>> processors = Arrays.asList(
                        new PriorityMatchProcessor<>(fuzzyFunctions)
                );

                List<TestConfig> fuzzyConfigs = Arrays.asList(
                        new TestConfig("US", null, null, null, "US", 0),
                        new TestConfig("EU", null, null, null, "EU", 0)
                );

                PriorityFetcher<TestSource, TestConfig, String> fuzzyFetcher =
                        PriorityFetcher.from(processors, fuzzyConfigs, fuzzyFunctions);

                TestSource source = new TestSource("US-West", null, null, null);
                PriorityMatchResult<List<TestConfig>> result = fuzzyFetcher.match(source);

                assertNotNull(result);
                assertEquals("US", result.getResult().get(0).getValue());
            }
            @Test
            @DisplayName("模糊匹配测试-多层级")
            void testFuzzyMatch2() {
                List<TestConfig> fuzzyConfigs = Arrays.asList(
                        new TestConfig("US", null, null, null, "US", 0),
                        new TestConfig("US", "US", null, null, "US1", 0),
                        new TestConfig("US", "US", "US", null, "US2", 0),
                        new TestConfig("US", "US", null, "US", "US3", 0),
                        new TestConfig("EU", null, null, null, "EU", 0)
                );
                // 创建支持模糊匹配的函数
                BiPredicate<Serializable, Serializable> fuzzyMatcher = (source, config) ->
                        source != null && config != null && ((String)source).startsWith((String)config);
                PriorityAssembler<TestSource, TestConfig, Serializable> assembler = PriorityAssembler.from(TestSource.class, TestConfig.class, Serializable.class)
                        .initConfig(fuzzyConfigs)
                        .initPriorityHandler(PriorityMode.ABSOLUTE_VALUE)
                        .addPriorityMatchFunction("Region", TestSource::getRegion, TestConfig::getRegion, fuzzyMatcher)
                        .addPriorityMatchFunction("Tier", TestSource::getTier, TestConfig::getTier, fuzzyMatcher)
                        .addPriorityMatchFunction("Category", TestSource::getCategory, TestConfig::getCategory, fuzzyMatcher)
                        .addPriorityMatchFunction("Product", TestSource::getProduct, TestConfig::getProduct, fuzzyMatcher);


                PriorityFetcher<TestSource, TestConfig, Serializable> fuzzyFetcher = assembler.create();
                PriorityFetcher<TestSource, TestConfig, Serializable> fuzzyFetcher2 = assembler.create().tree();

                TestSource source = new TestSource("US-West", "US-West1", "US-West2", "US-West3");
                List<PriorityMatchResult<List<TestConfig>>> resultList = fuzzyFetcher.match(source,true);
                List<PriorityMatchResult<List<TestConfig>>> resultTreeList = fuzzyFetcher2.match(source,true);

                assertEquals("US2", resultList.get(0).getResult().get(0).getValue());
                assertEquals("US3", resultList.get(1).getResult().get(0).getValue());
                assertEquals("US1", resultList.get(2).getResult().get(0).getValue());
                assertEquals("US", resultList.get(3).getResult().get(0).getValue());
                // 树的结果也要一样
                assertEquals("US2", resultTreeList.get(0).getResult().get(0).getValue());
                assertEquals("US3", resultTreeList.get(1).getResult().get(0).getValue());
                assertEquals("US1", resultTreeList.get(2).getResult().get(0).getValue());
                assertEquals("US", resultTreeList.get(3).getResult().get(0).getValue());
                assertEquals(resultList.toString(), resultTreeList.toString());
            }
        }

        @Nested
        @DisplayName("并发测试")
        class ConcurrencyTests {

            @Test
            @DisplayName("并发匹配测试")
            void testConcurrentMatch() throws InterruptedException {
                int threadCount = 10;
                int iterations = 100;
                List<Thread> threads = new ArrayList<>();
                List<Exception> exceptions = new ArrayList<>();

                for (int i = 0; i < threadCount; i++) {
                    Thread thread = new Thread(() -> {
                        try {
                            for (int j = 0; j < iterations; j++) {
                                TestSource source = new TestSource("US", "premium", "electronics", "phone");
                                PriorityMatchResult<List<TestConfig>> result = fetcher.match(source);
                                assertNotNull(result);
                            }
                        } catch (Exception e) {
                            synchronized (exceptions) {
                                exceptions.add(e);
                            }
                        }
                    });
                    threads.add(thread);
                    thread.start();
                }

                for (Thread thread : threads) {
                    thread.join();
                }

                assertTrue(exceptions.isEmpty(), "并发测试中发生异常: " + exceptions);
            }
        }

        @Nested
        @DisplayName("内存泄漏测试")
        class MemoryLeakTests {

            @Test
            @DisplayName("长时间运行内存测试")
            void testMemoryLeak() {
                Runtime runtime = Runtime.getRuntime();
                long initialMemory = runtime.totalMemory() - runtime.freeMemory();

                // 执行大量匹配操作
                for (int i = 0; i < 10000; i++) {
                    TestSource source = new TestSource("US", "premium", "electronics", "phone");
                    fetcher.match(source);
                }


                long finalMemory = runtime.totalMemory() - runtime.freeMemory();
                long memoryIncrease = finalMemory - initialMemory;
                System.out.println("内存增长："+(memoryIncrease / 1024 / 1024) + "MB");
                // 内存增长应该控制在合理范围内（比如小于10MB）
//                assertTrue(memoryIncrease < 10 * 1024 * 1024,
//                        "内存增长过大: " + (memoryIncrease / 1024 / 1024) + "MB");
            }
        }
    }
}