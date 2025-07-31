package cn.ykccchen.businessutil.util.match;

import cn.ykccchen.businessutil.match.PriorityFetcher;
import cn.ykccchen.businessutil.match.PriorityMatchFunction;
import cn.ykccchen.businessutil.match.PriorityMatchProcessor;
import cn.ykccchen.businessutil.match.PriorityMatchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Nested;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PriorityFetcher 随机性测试用例
 * 通过随机数据测试各种边界情况和异常场景
 * @author ykccchen
 */
@DisplayName("PriorityFetcher 随机性测试")
class PriorityFetcherRandomTest {

    // 测试数据类
    static class RandomSource {
        private final String region;
        private final String tier;
        private final String category;
        private final String product;
        private final String brand;
        private final String color;
        private final int price;
        private final boolean isVip;

        public RandomSource(String region, String tier, String category, String product,
                            String brand, String color, int price, boolean isVip) {
            this.region = region;
            this.tier = tier;
            this.category = category;
            this.product = product;
            this.brand = brand;
            this.color = color;
            this.price = price;
            this.isVip = isVip;
        }

        // Getters
        public String getRegion() { return region; }
        public String getTier() { return tier; }
        public String getCategory() { return category; }
        public String getProduct() { return product; }
        public String getBrand() { return brand; }
        public String getColor() { return color; }
        public int getPrice() { return price; }
        public boolean isVip() { return isVip; }
    }

    static class RandomConfig {
        private final String region;
        private final String tier;
        private final String category;
        private final String product;
        private final String brand;
        private final String color;
        private final int minPrice;
        private final int maxPrice;
        private final boolean vipOnly;
        private final String value;
        private final int priority;

        public RandomConfig(String region, String tier, String category, String product,
                            String brand, String color, int minPrice, int maxPrice,
                            boolean vipOnly, String value, int priority) {
            this.region = region;
            this.tier = tier;
            this.category = category;
            this.product = product;
            this.brand = brand;
            this.color = color;
            this.minPrice = minPrice;
            this.maxPrice = maxPrice;
            this.vipOnly = vipOnly;
            this.value = value;
            this.priority = priority;
        }

        // Getters
        public String getRegion() { return region; }
        public String getTier() { return tier; }
        public String getCategory() { return category; }
        public String getProduct() { return product; }
        public String getBrand() { return brand; }
        public String getColor() { return color; }
        public int getMinPrice() { return minPrice; }
        public int getMaxPrice() { return maxPrice; }
        public boolean isVipOnly() { return vipOnly; }
        public String getValue() { return value; }
        public int getPriority() { return priority; }
    }

    private ThreadLocalRandom random;
    private List<PriorityMatchFunction<RandomSource, RandomConfig, String>> functions;
    private List<RandomConfig> configs;
    private PriorityFetcher<RandomSource, RandomConfig, String> fetcher;

    // 随机数据池
    private static final String[] REGIONS = {"US", "EU", "CN", "JP", "KR", "IN", "BR", "AU", "CA", "MX"};
    private static final String[] TIERS = {"premium", "standard", "basic", "vip", "enterprise", "student"};
    private static final String[] CATEGORIES = {"electronics", "clothing", "food", "books", "sports", "beauty", "home", "automotive"};
    private static final String[] PRODUCTS = {"phone", "laptop", "shirt", "shoes", "book", "ball", "cosmetic", "furniture"};
    private static final String[] BRANDS = {"Apple", "Samsung", "Nike", "Adidas", "CocaCola", "Toyota", "Sony", "LG"};
    private static final String[] COLORS = {"red", "blue", "green", "black", "white", "yellow", "purple", "orange"};

    @BeforeEach
    void setUp() {
        random = ThreadLocalRandom.current();

        // 初始化匹配函数
        functions = Arrays.asList(
                PriorityMatchFunction.of("region", 0, RandomSource::getRegion, RandomConfig::getRegion),
                PriorityMatchFunction.of("tier", 1, RandomSource::getTier, RandomConfig::getTier),
                PriorityMatchFunction.of("category", 2, RandomSource::getCategory, RandomConfig::getCategory),
                PriorityMatchFunction.of("product", 3, RandomSource::getProduct, RandomConfig::getProduct),
                PriorityMatchFunction.of("brand", 4, RandomSource::getBrand, RandomConfig::getBrand),
                PriorityMatchFunction.of("color", 5, RandomSource::getColor, RandomConfig::getColor),
                PriorityMatchFunction.of("price", 6,
                        s -> String.valueOf(s.getPrice() / 100), // 价格区间
                        c -> String.valueOf(c.getMinPrice() / 100)),
                PriorityMatchFunction.of("vip", 7,
                        s -> s.isVip() ? "vip" : "normal",
                        c -> c.isVipOnly() ? "vip" : "normal")
        );

        // 生成随机配置
        configs = generateRandomConfigs(100);

        // 创建处理器
        List<PriorityMatchProcessor<RandomSource, RandomConfig, String>> processors = generateRandomProcessors();

        fetcher = PriorityFetcher.from(processors, configs, functions);
    }

