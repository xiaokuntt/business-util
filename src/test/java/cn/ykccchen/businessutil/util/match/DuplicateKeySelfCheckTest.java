package cn.ykccchen.businessutil.util.match;

import cn.ykccchen.businessutil.match.DuplicateKeyCheckLevel;
import cn.ykccchen.businessutil.match.DuplicateKeyCheckReport;
import cn.ykccchen.businessutil.match.DuplicateKeySample;
import cn.ykccchen.businessutil.match.DuplicateMatchKeyException;
import cn.ykccchen.businessutil.match.PriorityAssembler;
import cn.ykccchen.businessutil.match.PriorityFetcher;
import cn.ykccchen.businessutil.match.PriorityMatchFunction;
import cn.ykccchen.businessutil.match.PriorityMatchProcessor;
import cn.ykccchen.businessutil.match.PriorityMatchResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DuplicateKeySelfCheckTest {

    @Test
    void defaultOffPreservesDuplicateMatchingAndExposesEmptyReport() {
        PriorityFetcher<TestSource, TestConfig, String> fetcher = assembler(Arrays.asList(
                config("first", "a", "b"),
                config("second", "a", "b")))
                .create();

        DuplicateKeyCheckReport<TestConfig, String> report = fetcher.getDuplicateKeyCheckReport();
        assertNotNull(report);
        assertEquals(DuplicateKeyCheckLevel.OFF, report.getLevel());
        assertFalse(report.hasDuplicates());
        assertEquals(0, report.getDuplicateGroupCount());
        assertEquals(0, report.getDuplicateRecordCount());
        assertEquals(Collections.emptyList(), report.getSamples());
        assertEquals(Arrays.asList("first", "second"), ids(fetcher.match(new TestSource("a", "b"))));
    }

    @Test
    void explicitOffAlsoSkipsReportingAndLogging() {
        RecordingHandler logs = new RecordingHandler();
        Logger logger = Logger.getLogger(PriorityFetcher.class.getName());
        boolean originalParentSetting = logger.getUseParentHandlers();
        Level originalLevel = logger.getLevel();
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);
        logger.addHandler(logs);
        PriorityFetcher<TestSource, TestConfig, String> fetcher;
        try {
            fetcher = assembler(Arrays.asList(
                    config("first", "a", "b"),
                    config("second", "a", "b")))
                    .initDuplicateKeyCheck(DuplicateKeyCheckLevel.OFF)
                    .create();
        } finally {
            logger.removeHandler(logs);
            logger.setUseParentHandlers(originalParentSetting);
            logger.setLevel(originalLevel);
        }

        assertEquals(0, logs.warningRecords().size());
        assertEquals(DuplicateKeyCheckLevel.OFF, fetcher.getDuplicateKeyCheckReport().getLevel());
        assertFalse(fetcher.getDuplicateKeyCheckReport().hasDuplicates());
        assertEquals(Arrays.asList("first", "second"), ids(fetcher.match(new TestSource("a", "b"))));
    }

    @Test
    void warningStoresReportLogsOnceAndPreservesIndependentTreeResults() {
        List<TestConfig> configs = Arrays.asList(
                config("first", "a", "b"),
                config("second", "a", "b"),
                config("fallback", "a", null));
        RecordingHandler logs = new RecordingHandler();
        Logger logger = Logger.getLogger(PriorityFetcher.class.getName());
        boolean originalParentSetting = logger.getUseParentHandlers();
        Level originalLevel = logger.getLevel();
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);
        logger.addHandler(logs);
        PriorityFetcher<TestSource, TestConfig, String> level;
        PriorityFetcher<TestSource, TestConfig, String> tree;
        try {
            level = assembler(configs).initDuplicateKeyCheck(DuplicateKeyCheckLevel.WARNING).create();
            tree = assembler(configs).initDuplicateKeyCheck(DuplicateKeyCheckLevel.WARNING).create().tree();
        } finally {
            logger.removeHandler(logs);
            logger.setUseParentHandlers(originalParentSetting);
            logger.setLevel(originalLevel);
        }

        assertEquals(2, logs.warningRecords().size(), "each create call must log exactly once");
        for (LogRecord record : logs.warningRecords()) {
            assertTrue(record.getMessage().contains("duplicateGroupCount=1"));
            assertTrue(record.getMessage().contains("duplicateRecordCount=2"));
            assertTrue(record.getMessage().contains("first"));
            assertTrue(record.getMessage().contains("second"));
        }
        DuplicateKeyCheckReport<TestConfig, String> report = level.getDuplicateKeyCheckReport();
        assertEquals(DuplicateKeyCheckLevel.WARNING, report.getLevel());
        assertEquals(1, report.getDuplicateGroupCount());
        assertEquals(2, report.getDuplicateRecordCount());
        assertEquals(Arrays.asList("first", "second"), sampleIds(report));
        assertEquals(DuplicateKeyCheckLevel.WARNING, tree.getDuplicateKeyCheckReport().getLevel());
        assertEquals(report.getDuplicateGroupCount(),
                tree.getDuplicateKeyCheckReport().getDuplicateGroupCount());
        assertEquals(report.getDuplicateRecordCount(),
                tree.getDuplicateKeyCheckReport().getDuplicateRecordCount());
        assertEquals(sampleIds(report), sampleIds(tree.getDuplicateKeyCheckReport()));
        assertEquals(project(level.match(new TestSource("a", "b"), true)),
                project(tree.match(new TestSource("a", "b"), true)));
    }

    @Test
    void exceptionAbortsCreationAndCarriesTheCompleteReport() {
        DuplicateMatchKeyException exception = assertThrows(DuplicateMatchKeyException.class,
                () -> assembler(Arrays.asList(
                        config("one", "a", "b"),
                        config("two", "a", "b"),
                        config("three", "a", "b")))
                        .initDuplicateKeyCheck(DuplicateKeyCheckLevel.EXCEPTION)
                        .create());

        DuplicateKeyCheckReport<?, ?> report = exception.getReport();
        assertEquals(DuplicateKeyCheckLevel.EXCEPTION, report.getLevel());
        assertEquals(1, report.getDuplicateGroupCount());
        assertEquals(3, report.getDuplicateRecordCount());
        assertEquals(3, report.getSamples().size());
        assertTrue(exception.getMessage().contains("duplicateGroupCount=1"));
        assertTrue(exception.getMessage().contains("duplicateRecordCount=3"));
    }

    @Test
    void enabledChecksRemainSilentWhenEveryFullKeyIsUnique() {
        List<TestConfig> configs = Arrays.asList(
                config("ab", "a", "b"),
                config("ac", "a", "c"),
                config("b", null, "b"));
        RecordingHandler logs = new RecordingHandler();
        Logger logger = Logger.getLogger(PriorityFetcher.class.getName());
        boolean originalParentSetting = logger.getUseParentHandlers();
        Level originalLevel = logger.getLevel();
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);
        logger.addHandler(logs);
        PriorityFetcher<TestSource, TestConfig, String> warning;
        try {
            warning = assembler(configs).initDuplicateKeyCheck(DuplicateKeyCheckLevel.WARNING).create();
        } finally {
            logger.removeHandler(logs);
            logger.setUseParentHandlers(originalParentSetting);
            logger.setLevel(originalLevel);
        }

        assertEquals(0, logs.warningRecords().size());
        assertEquals(DuplicateKeyCheckLevel.WARNING, warning.getDuplicateKeyCheckReport().getLevel());
        assertFalse(warning.getDuplicateKeyCheckReport().hasDuplicates());

        PriorityFetcher<TestSource, TestConfig, String> exceptionLevel = assembler(configs)
                .initDuplicateKeyCheck(DuplicateKeyCheckLevel.EXCEPTION)
                .create();
        assertEquals(DuplicateKeyCheckLevel.EXCEPTION,
                exceptionLevel.getDuplicateKeyCheckReport().getLevel());
        assertFalse(exceptionLevel.getDuplicateKeyCheckReport().hasDuplicates());
    }

    @Test
    void sampleLimitDoesNotTruncateGroupOrRecordCounts() {
        List<TestConfig> configs = new ArrayList<>();
        for (int index = 0; index < 12; index++) {
            configs.add(config("cfg-" + index, "a", "b"));
        }
        configs.add(config("other-0", "x", null));
        configs.add(config("other-1", "x", null));
        configs.add(config("other-2", "x", null));

        RecordingHandler logs = new RecordingHandler();
        Logger logger = Logger.getLogger(PriorityFetcher.class.getName());
        boolean originalParentSetting = logger.getUseParentHandlers();
        Level originalLevel = logger.getLevel();
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);
        logger.addHandler(logs);
        DuplicateKeyCheckReport<TestConfig, String> warningReport;
        try {
            warningReport = assembler(configs)
                    .initDuplicateKeyCheck(DuplicateKeyCheckLevel.WARNING)
                    .create()
                    .getDuplicateKeyCheckReport();
        } finally {
            logger.removeHandler(logs);
            logger.setUseParentHandlers(originalParentSetting);
            logger.setLevel(originalLevel);
        }
        assertEquals(2, warningReport.getDuplicateGroupCount());
        assertEquals(15, warningReport.getDuplicateRecordCount());
        assertEquals(10, warningReport.getSamples().size());
        assertEquals("cfg-0", warningReport.getSamples().get(0).getConfig().id);
        assertEquals("cfg-9", warningReport.getSamples().get(9).getConfig().id);
        assertFalse(warningReport.toString().contains("cfg-10"));
        assertFalse(warningReport.toString().contains("other-0"));
        assertEquals(1, logs.warningRecords().size());
        String warningMessage = logs.warningRecords().get(0).getMessage();
        assertTrue(warningMessage.contains("duplicateGroupCount=2"));
        assertTrue(warningMessage.contains("duplicateRecordCount=15"));
        assertTrue(warningMessage.contains("cfg-0"));
        assertTrue(warningMessage.contains("cfg-9"));
        assertFalse(warningMessage.contains("cfg-10"));
        assertFalse(warningMessage.contains("other-0"));

        DuplicateMatchKeyException exception = assertThrows(DuplicateMatchKeyException.class,
                () -> assembler(configs)
                        .initDuplicateKeyCheck(DuplicateKeyCheckLevel.EXCEPTION)
                        .create());
        assertEquals(2, exception.getReport().getDuplicateGroupCount());
        assertEquals(15, exception.getReport().getDuplicateRecordCount());
        assertEquals(10, exception.getReport().getSamples().size());
        assertTrue(exception.getMessage().contains("duplicateGroupCount=2"));
        assertTrue(exception.getMessage().contains("duplicateRecordCount=15"));
        assertTrue(exception.getMessage().contains("cfg-0"));
        assertTrue(exception.getMessage().contains("cfg-9"));
        assertFalse(exception.getMessage().contains("cfg-10"));
        assertFalse(exception.getMessage().contains("other-0"));
    }

    @Test
    void effectiveDimensionShapeSeparatesOtherwiseEqualRawKeyValues() {
        List<TestConfig> configs = Arrays.asList(
                config("a-null", "same", null),
                config("a-empty", "same", ""),
                config("b-null", null, "same"),
                config("b-empty", "", "same"));

        DuplicateKeyCheckReport<TestConfig, String> report = assembler(configs)
                .initDuplicateKeyCheck(DuplicateKeyCheckLevel.WARNING)
                .create()
                .getDuplicateKeyCheckReport();

        assertEquals(2, report.getDuplicateGroupCount());
        assertEquals(4, report.getDuplicateRecordCount());
        assertEquals(4, report.getSamples().size());
        assertEquals(Collections.singletonList(0), priorities(report.getSamples().get(0)));
        assertEquals(Collections.singletonList(0), priorities(report.getSamples().get(1)));
        assertEquals(Collections.singletonList(1), priorities(report.getSamples().get(2)));
        assertEquals(Collections.singletonList(1), priorities(report.getSamples().get(3)));
    }

    @Test
    void nullEmptyAndAllMissingRecordsAreIgnoredButWhitespaceIsExact() {
        List<TestConfig> configs = Arrays.asList(
                null,
                config("nulls", null, null),
                config("empties", "", ""),
                config("space-1", " ", null),
                config("space-2", " ", ""),
                config("tab-1", "\t", null),
                config("tab-2", "\t", null));

        DuplicateKeyCheckReport<TestConfig, String> report = assembler(configs)
                .initDuplicateKeyCheck(DuplicateKeyCheckLevel.WARNING)
                .create()
                .getDuplicateKeyCheckReport();

        assertEquals(2, report.getDuplicateGroupCount());
        assertEquals(4, report.getDuplicateRecordCount());
        assertEquals(Arrays.asList("space-1", "space-2", "tab-1", "tab-2"), sampleIds(report));
    }

    @Test
    void equalCustomKeyObjectsGroupAndAllReportViewsAreImmutable() {
        KeyConfig first = new KeyConfig("first", new EqualKey("same"));
        KeyConfig second = new KeyConfig("second", new EqualKey("same"));
        PriorityAssembler<KeySource, KeyConfig, EqualKey> assembler = PriorityAssembler
                .from(KeySource.class, KeyConfig.class, EqualKey.class)
                .initConfig(Arrays.asList(first, second))
                .addPriorityMatchFunction("key", KeySource::getKey, KeyConfig::getKey)
                .initDuplicateKeyCheck(DuplicateKeyCheckLevel.WARNING);

        DuplicateKeyCheckReport<KeyConfig, EqualKey> report = assembler.create()
                .getDuplicateKeyCheckReport();
        assertEquals(1, report.getDuplicateGroupCount());
        assertEquals(2, report.getDuplicateRecordCount());
        assertThrows(UnsupportedOperationException.class, () -> report.getSamples().clear());
        DuplicateKeySample<KeyConfig, EqualKey> sample = report.getSamples().get(0);
        assertThrows(UnsupportedOperationException.class, () -> sample.getKeyParts().clear());
        assertEquals("key", sample.getKeyParts().get(0).getName());
        assertEquals(0, sample.getKeyParts().get(0).getPriority());
        assertEquals(new EqualKey("same"), sample.getKeyParts().get(0).getKey());
    }

    @Test
    void overlappingCustomPredicateKeysAreNotTreatedAsExactDuplicates() {
        PriorityFetcher<TestSource, TestConfig, String> fetcher = PriorityAssembler
                .from(TestSource.class, TestConfig.class, String.class)
                .initConfig(Arrays.asList(
                        config("short-prefix", "U", null),
                        config("long-prefix", "US", null)))
                .addPriorityMatchFunction("prefix", TestSource::getA, TestConfig::getA,
                        (source, configured) -> source.startsWith(configured))
                .initDuplicateKeyCheck(DuplicateKeyCheckLevel.EXCEPTION)
                .create();

        assertFalse(fetcher.getDuplicateKeyCheckReport().hasDuplicates());
        assertEquals(Arrays.asList("short-prefix", "long-prefix"),
                ids(fetcher.match(new TestSource("USA", null))));
    }

    @Test
    void throwingBusinessToStringCannotBreakWarningOrMaskDomainException() {
        ThrowingConfig first = new ThrowingConfig(new ThrowingKey("a"));
        ThrowingConfig second = new ThrowingConfig(new ThrowingKey("a"));
        PriorityAssembler<TestSource, ThrowingConfig, ThrowingKey> warningAssembler = PriorityAssembler
                .from(TestSource.class, ThrowingConfig.class, ThrowingKey.class)
                .initConfig(Arrays.asList(first, second))
                .addPriorityMatchFunction("A", source -> new ThrowingKey(source.getA()), ThrowingConfig::getA)
                .initDuplicateKeyCheck(DuplicateKeyCheckLevel.WARNING);

        DuplicateKeyCheckReport<ThrowingConfig, ThrowingKey> warningReport = warningAssembler.create()
                .getDuplicateKeyCheckReport();
        assertTrue(warningReport.hasDuplicates());
        assertTrue(warningReport.toString().contains("toString-failed"));

        PriorityAssembler<TestSource, ThrowingConfig, ThrowingKey> exceptionAssembler = PriorityAssembler
                .from(TestSource.class, ThrowingConfig.class, ThrowingKey.class)
                .initConfig(Arrays.asList(first, second))
                .addPriorityMatchFunction("A", source -> new ThrowingKey(source.getA()), ThrowingConfig::getA)
                .initDuplicateKeyCheck(DuplicateKeyCheckLevel.EXCEPTION);
        DuplicateMatchKeyException exception = assertThrows(DuplicateMatchKeyException.class,
                exceptionAssembler::create);
        assertTrue(exception.getMessage().contains("toString-failed"));
        assertEquals(2, exception.getReport().getDuplicateRecordCount());
    }

    @Test
    void throwingApplicationLogHandlerCannotUpgradeWarningToCreationFailure() {
        Logger logger = Logger.getLogger(PriorityFetcher.class.getName());
        boolean originalParentSetting = logger.getUseParentHandlers();
        Level originalLevel = logger.getLevel();
        ThrowingHandler handler = new ThrowingHandler();
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);
        logger.addHandler(handler);
        PriorityFetcher<TestSource, TestConfig, String> fetcher;
        try {
            fetcher = assembler(Arrays.asList(
                    config("first", "a", "b"),
                    config("second", "a", "b")))
                    .initDuplicateKeyCheck(DuplicateKeyCheckLevel.WARNING)
                    .create();
        } finally {
            logger.removeHandler(handler);
            logger.setUseParentHandlers(originalParentSetting);
            logger.setLevel(originalLevel);
        }

        assertEquals(1, handler.publishCount);
        assertEquals(1, fetcher.getDuplicateKeyCheckReport().getDuplicateGroupCount());
        assertEquals(2, fetcher.getDuplicateKeyCheckReport().getDuplicateRecordCount());
    }

    @Test
    void nullLevelFailsFast() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> assembler(Collections.singletonList(config("one", "a", null)))
                        .initDuplicateKeyCheck(null));
        assertTrue(exception.getMessage().contains("check level"));
    }

    @Test
    void legacyFetcherFactoryRemainsOff() {
        PriorityMatchFunction<TestSource, TestConfig, String> function = PriorityMatchFunction.of(
                "A", 0, TestSource::getA, TestConfig::getA);
        PriorityMatchProcessor<TestSource, TestConfig, String> processor =
                new PriorityMatchProcessor<>(Collections.singletonList(function));
        PriorityFetcher<TestSource, TestConfig, String> fetcher = PriorityFetcher.from(
                Collections.singletonList(processor),
                Arrays.asList(config("one", "a", null), config("two", "a", null)),
                Collections.singletonList(function));

        assertEquals(DuplicateKeyCheckLevel.OFF, fetcher.getDuplicateKeyCheckReport().getLevel());
        assertFalse(fetcher.getDuplicateKeyCheckReport().hasDuplicates());
        assertEquals(Arrays.asList("one", "two"), ids(fetcher.match(new TestSource("a", null))));
    }

    private static PriorityAssembler<TestSource, TestConfig, String> assembler(List<TestConfig> configs) {
        return PriorityAssembler.from(TestSource.class, TestConfig.class, String.class)
                .initConfig(configs)
                .addPriorityMatchFunction("A", TestSource::getA, TestConfig::getA)
                .addPriorityMatchFunction("B", TestSource::getB, TestConfig::getB);
    }

    private static TestConfig config(String id, String a, String b) {
        return new TestConfig(id, a, b);
    }

    private static List<String> ids(PriorityMatchResult<List<TestConfig>> result) {
        List<String> ids = new ArrayList<>();
        for (TestConfig config : result.getResult()) {
            ids.add(config.id);
        }
        return ids;
    }

    private static List<String> sampleIds(DuplicateKeyCheckReport<TestConfig, String> report) {
        List<String> ids = new ArrayList<>();
        for (DuplicateKeySample<TestConfig, String> sample : report.getSamples()) {
            ids.add(sample.getConfig().id);
        }
        return ids;
    }

    private static List<Integer> priorities(DuplicateKeySample<TestConfig, String> sample) {
        List<Integer> priorities = new ArrayList<>();
        sample.getKeyParts().forEach(part -> priorities.add(part.getPriority()));
        return priorities;
    }

    private static List<List<String>> project(List<PriorityMatchResult<List<TestConfig>>> results) {
        List<List<String>> projection = new ArrayList<>();
        for (PriorityMatchResult<List<TestConfig>> result : results) {
            List<String> row = new ArrayList<>();
            row.add(result.getName());
            row.add(String.valueOf(result.getLevel()));
            result.getResult().forEach(config -> row.add(config.id));
            projection.add(row);
        }
        return projection;
    }

    private static final class RecordingHandler extends Handler {
        private final List<LogRecord> records = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        List<LogRecord> warningRecords() {
            List<LogRecord> warnings = new ArrayList<>();
            for (LogRecord record : records) {
                if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
                    warnings.add(record);
                }
            }
            return warnings;
        }
    }

    private static final class ThrowingHandler extends Handler {
        private int publishCount;

        @Override
        public void publish(LogRecord record) {
            publishCount++;
            throw new IllegalStateException("broken application logging handler");
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }

    private static final class TestSource {
        private final String a;
        private final String b;

        private TestSource(String a, String b) {
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

    private static final class TestConfig {
        private final String id;
        private final String a;
        private final String b;

        private TestConfig(String id, String a, String b) {
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

        @Override
        public String toString() {
            return id;
        }
    }

    private static final class KeySource {
        private final EqualKey key;

        private KeySource(EqualKey key) {
            this.key = key;
        }

        EqualKey getKey() {
            return key;
        }
    }

    private static final class KeyConfig {
        private final String id;
        private final EqualKey key;

        private KeyConfig(String id, EqualKey key) {
            this.id = id;
            this.key = key;
        }

        EqualKey getKey() {
            return key;
        }

        @Override
        public String toString() {
            return id;
        }
    }

    private static final class EqualKey {
        private final String value;

        private EqualKey(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof EqualKey)) {
                return false;
            }
            EqualKey equalKey = (EqualKey) other;
            return Objects.equals(value, equalKey.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return value;
        }
    }

    private static final class ThrowingConfig {
        private final ThrowingKey a;

        private ThrowingConfig(ThrowingKey a) {
            this.a = a;
        }

        ThrowingKey getA() {
            return a;
        }

        @Override
        public String toString() {
            throw new IllegalStateException("broken diagnostic rendering");
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
            throw new IllegalStateException("broken key diagnostic rendering");
        }
    }
}
