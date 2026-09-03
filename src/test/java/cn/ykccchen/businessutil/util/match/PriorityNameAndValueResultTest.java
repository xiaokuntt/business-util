package cn.ykccchen.businessutil.util.match;

import cn.ykccchen.businessutil.match.DuplicateKeyCheckLevel;
import cn.ykccchen.businessutil.match.DuplicateMatchKeyException;
import cn.ykccchen.businessutil.match.PriorityAssembler;
import cn.ykccchen.businessutil.match.PriorityFetcher;
import cn.ykccchen.businessutil.match.PriorityMatchFunction;
import cn.ykccchen.businessutil.match.PriorityMatchProcessor;
import cn.ykccchen.businessutil.match.PriorityMatchResult;
import cn.ykccchen.businessutil.match.PriorityNameAndValueHandler;
import cn.ykccchen.businessutil.match.handler.PriorityMode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriorityNameAndValueResultTest {

    @Test
    void exactMatchesExposeActualConfigurationValuesInPriorityOrder() {
        List<Config> configs = Arrays.asList(
                config("full", "CN", "APP"),
                config("fallback", "CN", null));

        PriorityFetcher<Source, Config, String> fetcher = assembler(configs).create();

        PriorityMatchResult<List<Config>> winner = fetcher.match(new Source("CN", "APP"));
        assertEquals("region:CN_channel:APP", winner.getNameAndValue());
        assertTrue(winner.toString().contains("nameAndValue='region:CN_channel:APP'"));
        List<PriorityMatchResult<List<Config>>> all = fetcher.match(new Source("CN", "APP"), true);
        assertEquals("region:CN_channel:APP", all.get(0).getNameAndValue());
        assertEquals("region:CN", all.get(1).getNameAndValue());
    }

    @Test
    void missingNamesFallBackToPriorityAndWhitespaceNamesRemainExact() {
        PriorityFetcher<Source, Config, String> fallback = PriorityAssembler
                .from(Source.class, Config.class, String.class)
                .initConfig(Collections.singletonList(config("one", "CN", "APP")))
                .addPriorityMatchFunction(Source::getA, Config::getA)
                .addPriorityMatchFunction(" ", Source::getB, Config::getB)
                .create();

        assertEquals("priority[0]:CN_ :APP",
                fallback.match(new Source("CN", "APP")).getNameAndValue());
    }

    @Test
    void exactDuplicateConfigurationsDoNotRepeatTheSameCompletePath() {
        PriorityFetcher<Source, Config, String> fetcher = assembler(Arrays.asList(
                config("first", "CN", "APP"),
                config("second", "CN", "APP")))
                .create();

        PriorityMatchResult<List<Config>> result = fetcher.match(new Source("CN", "APP"));
        assertEquals(Arrays.asList("first", "second"), ids(result));
        assertEquals("region:CN_channel:APP", result.getNameAndValue());
    }

    @Test
    void predicateBucketsExposeDistinctActualKeysInFirstSeenOrder() {
        PriorityAssembler<Source, Config, String> assembler = PriorityAssembler
                .from(Source.class, Config.class, String.class)
                .initConfig(Arrays.asList(
                        config("short", "U", null),
                        config("long", "US", null),
                        config("short-again", "U", null)))
                .addPriorityMatchFunction("prefix", Source::getA, Config::getA,
                        (source, configured) -> source.startsWith(configured));

        PriorityMatchResult<List<Config>> level = assembler.create().match(new Source("USA", null));
        List<PriorityMatchResult<List<Config>>> treeResults = assembler.create().tree()
                .match(new Source("USA", null), true);
        assertEquals(1, treeResults.size());
        PriorityMatchResult<List<Config>> tree = treeResults.get(0);

        assertEquals(Arrays.asList("short", "short-again", "long"), ids(level));
        assertEquals("prefix:U;prefix:US", level.getNameAndValue());
        assertEquals(level.getNameAndValue(), tree.getNameAndValue());
        assertEquals(ids(level), ids(tree));
    }

    @Test
    void duplicateSelfCheckAndNameAndValueMetadataRemainIndependent() {
        List<Config> duplicates = Arrays.asList(
                config("first", "CN", "APP"),
                config("second", "CN", "APP"));
        PriorityFetcher<Source, Config, String> warning = assembler(duplicates)
                .initDuplicateKeyCheck(DuplicateKeyCheckLevel.WARNING)
                .create();

        assertEquals(1, warning.getDuplicateKeyCheckReport().getDuplicateGroupCount());
        assertEquals(2, warning.getDuplicateKeyCheckReport().getDuplicateRecordCount());
        assertEquals("region:CN_channel:APP",
                warning.match(new Source("CN", "APP")).getNameAndValue());

        int[] handlerCalls = {0};
        assertThrows(DuplicateMatchKeyException.class,
                () -> assembler(duplicates)
                        .initDuplicateKeyCheck(DuplicateKeyCheckLevel.EXCEPTION)
                        .initPriorityNameAndValueHandler(
                                (name, priority, source, config, sourceValue, configValue) -> {
                                    handlerCalls[0]++;
                                    return name + ":" + configValue;
                                })
                        .create());
        assertEquals(0, handlerCalls[0], "EXCEPTION creation must stop before result formatting");
    }

    @Test
    void emptyConfigurationKeysAreMissingButWhitespaceValuesRemainExact() {
        PriorityFetcher<Source, Config, String> fetcher = assembler(Arrays.asList(
                config("space", "", " "),
                config("tab", null, "\t")))
                .create();

        PriorityMatchResult<List<Config>> space = fetcher.match(new Source("ignored", " "));
        assertEquals(Collections.singletonList("space"), ids(space));
        assertEquals("channel: ", space.getNameAndValue());
        PriorityMatchResult<List<Config>> tab = fetcher.match(new Source("ignored", "\t"));
        assertEquals(Collections.singletonList("tab"), ids(tab));
        assertEquals("channel:\t", tab.getNameAndValue());
    }

    @Test
    void customHandlerReceivesFullContextAndControlsRenderedPairs() {
        Config config = config("one", "CN", "APP");
        Source source = new Source("cn", "app");
        RecordingHandler handler = new RecordingHandler();
        PriorityFetcher<Source, Config, String> fetcher = PriorityAssembler
                .from(Source.class, Config.class, String.class)
                .initConfig(Collections.singletonList(config))
                .addPriorityMatchFunction("region", Source::getA, Config::getA,
                        String::equalsIgnoreCase)
                .addPriorityMatchFunction("channel", Source::getB, Config::getB,
                        String::equalsIgnoreCase)
                .initPriorityNameAndValueHandler(handler)
                .create();

        PriorityMatchResult<List<Config>> result = fetcher.match(source);

        assertEquals("custom-region=CN_custom-channel=APP", result.getNameAndValue());
        assertEquals(2, handler.calls.size());
        assertEquals("region|0|cn|CN", handler.calls.get(0));
        assertEquals("channel|1|app|APP", handler.calls.get(1));
        assertSame(source, handler.sources.get(0));
        assertSame(config, handler.configs.get(0));
    }

    @Test
    void nullHandlerOutputOmitsDimensionsAndCanProduceEmptyString() {
        PriorityFetcher<Source, Config, String> omitOne = assembler(
                Collections.singletonList(config("one", "CN", "APP")))
                .initPriorityNameAndValueHandler((name, priority, source, config, sourceValue, configValue) ->
                        priority == 0 ? null : name + ":" + configValue)
                .create();
        assertEquals("channel:APP", omitOne.match(new Source("CN", "APP")).getNameAndValue());

        PriorityFetcher<Source, Config, String> omitAll = assembler(
                Collections.singletonList(config("one", "CN", "APP")))
                .initPriorityNameAndValueHandler((name, priority, source, config, sourceValue, configValue) -> null)
                .create();
        assertEquals("", omitAll.match(new Source("CN", "APP")).getNameAndValue());
    }

    @Test
    void customHandlerExceptionPropagatesUnchangedAcrossModesEnginesAndApis() {
        RuntimeException sentinel = new RuntimeException("sentinel");
        for (PriorityMode mode : PriorityMode.values()) {
            for (boolean tree : Arrays.asList(false, true)) {
                for (boolean all : Arrays.asList(false, true)) {
                    PriorityFetcher<Source, Config, String> fetcher = assembler(
                            Collections.singletonList(config("one", "CN", "APP")))
                            .initPriorityHandler(mode)
                            .initPriorityNameAndValueHandler(
                                    (name, priority, source, config, sourceValue, configValue) -> {
                                        throw sentinel;
                                    })
                            .create();
                    if (tree) {
                        fetcher.tree();
                    }
                    RuntimeException thrown = assertThrows(RuntimeException.class,
                            () -> fetcher.match(new Source("CN", "APP"), all));
                    assertSame(sentinel, thrown);
                }
            }
        }
    }

    @Test
    void levelAndTreeStayEquivalentForBothPriorityModesAndMultipleShapes() {
        List<Config> configs = Arrays.asList(
                config("full", "CN", "APP"),
                config("region", "CN", null),
                config("channel", null, "APP"));
        Source source = new Source("CN", "APP");
        for (PriorityMode mode : PriorityMode.values()) {
            PriorityFetcher<Source, Config, String> level = assembler(configs).initPriorityHandler(mode).create();
            PriorityFetcher<Source, Config, String> tree = assembler(configs).initPriorityHandler(mode).create().tree();

            assertEquals(project(level.match(source, true)), project(tree.match(source, true)));
            assertEquals(level.match(source).getNameAndValue(), tree.match(source).getNameAndValue());
        }
    }

    @Test
    void oldAndNewResultConstructorsRemainAvailable() {
        PriorityMatchResult<String> oldResult = new PriorityMatchResult<>("id", "name", 1, "result");
        assertNull(oldResult.getNameAndValue());

        PriorityMatchResult<String> newResult = new PriorityMatchResult<>(
                "id", "name", "name:value", 1, "result");
        assertEquals("name:value", newResult.getNameAndValue());
        assertEquals("result", newResult.getResult());
    }

    @Test
    void directFetcherFactoryUsesDefaultNameAndValueHandler() {
        PriorityMatchFunction<Source, Config, String> function = PriorityMatchFunction.of(
                "region", 0, Source::getA, Config::getA);
        PriorityMatchProcessor<Source, Config, String> processor =
                new PriorityMatchProcessor<>(Collections.singletonList(function));
        PriorityFetcher<Source, Config, String> fetcher = PriorityFetcher.from(
                Collections.singletonList(processor),
                Collections.singletonList(config("one", "CN", null)),
                Collections.singletonList(function));

        assertEquals("region:CN", fetcher.match(new Source("CN", null)).getNameAndValue());
    }

    @Test
    void defaultHandlerSafelyRendersKeysWhoseToStringThrows() {
        ThrowingKey one = new ThrowingKey("same");
        KeyConfig config = new KeyConfig(one);
        PriorityFetcher<KeySource, KeyConfig, ThrowingKey> fetcher = PriorityAssembler
                .from(KeySource.class, KeyConfig.class, ThrowingKey.class)
                .initConfig(Collections.singletonList(config))
                .addPriorityMatchFunction("key", KeySource::getKey, KeyConfig::getKey)
                .create();

        String nameAndValue = fetcher.match(new KeySource(new ThrowingKey("same"))).getNameAndValue();
        assertTrue(nameAndValue.startsWith("key:<toString-failed:"));
        assertTrue(nameAndValue.contains(ThrowingKey.class.getName()));
    }

    @Test
    void resultUsesTheIndexedValueEvenIfConfigurationMutatesAfterCreate() {
        MutableConfig config = new MutableConfig("CN");
        PriorityFetcher<Source, MutableConfig, String> fetcher = PriorityAssembler
                .from(Source.class, MutableConfig.class, String.class)
                .initConfig(Collections.singletonList(config))
                .addPriorityMatchFunction("region", Source::getA, MutableConfig::getValue)
                .create();
        config.value = "US";

        PriorityMatchResult<List<MutableConfig>> result = fetcher.match(new Source("CN", null));
        assertEquals("region:CN", result.getNameAndValue());
        assertSame(config, result.getResult().get(0));
    }

    @Test
    void businessEqualConfigurationInstancesKeepIndependentIndexedPaths() {
        EqualConfig first = new EqualConfig("same-business-id", "U");
        EqualConfig second = new EqualConfig("same-business-id", "US");
        PriorityFetcher<Source, EqualConfig, String> fetcher = PriorityAssembler
                .from(Source.class, EqualConfig.class, String.class)
                .initConfig(Arrays.asList(first, second))
                .addPriorityMatchFunction("prefix", Source::getA, EqualConfig::getKey,
                        (source, configured) -> source.startsWith(configured))
                .create();

        PriorityMatchResult<List<EqualConfig>> result = fetcher.match(new Source("USA", null));
        assertEquals(Arrays.asList(first, second), result.getResult());
        assertEquals("prefix:U;prefix:US", result.getNameAndValue());
    }

    @Test
    void sourceGetterValueUsedForMatchingIsReusedByTheCustomHandler() {
        CountingSource source = new CountingSource("CN");
        List<String> handlerValues = new ArrayList<>();
        PriorityFetcher<CountingSource, Config, String> fetcher = PriorityAssembler
                .from(CountingSource.class, Config.class, String.class)
                .initConfig(Collections.singletonList(config("one", "CN", null)))
                .addPriorityMatchFunction("region", CountingSource::getValue, Config::getA)
                .initPriorityNameAndValueHandler(
                        (name, priority, request, config, sourceValue, configValue) -> {
                            handlerValues.add(sourceValue);
                            return name + ":" + configValue;
                        })
                .create();

        assertEquals("region:CN", fetcher.match(source).getNameAndValue());
        assertEquals(1, source.calls);
        assertEquals(Collections.singletonList("CN"), handlerValues);
    }

    @Test
    void sourceGetterForAnUnusedDimensionRemainsLazy() {
        SelectiveSource source = new SelectiveSource("CN");
        PriorityFetcher<SelectiveSource, Config, String> fetcher = PriorityAssembler
                .from(SelectiveSource.class, Config.class, String.class)
                .initConfig(Collections.singletonList(config("one", "CN", null)))
                .addPriorityMatchFunction("region", SelectiveSource::getUsedValue, Config::getA)
                .addPriorityMatchFunction("unused", SelectiveSource::getUnusedValue, Config::getB)
                .create();

        assertEquals("region:CN", fetcher.match(source).getNameAndValue());
        assertEquals(1, source.usedCalls);
        assertEquals(0, source.unusedCalls);
    }

    @Test
    void customHandlerCannotChangeLaterMatchesByMutatingTheSource() {
        List<MutableSourceConfig> configs = Arrays.asList(
                new MutableSourceConfig("high", "A", null),
                new MutableSourceConfig("low", null, "B"));
        MutableSource levelSource = new MutableSource("A", "B");
        MutableSource treeSource = new MutableSource("A", "B");
        PriorityNameAndValueHandler<MutableSource, MutableSourceConfig, String> mutatingHandler =
                (name, priority, source, config, sourceValue, configValue) -> {
                    if (priority == 0) {
                        source.b = "changed-by-handler";
                    }
                    return name + ":" + configValue;
                };
        PriorityAssembler<MutableSource, MutableSourceConfig, String> levelAssembler = mutableAssembler(configs)
                .initPriorityNameAndValueHandler(mutatingHandler);
        PriorityAssembler<MutableSource, MutableSourceConfig, String> treeAssembler = mutableAssembler(configs)
                .initPriorityNameAndValueHandler(mutatingHandler);

        List<PriorityMatchResult<List<MutableSourceConfig>>> level = levelAssembler.create()
                .match(levelSource, true);
        List<PriorityMatchResult<List<MutableSourceConfig>>> tree = treeAssembler.create().tree()
                .match(treeSource, true);

        assertEquals(2, level.size());
        assertEquals(Arrays.asList("high", "low"), mutableIds(level));
        assertEquals(Arrays.asList("region:A", "channel:B"), mutableNameAndValues(level));
        assertEquals(mutableIds(level), mutableIds(tree));
        assertEquals(mutableNameAndValues(level), mutableNameAndValues(tree));
    }

    @Test
    void nullHandlerFailsAtRegistration() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> assembler(Collections.singletonList(config("one", "CN", null)))
                        .initPriorityNameAndValueHandler(null));
        assertTrue(exception.getMessage().contains("name and value handler"));
    }

    private static PriorityAssembler<Source, Config, String> assembler(List<Config> configs) {
        return PriorityAssembler.from(Source.class, Config.class, String.class)
                .initConfig(configs)
                .addPriorityMatchFunction("region", Source::getA, Config::getA)
                .addPriorityMatchFunction("channel", Source::getB, Config::getB);
    }

    private static PriorityAssembler<MutableSource, MutableSourceConfig, String> mutableAssembler(
            List<MutableSourceConfig> configs) {
        return PriorityAssembler.from(MutableSource.class, MutableSourceConfig.class, String.class)
                .initConfig(configs)
                .addPriorityMatchFunction("region", MutableSource::getA, MutableSourceConfig::getA)
                .addPriorityMatchFunction("channel", MutableSource::getB, MutableSourceConfig::getB);
    }

    private static Config config(String id, String a, String b) {
        return new Config(id, a, b);
    }

    private static List<String> ids(PriorityMatchResult<List<Config>> result) {
        List<String> ids = new ArrayList<>();
        result.getResult().forEach(config -> ids.add(config.id));
        return ids;
    }

    private static List<List<String>> project(List<PriorityMatchResult<List<Config>>> results) {
        List<List<String>> projection = new ArrayList<>();
        for (PriorityMatchResult<List<Config>> result : results) {
            List<String> row = new ArrayList<>();
            row.add(result.getName());
            row.add(result.getNameAndValue());
            row.add(String.valueOf(result.getLevel()));
            row.addAll(ids(result));
            projection.add(row);
        }
        return projection;
    }

    private static List<String> mutableIds(
            List<PriorityMatchResult<List<MutableSourceConfig>>> results) {
        List<String> ids = new ArrayList<>();
        for (PriorityMatchResult<List<MutableSourceConfig>> result : results) {
            ids.add(result.getResult().get(0).id);
        }
        return ids;
    }

    private static List<String> mutableNameAndValues(
            List<PriorityMatchResult<List<MutableSourceConfig>>> results) {
        List<String> values = new ArrayList<>();
        for (PriorityMatchResult<List<MutableSourceConfig>> result : results) {
            values.add(result.getNameAndValue());
        }
        return values;
    }

    private static final class RecordingHandler implements PriorityNameAndValueHandler<Source, Config, String> {
        private final List<String> calls = new ArrayList<>();
        private final List<Source> sources = new ArrayList<>();
        private final List<Config> configs = new ArrayList<>();

        @Override
        public String handle(String defaultName,
                             int priority,
                             Source source,
                             Config config,
                             String sourceValue,
                             String matchedConfigValue) {
            calls.add(defaultName + "|" + priority + "|" + sourceValue + "|" + matchedConfigValue);
            sources.add(source);
            configs.add(config);
            return "custom-" + defaultName + "=" + matchedConfigValue;
        }
    }

    private static final class Source {
        private final String a;
        private final String b;

        private Source(String a, String b) {
            this.a = a;
            this.b = b;
        }

        String getA() {
            return a;
        }

        String getB() {
            return b;
        }
    }

    private static final class MutableSource {
        private final String a;
        private String b;

        private MutableSource(String a, String b) {
            this.a = a;
            this.b = b;
        }

        String getA() {
            return a;
        }

        String getB() {
            return b;
        }
    }

    private static final class MutableSourceConfig {
        private final String id;
        private final String a;
        private final String b;

        private MutableSourceConfig(String id, String a, String b) {
            this.id = id;
            this.a = a;
            this.b = b;
        }

        String getA() {
            return a;
        }

        String getB() {
            return b;
        }
    }

    private static final class Config {
        private final String id;
        private final String a;
        private final String b;

        private Config(String id, String a, String b) {
            this.id = id;
            this.a = a;
            this.b = b;
        }

        String getA() {
            return a;
        }

        String getB() {
            return b;
        }
    }

    private static final class KeySource {
        private final ThrowingKey key;

        private KeySource(ThrowingKey key) {
            this.key = key;
        }

        ThrowingKey getKey() {
            return key;
        }
    }

    private static final class MutableConfig {
        private String value;

        private MutableConfig(String value) {
            this.value = value;
        }

        String getValue() {
            return value;
        }
    }

    private static final class EqualConfig {
        private final String businessId;
        private final String key;

        private EqualConfig(String businessId, String key) {
            this.businessId = businessId;
            this.key = key;
        }

        String getKey() {
            return key;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof EqualConfig
                    && Objects.equals(businessId, ((EqualConfig) other).businessId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(businessId);
        }
    }

    private static final class CountingSource {
        private final String value;
        private int calls;

        private CountingSource(String value) {
            this.value = value;
        }

        String getValue() {
            calls++;
            if (calls > 1) {
                throw new IllegalStateException("source getter called more than once");
            }
            return value;
        }
    }

    private static final class SelectiveSource {
        private final String usedValue;
        private int usedCalls;
        private int unusedCalls;

        private SelectiveSource(String usedValue) {
            this.usedValue = usedValue;
        }

        String getUsedValue() {
            usedCalls++;
            return usedValue;
        }

        String getUnusedValue() {
            unusedCalls++;
            throw new IllegalStateException("unused getter must remain lazy");
        }
    }

    private static final class KeyConfig {
        private final ThrowingKey key;

        private KeyConfig(ThrowingKey key) {
            this.key = key;
        }

        ThrowingKey getKey() {
            return key;
        }
    }

    private static final class ThrowingKey {
        private final String value;

        private ThrowingKey(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof ThrowingKey
                    && Objects.equals(value, ((ThrowingKey) other).value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            throw new IllegalStateException("broken key rendering");
        }
    }
}