    @Nested
    @DisplayName("随机数据生成测试")
    class RandomDataGenerationTests {

        @RepeatedTest(10)
        @DisplayName("随机配置生成测试")
        void testRandomConfigGeneration() {
            List<RandomConfig> randomConfigs = generateRandomConfigs(random.nextInt( 50,200));

            assertFalse(randomConfigs.isEmpty());
            assertTrue(randomConfigs.size() >= 50);

            // 验证配置的多样性
            Set<String> regions = randomConfigs.stream()
                    .map(RandomConfig::getRegion)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            assertTrue(regions.size() > 1);

            Set<String> categories = randomConfigs.stream()
                    .map(RandomConfig::getCategory)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            assertTrue(categories.size() > 1);
        }

        @RepeatedTest(10)
        @DisplayName("随机源对象生成测试")
        void testRandomSourceGeneration() {
            List<RandomSource> randomSources = generateRandomSources(random.nextInt(10,100));

            assertFalse(randomSources.isEmpty());

            // 验证源对象的多样性
            Set<String> regions = randomSources.stream()
                    .map(RandomSource::getRegion)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            assertTrue(regions.size() > 1);
        }

        @RepeatedTest(5)
        @DisplayName("随机处理器生成测试")
        void testRandomProcessorGeneration() {
            List<PriorityMatchProcessor<RandomSource, RandomConfig, String>> processors = generateRandomProcessors();

            assertFalse(processors.isEmpty());

            // 验证处理器的多样性
            Set<Integer> functionCounts = processors.stream()
                    .map(PriorityMatchProcessor::getFunctionSize)
                    .collect(Collectors.toSet());
            assertTrue(functionCounts.size() >= 1);
        }
    }

    @Nested
    @DisplayName("随机匹配测试")
    class RandomMatchingTests {

        @RepeatedTest(50)
        @DisplayName("随机源对象匹配测试")
        void testRandomSourceMatching() {
            RandomSource source = generateRandomSource();

            PriorityMatchResult<List<RandomConfig>> result = fetcher.match(source);

            // 如果有匹配结果，验证结果的合理性
            if (result != null) {
                assertFalse(result.getResult().isEmpty());

                // 验证匹配结果的优先级
                for (RandomConfig config : result.getResult()) {
                    assertTrue(config.getPriority() >= 0);
                }
            }
        }

        @RepeatedTest(20)
        @DisplayName("随机全优先级匹配测试")
        void testRandomAllPriorityMatching() {
            RandomSource source = generateRandomSource();

            List<PriorityMatchResult<List<RandomConfig>>> results = fetcher.match(source, true);

            // 验证结果的优先级顺序
            for (int i = 0; i < results.size() - 1; i++) {
                assertTrue(results.get(i).getLevel() >= results.get(i + 1).getLevel());
            }
        }

        @RepeatedTest(10)
        @DisplayName("随机树模式匹配测试")
        void testRandomTreeModeMatching() {
            PriorityFetcher<RandomSource, RandomConfig, String> treeFetcher = fetcher.tree();
            RandomSource source = generateRandomSource();

            PriorityMatchResult<List<RandomConfig>> treeResult = treeFetcher.match(source);
            PriorityMatchResult<List<RandomConfig>> levelResult = fetcher.match(source);

            // 验证树模式和层级模式结果一致性
            if (treeResult != null && levelResult != null) {
                assertEquals(levelResult.getResult().get(0).getValue(),
                        treeResult.getResult().get(0).getValue());
            } else {
                assertNull(treeResult);
                assertNull(levelResult);
            }
        }
    }

    @Nested
    @DisplayName("随机边界条件测试")
    class RandomBoundaryTests {

        @RepeatedTest(20)
        @DisplayName("随机空值测试")
        void testRandomNullValues() {
            RandomSource source = generateRandomSourceWithNulls();

            PriorityMatchResult<List<RandomConfig>> result = fetcher.match(source);

            // 应该能正常处理空值，不会抛出异常
            assertDoesNotThrow(() -> fetcher.match(source));
        }

