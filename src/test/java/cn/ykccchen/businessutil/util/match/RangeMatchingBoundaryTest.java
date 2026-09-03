package cn.ykccchen.businessutil.util.match;

import cn.ykccchen.businessutil.match.PriorityAssembler;
import cn.ykccchen.businessutil.match.PriorityFetcher;
import cn.ykccchen.businessutil.match.PriorityMatchResult;
import cn.ykccchen.businessutil.match.handler.PriorityMode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RangeMatchingBoundaryTest {

    @Test
    void numericRangesHonorEveryOpenAndClosedBoundary() {
        BigDecimal ten = decimal("10");
        BigDecimal fifteen = decimal("15");
        BigDecimal twenty = decimal("20");
        List<RangeConfig> configs = Arrays.asList(
                config("closed", interval(ten, true, twenty, true)),
                config("open", interval(ten, false, twenty, false)),
                config("closed-open", interval(ten, true, twenty, false)),
                config("open-closed", interval(ten, false, twenty, true)));

        assertAcrossAllModesAndEngines(configs, decimal("9"), Collections.emptyList());
        assertAcrossAllModesAndEngines(configs, ten, Arrays.asList("closed", "closed-open"));
        assertAcrossAllModesAndEngines(configs, fifteen,
                Arrays.asList("closed", "open", "closed-open", "open-closed"));
        assertAcrossAllModesAndEngines(configs, twenty, Arrays.asList("closed", "open-closed"));
        assertAcrossAllModesAndEngines(configs, decimal("21"), Collections.emptyList());

        List<RangeConfig> adjacent = Arrays.asList(
                config("left", interval(decimal("0"), true, ten, false)),
                config("right", interval(ten, true, twenty, false)));
        assertAcrossAllModesAndEngines(adjacent, ten, Collections.singletonList("right"));

        List<RangeConfig> integerExtremes = Collections.singletonList(config("extremes",
                interval(Integer.MIN_VALUE, true, Integer.MAX_VALUE, true)));
        assertAcrossAllModesAndEngines(integerExtremes, Integer.MIN_VALUE,
                Collections.singletonList("extremes"));
        assertAcrossAllModesAndEngines(integerExtremes, Integer.MAX_VALUE,
                Collections.singletonList("extremes"));
    }

    @Test
    void numericRangesCoverNegativeDecimalSingletonAndOverlappingBuckets() {
        Interval<BigDecimal> first = interval(decimal("-1.5"), true, decimal("10"), true);
        List<RangeConfig> configs = Arrays.asList(
                config("A-1", first),
                config("B", interval(decimal("5"), true, decimal("15"), true)),
                config("A-2", first),
                config("point", interval(decimal("7"), true, decimal("7"), true)));

        assertAcrossAllModesAndEngines(configs, decimal("-1.6"), Collections.emptyList());
        assertAcrossAllModesAndEngines(configs, decimal("-1.5"), Arrays.asList("A-1", "A-2"));
        assertAcrossAllModesAndEngines(configs, BigDecimal.ZERO, Arrays.asList("A-1", "A-2"));
        assertAcrossAllModesAndEngines(configs, decimal("7"), Arrays.asList("A-1", "A-2", "B", "point"));
        assertAcrossAllModesAndEngines(configs, decimal("15"), Collections.singletonList("B"));
        assertThrows(IllegalArgumentException.class,
                () -> interval(decimal("2"), true, decimal("1"), true));
        assertThrows(IllegalArgumentException.class,
                () -> interval(decimal("1"), false, decimal("1"), true));
    }

    @Test
    void timeRangesHonorEveryBoundaryAndCrossDayIntervals() {
        Instant start = Instant.parse("2026-09-03T10:00:00Z");
        Instant middle = Instant.parse("2026-09-03T10:30:00Z");
        Instant end = Instant.parse("2026-09-03T11:00:00Z");
        List<RangeConfig> configs = Arrays.asList(
                config("closed", interval(start, true, end, true)),
                config("open", interval(start, false, end, false)),
                config("closed-open", interval(start, true, end, false)),
                config("open-closed", interval(start, false, end, true)));

        assertAcrossAllModesAndEngines(configs, start.minusNanos(1), Collections.emptyList());
        assertAcrossAllModesAndEngines(configs, start, Arrays.asList("closed", "closed-open"));
        assertAcrossAllModesAndEngines(configs, middle,
                Arrays.asList("closed", "open", "closed-open", "open-closed"));
        assertAcrossAllModesAndEngines(configs, end, Arrays.asList("closed", "open-closed"));
        assertAcrossAllModesAndEngines(configs, end.plusNanos(1), Collections.emptyList());

        List<RangeConfig> adjacent = Arrays.asList(
                config("early", interval(start, true, middle, false)),
                config("late", interval(middle, true, end, false)));
        assertAcrossAllModesAndEngines(adjacent, middle, Collections.singletonList("late"));

        List<RangeConfig> crossDay = Collections.singletonList(config("cross-day",
                interval(Instant.parse("2026-09-03T23:00:00Z"), true,
                        Instant.parse("2026-09-04T01:00:00Z"), false)));
        assertAcrossAllModesAndEngines(crossDay, Instant.parse("2026-09-04T00:00:00Z"),
                Collections.singletonList("cross-day"));
    }

    @Test
    void timeRangesAggregateOverlapsAndUseCallerNormalizedInstants() {
        Interval<Instant> first = interval(Instant.parse("2026-09-03T10:00:00Z"), true,
                Instant.parse("2026-09-03T11:00:00Z"), true);
        Interval<Instant> second = interval(Instant.parse("2026-09-03T10:30:00Z"), true,
                Instant.parse("2026-09-03T11:30:00Z"), true);
        List<RangeConfig> configs = Arrays.asList(
                config("A-1", first), config("B", second), config("A-2", first));

        Instant sameInstantInShanghai = OffsetDateTime.parse("2026-09-03T18:45:00+08:00").toInstant();
        assertEquals(Instant.parse("2026-09-03T10:45:00Z"), sameInstantInShanghai);
        assertAcrossAllModesAndEngines(configs, sameInstantInShanghai, Arrays.asList("A-1", "A-2", "B"));
    }

    @Test
    void missingRangeKeysAreIgnoredWithoutCallingThePredicate() {
        AtomicInteger predicateCalls = new AtomicInteger();
        List<RangeConfig> configs = Arrays.asList(
                null,
                config("null", null),
                config("empty", ""),
                config("valid", interval(decimal("0"), true, decimal("10"), true)));

        for (PriorityMode mode : PriorityMode.values()) {
            PriorityAssembler<RangeSource, RangeConfig, Object> assembler =
                    PriorityAssembler.from(RangeSource.class, RangeConfig.class, Object.class)
                            .initConfig(configs)
                            .initPriorityHandler(mode)
                            .addPriorityMatchFunction("range", RangeSource::getValue, RangeConfig::getRange,
                                    (source, range) -> {
                                        predicateCalls.incrementAndGet();
                                        return contains(source, range);
                                    });
            for (PriorityFetcher<RangeSource, RangeConfig, Object> fetcher
                    : Arrays.asList(assembler.create(), assembler.create().tree())) {
                int callsBeforeMissingValues = predicateCalls.get();
                assertNull(fetcher.match(new RangeSource(null)));
                assertEquals(Collections.emptyList(), fetcher.match(new RangeSource(null), true));
                assertNull(fetcher.match(new RangeSource("")));
                assertEquals(Collections.emptyList(), fetcher.match(new RangeSource(""), true));
                assertEquals(callsBeforeMissingValues, predicateCalls.get());
                int callsBeforeValidValue = predicateCalls.get();
                assertEquals(Collections.singletonList("valid"),
                        ids(fetcher.match(new RangeSource(decimal("5"))).getResult()));
                assertEquals(callsBeforeValidValue + 1, predicateCalls.get());
            }
        }
    }

    @Test
    void predicateExceptionsPropagateUnchangedAcrossEveryExecutionPath() {
        SentinelException sentinel = new SentinelException();
        List<RangeConfig> configs = Collections.singletonList(
                config("range", interval(decimal("0"), true, decimal("10"), true)));
        for (PriorityMode mode : PriorityMode.values()) {
            PriorityAssembler<RangeSource, RangeConfig, Object> assembler =
                    PriorityAssembler.from(RangeSource.class, RangeConfig.class, Object.class)
                            .initConfig(configs)
                            .initPriorityHandler(mode)
                            .addPriorityMatchFunction("range", RangeSource::getValue, RangeConfig::getRange,
                                    (source, range) -> {
                                        throw sentinel;
                                    });
            for (PriorityFetcher<RangeSource, RangeConfig, Object> fetcher
                    : Arrays.asList(assembler.create(), assembler.create().tree())) {
                assertSame(sentinel, assertThrows(SentinelException.class,
                        () -> fetcher.match(new RangeSource(decimal("5")))));
                assertSame(sentinel, assertThrows(SentinelException.class,
                        () -> fetcher.match(new RangeSource(decimal("5")), true)));
            }
        }
    }

    @Test
    void numericAndTimeRangeDimensionsParticipateInPriorityOrdering() {
        Instant start = Instant.parse("2026-09-03T10:00:00Z");
        Instant end = Instant.parse("2026-09-03T11:00:00Z");
        MultiConfig numberOnly = new MultiConfig("number", interval(decimal("0"), true, decimal("10"), true),
                null, null);
        MultiConfig timeAndTag = new MultiConfig("time-tag", null, interval(start, true, end, true), "x");
        MultiSource source = new MultiSource(decimal("5"), Instant.parse("2026-09-03T10:30:00Z"), "x");

        for (PriorityMode mode : PriorityMode.values()) {
            PriorityAssembler<MultiSource, MultiConfig, Object> assembler =
                    PriorityAssembler.from(MultiSource.class, MultiConfig.class, Object.class)
                            .initConfig(Arrays.asList(numberOnly, timeAndTag))
                            .initPriorityHandler(mode)
                            .addPriorityMatchFunction("number", MultiSource::getNumber, MultiConfig::getNumber,
                                    RangeMatchingBoundaryTest::contains)
                            .addPriorityMatchFunction("time", MultiSource::getTime, MultiConfig::getTime,
                                    RangeMatchingBoundaryTest::contains)
                            .addPriorityMatchFunction("tag", MultiSource::getTag, MultiConfig::getTag);
            List<String> expected = mode == PriorityMode.NUMBER_OF_MATCHES
                    ? Arrays.asList("time-tag", "number")
                    : Arrays.asList("number", "time-tag");
            for (PriorityFetcher<MultiSource, MultiConfig, Object> fetcher
                    : Arrays.asList(assembler.create(), assembler.create().tree())) {
                assertEquals(Collections.singletonList(expected.get(0)), multiIds(fetcher.match(source).getResult()));
                List<PriorityMatchResult<List<MultiConfig>>> all = fetcher.match(source, true);
                assertEquals(2, all.size());
                assertEquals(expected, Arrays.asList(all.get(0).getResult().get(0).getId(),
                        all.get(1).getResult().get(0).getId()));
                assertEquals(mode == PriorityMode.NUMBER_OF_MATCHES
                                ? Arrays.asList("time_tag", "number") : Arrays.asList("number", "time_tag"),
                        Arrays.asList(all.get(0).getName(), all.get(1).getName()));
                assertEquals(mode == PriorityMode.NUMBER_OF_MATCHES
                                ? Arrays.asList(2, 1) : Arrays.asList(1, 2),
                        Arrays.asList(all.get(0).getLevel(), all.get(1).getLevel()));
            }
        }
    }

    private static void assertAcrossAllModesAndEngines(List<RangeConfig> configs,
                                                        Object sourceValue,
                                                        List<String> expectedIds) {
        for (PriorityMode mode : PriorityMode.values()) {
            PriorityAssembler<RangeSource, RangeConfig, Object> assembler = assembler(configs, mode);
            List<PriorityFetcher<RangeSource, RangeConfig, Object>> fetchers = Arrays.asList(
                    assembler.create(), assembler.create().tree());
            for (PriorityFetcher<RangeSource, RangeConfig, Object> fetcher : fetchers) {
                PriorityMatchResult<List<RangeConfig>> winner = fetcher.match(new RangeSource(sourceValue));
                List<PriorityMatchResult<List<RangeConfig>>> all =
                        fetcher.match(new RangeSource(sourceValue), true);
                if (expectedIds.isEmpty()) {
                    assertNull(winner);
                    assertEquals(Collections.emptyList(), all);
                } else {
                    assertEquals(expectedIds, ids(winner.getResult()));
                    assertEquals(1, all.size());
                    assertEquals(expectedIds, ids(all.get(0).getResult()));
                }
            }
        }
    }

    private static PriorityAssembler<RangeSource, RangeConfig, Object> assembler(
            List<RangeConfig> configs, PriorityMode mode) {
        return PriorityAssembler.from(RangeSource.class, RangeConfig.class, Object.class)
                .initConfig(configs)
                .initPriorityHandler(mode)
                .addPriorityMatchFunction("range", RangeSource::getValue, RangeConfig::getRange,
                        RangeMatchingBoundaryTest::contains);
    }

    private static boolean contains(Object source, Object configuredRange) {
        return configuredRange instanceof Interval && ((Interval<?>) configuredRange).contains(source);
    }

    private static List<String> ids(List<RangeConfig> configs) {
        List<String> ids = new ArrayList<>();
        for (RangeConfig config : configs) {
            ids.add(config.getId());
        }
        return ids;
    }

    private static List<String> multiIds(List<MultiConfig> configs) {
        List<String> ids = new ArrayList<>();
        for (MultiConfig config : configs) {
            ids.add(config.getId());
        }
        return ids;
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }

    private static RangeConfig config(String id, Object range) {
        return new RangeConfig(id, range);
    }

    private static <T extends Comparable<? super T>> Interval<T> interval(
            T lower, boolean lowerInclusive, T upper, boolean upperInclusive) {
        return new Interval<>(lower, lowerInclusive, upper, upperInclusive);
    }

    private static final class RangeSource {
        private final Object value;

        private RangeSource(Object value) {
            this.value = value;
        }

        private Object getValue() {
            return value;
        }
    }

    private static final class RangeConfig {
        private final String id;
        private final Object range;

        private RangeConfig(String id, Object range) {
            this.id = id;
            this.range = range;
        }

        private String getId() {
            return id;
        }

        private Object getRange() {
            return range;
        }
    }

    private static final class MultiSource {
        private final Object number;
        private final Object time;
        private final Object tag;

        private MultiSource(Object number, Object time, Object tag) {
            this.number = number;
            this.time = time;
            this.tag = tag;
        }

        private Object getNumber() {
            return number;
        }

        private Object getTime() {
            return time;
        }

        private Object getTag() {
            return tag;
        }
    }

    private static final class MultiConfig {
        private final String id;
        private final Object number;
        private final Object time;
        private final Object tag;

        private MultiConfig(String id, Object number, Object time, Object tag) {
            this.id = id;
            this.number = number;
            this.time = time;
            this.tag = tag;
        }

        private String getId() {
            return id;
        }

        private Object getNumber() {
            return number;
        }

        private Object getTime() {
            return time;
        }

        private Object getTag() {
            return tag;
        }
    }

    private static final class Interval<T extends Comparable<? super T>> {
        private final T lower;
        private final boolean lowerInclusive;
        private final T upper;
        private final boolean upperInclusive;

        private Interval(T lower, boolean lowerInclusive, T upper, boolean upperInclusive) {
            if (lower == null || upper == null || lower.compareTo(upper) > 0
                    || (lower.compareTo(upper) == 0 && !(lowerInclusive && upperInclusive))) {
                throw new IllegalArgumentException("Interval must contain at least one valid boundary value");
            }
            this.lower = lower;
            this.lowerInclusive = lowerInclusive;
            this.upper = upper;
            this.upperInclusive = upperInclusive;
        }

        @SuppressWarnings("unchecked")
        private boolean contains(Object value) {
            if (value == null) {
                return false;
            }
            final T typedValue;
            try {
                typedValue = (T) value;
            } catch (ClassCastException exception) {
                return false;
            }
            int lowerComparison = typedValue.compareTo(lower);
            int upperComparison = typedValue.compareTo(upper);
            return (lowerComparison > 0 || lowerInclusive && lowerComparison == 0)
                    && (upperComparison < 0 || upperInclusive && upperComparison == 0);
        }
    }

    private static final class SentinelException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
