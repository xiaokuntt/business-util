package cn.ykccchen.businessutil.util.match;

import cn.ykccchen.businessutil.match.PriorityAssembler;
import cn.ykccchen.businessutil.match.PriorityFetcher;
import cn.ykccchen.businessutil.match.PriorityMatchResult;
import cn.ykccchen.businessutil.match.handler.PriorityMode;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTimeout;

class PriorityFetcherPerformanceTest {

    private static final int DIMENSION_COUNT = 14;
    private static final int SHAPE_COUNT = (1 << DIMENSION_COUNT) - 1;
    private static final int CONFIG_COUNT = 50_000;
    private static final String MATCHING_KEY = "match";
    private static volatile long blackhole;

    @Test
    void comparesLargeBatchLevelAndTreePerformance() {
        assertTimeout(Duration.ofSeconds(30), this::runBenchmark);
    }

    private void runBenchmark() {
        List<Config> configs = configs();
        PriorityAssembler<Source, Config, String> assembler = assembler(configs);

        long levelBuildStart = System.nanoTime();
        PriorityFetcher<Source, Config, String> level = assembler.create();
        long levelBuildNanos = System.nanoTime() - levelBuildStart;

        long treeBuildStart = System.nanoTime();
        PriorityFetcher<Source, Config, String> tree = assembler.create().tree();
        long treeBuildNanos = System.nanoTime() - treeBuildStart;

        assertEquals(SHAPE_COUNT, level.getProcessorList().size());
        assertEquals(SHAPE_COUNT, tree.getProcessorList().size());

        Source noMatch = source(0);
        Source selectiveMatch = source((1 << 7) - 1);
        Source fullMatch = source(SHAPE_COUNT);

        assertEquivalent(level, tree, noMatch);
        assertEquivalent(level, tree, selectiveMatch);
        assertEquivalent(level, tree, fullMatch);

        warmUp(level, tree, noMatch, 20);
        warmUp(level, tree, selectiveMatch, 10);
        warmUp(level, tree, fullMatch, 2);

        Comparison noMatchStats = measurePair(level, tree, noMatch, 300);
        Comparison selectiveStats = measurePair(level, tree, selectiveMatch, 120);
        Comparison fullMatchStats = measurePair(level, tree, fullMatch, 8);

        String header = String.format(Locale.ROOT,
                "PERF api=match(source,true) configs=%d dimensions=%d shapes=%d "
                        + "level_build_ms_once=%.3f tree_build_ms_once=%.3f",
                CONFIG_COUNT, DIMENSION_COUNT, SHAPE_COUNT,
                millis(levelBuildNanos), millis(treeBuildNanos));
        String noMatchOutput = noMatchStats.format("no-match");
        String selectiveOutput = selectiveStats.format("selective-hit");
        String fullMatchOutput = fullMatchStats.format("full-hit");
        System.out.println(header);
        System.out.println(noMatchOutput);
        System.out.println(selectiveOutput);
        System.out.println(fullMatchOutput);

        assertTrue(selectiveStats.treeMedianNanos < selectiveStats.levelMedianNanos, selectiveOutput);
    }

    private static void assertEquivalent(PriorityFetcher<Source, Config, String> level,
                                         PriorityFetcher<Source, Config, String> tree,
                                         Source source) {
        List<PriorityMatchResult<List<Config>>> levelResults = level.match(source, true);
        List<PriorityMatchResult<List<Config>>> treeResults = tree.match(source, true);
        assertEquals(levelResults.size(), treeResults.size());
        for (int index = 0; index < levelResults.size(); index++) {
            PriorityMatchResult<List<Config>> levelResult = levelResults.get(index);
            PriorityMatchResult<List<Config>> treeResult = treeResults.get(index);
            assertEquals(levelResult.getUniqueId(), treeResult.getUniqueId());
            assertEquals(levelResult.getName(), treeResult.getName());
            assertEquals(levelResult.getNameAndValue(), treeResult.getNameAndValue());
            assertEquals(levelResult.getLevel(), treeResult.getLevel());
            assertEquals(levelResult.getResult(), treeResult.getResult());
        }
    }

    private static void warmUp(PriorityFetcher<Source, Config, String> level,
                               PriorityFetcher<Source, Config, String> tree,
                               Source source,
                               int iterations) {
        for (int iteration = 0; iteration < iterations; iteration++) {
            consume(level.match(source, true));
            consume(tree.match(source, true));
        }
    }