        @RepeatedTest(10)
        @DisplayName("随机极端值测试")
        void testRandomExtremeValues() {
            RandomSource source = generateRandomSourceWithExtremeValues();

            PriorityMatchResult<List<RandomConfig>> result = fetcher.match(source);

            // 应该能正常处理极端值
            assertDoesNotThrow(() -> fetcher.match(source));
        }

        @RepeatedTest(5)
        @DisplayName("随机大量数据测试")
        void testRandomLargeData() {
            List<RandomConfig> largeConfigs = generateRandomConfigs(random.nextInt(1000,5000));
            List<PriorityMatchProcessor<RandomSource, RandomConfig, String>> processors = generateRandomProcessors();

            long startTime = System.currentTimeMillis();
            PriorityFetcher<RandomSource, RandomConfig, String> largeFetcher =
                    PriorityFetcher.from(processors, largeConfigs, functions);
            long buildTime = System.currentTimeMillis() - startTime;

            RandomSource source = generateRandomSource();
            startTime = System.currentTimeMillis();
            PriorityMatchResult<List<RandomConfig>> result = largeFetcher.match(source);
            long matchTime = System.currentTimeMillis() - startTime;

            // 验证性能
            assertTrue(buildTime < 5000); // 构建时间应小于5秒
            assertTrue(matchTime < 1000);  // 匹配时间应小于1秒
        }
    }

    @Nested
    @DisplayName("随机压力测试")
    class RandomStressTests {