    private static Comparison measurePair(PriorityFetcher<Source, Config, String> level,
                                          PriorityFetcher<Source, Config, String> tree,
                                          Source source,
                                          int iterations) {
        long[] levelSamples = new long[iterations];
        long[] treeSamples = new long[iterations];
        for (int iteration = 0; iteration < iterations; iteration++) {
            if ((iteration & 1) == 0) {
                levelSamples[iteration] = measure(level, source);
                treeSamples[iteration] = measure(tree, source);
            } else {
                treeSamples[iteration] = measure(tree, source);
                levelSamples[iteration] = measure(level, source);
            }
        }
        return new Comparison(levelSamples, treeSamples);
    }

    private static long measure(PriorityFetcher<Source, Config, String> fetcher, Source source) {
        long start = System.nanoTime();
        consume(fetcher.match(source, true));
        return System.nanoTime() - start;
    }

    private static void consume(List<PriorityMatchResult<List<Config>>> results) {
        long value = results.size();
        for (PriorityMatchResult<List<Config>> result : results) {
            value += result.getResult().size();
        }
        blackhole = value;
    }

    private static List<Config> configs() {
        List<Config> configs = new ArrayList<>(CONFIG_COUNT);
        for (int index = 0; index < CONFIG_COUNT; index++) {
            configs.add(new Config(index, index % SHAPE_COUNT + 1));
        }
        return configs;
    }

    private static Source source(int matchingMask) {
        String[] values = new String[DIMENSION_COUNT];
        for (int dimension = 0; dimension < DIMENSION_COUNT; dimension++) {
            values[dimension] = (matchingMask & 1 << dimension) != 0 ? MATCHING_KEY : "missing";
        }
        return new Source(values);
    }

    private static PriorityAssembler<Source, Config, String> assembler(List<Config> configs) {
        PriorityAssembler<Source, Config, String> assembler =
                PriorityAssembler.from(Source.class, Config.class, String.class)
                        .initConfig(configs)
                        .initPriorityHandler(PriorityMode.NUMBER_OF_MATCHES);
        for (int dimension = 0; dimension < DIMENSION_COUNT; dimension++) {
            final int currentDimension = dimension;
            final int bit = 1 << dimension;
            assembler.addPriorityMatchFunction("D" + dimension,
                    source -> source.values[currentDimension],
                    config -> (config.mask & bit) != 0 ? MATCHING_KEY : null);
        }
        return assembler;
    }

    private static double millis(long nanos) {
        return nanos / 1_000_000.0;
    }

    private static final class Comparison {
        private final int iterations;
        private final long levelMedianNanos;
        private final long levelP95Nanos;
        private final long treeMedianNanos;
        private final long treeP95Nanos;

        private Comparison(long[] levelSamples, long[] treeSamples) {
            Arrays.sort(levelSamples);
            Arrays.sort(treeSamples);
            this.iterations = levelSamples.length;
            this.levelMedianNanos = percentile(levelSamples, 0.50);
            this.levelP95Nanos = percentile(levelSamples, 0.95);
            this.treeMedianNanos = percentile(treeSamples, 0.50);
            this.treeP95Nanos = percentile(treeSamples, 0.95);
        }

        private String format(String workload) {
            return String.format(Locale.ROOT,
                    "PERF workload=%s iterations=%d level_median_us=%.3f level_p95_us=%.3f "
                            + "tree_median_us=%.3f tree_p95_us=%.3f tree_speedup=%.2fx",
                    workload, iterations,
                    levelMedianNanos / 1_000.0, levelP95Nanos / 1_000.0,
                    treeMedianNanos / 1_000.0, treeP95Nanos / 1_000.0,
                    (double) levelMedianNanos / treeMedianNanos);
        }

        private static long percentile(long[] sortedSamples, double percentile) {
            int index = (int) Math.ceil(percentile * sortedSamples.length) - 1;
            return sortedSamples[Math.max(0, index)];
        }
    }

    private static final class Source {
        private final String[] values;

        private Source(String[] values) {
            this.values = values;
        }
    }

    private static final class Config {
        private final int id;
        private final int mask;

        private Config(int id, int mask) {
            this.id = id;
            this.mask = mask;
        }
    }
}