        @RepeatedTest(5)
        @DisplayName("随机并发测试")
        void testRandomConcurrency() throws InterruptedException {
            int threadCount = random.nextInt(5,20);
            int iterations = random.nextInt(100,500);

            List<Thread> threads = new ArrayList<>();
            List<Exception> exceptions = new ArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                Thread thread = new Thread(() -> {
                    try {
                        for (int j = 0; j < iterations; j++) {
                            RandomSource source = generateRandomSource();
                            fetcher.match(source);
                            fetcher.match(source, true);
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
    @DisplayName("随机异常场景测试")
    class RandomExceptionTests {

        @RepeatedTest(10)
        @DisplayName("随机异常数据测试")
        void testRandomExceptionData() {
            // 生成可能导致异常的随机数据
            List<RandomConfig> problematicConfigs = generateProblematicConfigs();
            List<PriorityMatchProcessor<RandomSource, RandomConfig, String>> processors = generateRandomProcessors();

            // 应该能正常处理异常数据
            assertDoesNotThrow(() -> {
                PriorityFetcher.from(processors, problematicConfigs, functions);
            });
        }

        @RepeatedTest(5)
        @DisplayName("随机循环引用测试")
        void testRandomCircularReference() {
            // 生成可能导致循环引用的处理器
            List<PriorityMatchProcessor<RandomSource, RandomConfig, String>> circularProcessors =
                    generateCircularProcessors();

            assertDoesNotThrow(() -> {
                PriorityFetcher.from(circularProcessors, configs, functions);
            });
        }
    }

    // 辅助方法

    private List<RandomConfig> generateRandomConfigs(int count) {
        List<RandomConfig> configs = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String region = random.nextBoolean() ? getRandomElement(REGIONS) : null;
            String tier = random.nextBoolean() ? getRandomElement(TIERS) : null;
            String category = random.nextBoolean() ? getRandomElement(CATEGORIES) : null;
            String product = random.nextBoolean() ? getRandomElement(PRODUCTS) : null;
            String brand = random.nextBoolean() ? getRandomElement(BRANDS) : null;
            String color = random.nextBoolean() ? getRandomElement(COLORS) : null;

            int minPrice = random.nextInt( 100,100000);
            int maxPrice = minPrice + random.nextInt(200,50000);
            boolean vipOnly = random.nextBoolean();
            String value = "config_" + i;
            int priority = random.nextInt(1,10);

            configs.add(new RandomConfig(region, tier, category, product, brand, color,
                    minPrice, maxPrice, vipOnly, value, priority));
        }

        return configs;
    }

    private List<RandomSource> generateRandomSources(int count) {
        List<RandomSource> sources = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String region = random.nextBoolean() ? getRandomElement(REGIONS) : null;
            String tier = random.nextBoolean() ? getRandomElement(TIERS) : null;
            String category = random.nextBoolean() ? getRandomElement(CATEGORIES) : null;
            String product = random.nextBoolean() ? getRandomElement(PRODUCTS) : null;
            String brand = random.nextBoolean() ? getRandomElement(BRANDS) : null;
            String color = random.nextBoolean() ? getRandomElement(COLORS) : null;
            int price = random.nextInt(100,100000);
            boolean isVip = random.nextBoolean();

            sources.add(new RandomSource(region, tier, category, product, brand, color, price, isVip));
        }

        return sources;
    }

    private List<PriorityMatchProcessor<RandomSource, RandomConfig, String>> generateRandomProcessors() {
        List<PriorityMatchProcessor<RandomSource, RandomConfig, String>> processors = new ArrayList<>();

        // 生成1-5个处理器
        int processorCount = random.nextInt( 2,6);

        for (int i = 0; i < processorCount; i++) {
            // 随机选择1-4个函数
            int functionCount = random.nextInt(1,8);
            List<PriorityMatchFunction<RandomSource, RandomConfig, String>> selectedFunctions =
                    new ArrayList<>();

            // 随机选择函数，确保不重复
            Set<Integer> selectedIndices = new HashSet<>();
            while (selectedFunctions.size() < functionCount && selectedIndices.size() < functions.size()) {
                int index = random.nextInt(functions.size());
                if (selectedIndices.add(index)) {
                    selectedFunctions.add(functions.get(index));
                }
            }

            processors.add(new PriorityMatchProcessor<>(selectedFunctions));
        }

        return processors;
    }

    private RandomSource generateRandomSource() {
        return new RandomSource(
                getRandomElement(REGIONS),
                getRandomElement(TIERS),
                getRandomElement(CATEGORIES),
                getRandomElement(PRODUCTS),
                getRandomElement(BRANDS),
                getRandomElement(COLORS),
                random.nextInt(50000,100000),
                random.nextBoolean()
        );
    }

    private RandomSource generateRandomSourceWithNulls() {
        return new RandomSource(
                random.nextBoolean() ? getRandomElement(REGIONS) : null,
                random.nextBoolean() ? getRandomElement(TIERS) : null,
                random.nextBoolean() ? getRandomElement(CATEGORIES) : null,
                random.nextBoolean() ? getRandomElement(PRODUCTS) : null,
                random.nextBoolean() ? getRandomElement(BRANDS) : null,
                random.nextBoolean() ? getRandomElement(COLORS) : null,
                random.nextInt(100000),
                random.nextBoolean()
        );
    }

    private RandomSource generateRandomSourceWithExtremeValues() {
        return new RandomSource(
                random.nextBoolean() ? "" : getRandomElement(REGIONS),
                random.nextBoolean() ? "EXTREME_VALUE_" + random.nextInt() : getRandomElement(TIERS),
                random.nextBoolean() ? null : getRandomElement(CATEGORIES),
                random.nextBoolean() ? "VERY_LONG_PRODUCT_NAME_THAT_MIGHT_CAUSE_ISSUES" : getRandomElement(PRODUCTS),
                random.nextBoolean() ? "特殊字符!@#$%" : getRandomElement(BRANDS),
                random.nextBoolean() ? "UNICODE_测试" : getRandomElement(COLORS),
                random.nextBoolean() ? Integer.MAX_VALUE : random.nextInt(100000),
                random.nextBoolean()
        );
    }

    private List<RandomConfig> generateProblematicConfigs() {
        List<RandomConfig> configs = new ArrayList<>();

        // 添加一些可能导致问题的配置
        configs.add(new RandomConfig("", "", "", "", "", "", -1, -1, false, "empty", 0));
        configs.add(new RandomConfig(null, null, null, null, null, null, 0, 0, false, "null", 0));
        configs.add(new RandomConfig("VERY_LONG_REGION_NAME", "VERY_LONG_TIER_NAME",
                "VERY_LONG_CATEGORY_NAME", "VERY_LONG_PRODUCT_NAME",
                "VERY_LONG_BRAND_NAME", "VERY_LONG_COLOR_NAME",
                Integer.MAX_VALUE, Integer.MAX_VALUE, true, "extreme", 0));

        // 添加一些随机配置
        configs.addAll(generateRandomConfigs(random.nextInt( 50)));

        return configs;
    }

    private List<PriorityMatchProcessor<RandomSource, RandomConfig, String>> generateCircularProcessors() {
        List<PriorityMatchProcessor<RandomSource, RandomConfig, String>> processors = new ArrayList<>();

        // 创建可能产生循环引用的处理器
        PriorityMatchFunction<RandomSource, RandomConfig, String> func1 = functions.get(0);
        PriorityMatchFunction<RandomSource, RandomConfig, String> func2 = functions.get(1);

        processors.add(new PriorityMatchProcessor<>(Arrays.asList(func1, func2)));
        processors.add(new PriorityMatchProcessor<>(Arrays.asList(func2, func1)));
        processors.add(new PriorityMatchProcessor<>(Arrays.asList(func1)));

        return processors;
    }

    private <T> T getRandomElement(T[] array) {
        return array[random.nextInt(array.length)];
    }
}
