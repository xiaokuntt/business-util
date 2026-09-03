package cn.ykccchen.businessutil.util.match;

import cn.ykccchen.businessutil.match.PriorityAssembler;
import cn.ykccchen.businessutil.match.PriorityFetcher;
import cn.ykccchen.businessutil.match.PriorityMatchFunction;
import cn.ykccchen.businessutil.match.PriorityMatchResult;
import cn.ykccchen.businessutil.match.handler.PriorityMode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PriorityFetcherRegressionTest {

    @Test
    void rt001TreeModePreservesGlobalProcessorPriority() {
        Map<String, String> abcBad = config("ABC-bad", "a", "1", "b", "1", "c", "bad");
        Map<String, String> ab = config("AB", "a", "1", "b", "1");
        Map<String, String> acd = config("ACD", "a", "1", "c", "ok", "d", "1");
        Map<String, String> source = config(null, "a", "1", "b", "1", "c", "ok", "d", "1");

        PriorityAssembler<Map<String, String>, Map<String, String>, String> assembler =
                mapAssembler(Arrays.asList(abcBad, ab, acd));
        addRules(assembler, "a", "b", "c", "d");

        assertEquals("ACD", winner(assembler.create(), source));
        assertEquals("ACD", winner(assembler.create().tree(), source));
    }

    @Test
    void rt002NumberOfMatchesDoesNotOverflowAtTwelveDimensions() {
        Map<String, String> high = config("high");
        Map<String, String> low = config("low");
        Map<String, String> source = config(null);
        for (int i = 1; i <= 12; i++) {
            source.put("p" + i, "x");
        }
        for (int i = 1; i <= 11; i++) {
            high.put("p" + i, "x");
        }
        low.put("p1", "x");
        for (int i = 3; i <= 12; i++) {
            low.put("p" + i, "x");
        }

        PriorityAssembler<Map<String, String>, Map<String, String>, String> assembler =
                mapAssembler(Arrays.asList(high, low));
        for (int i = 1; i <= 12; i++) {
            addRule(assembler, "p" + i);
        }

        assertEquals("high", winner(assembler.create(), source));
        assertEquals("high", winner(assembler.create().tree(), source));
    }

    @Test
    void rt003OverlappingPredicateKeysAreAggregatedPerProcessor() {
        Map<String, String> shortPrefixFirst = config("short-first", "key", "U");
        Map<String, String> longPrefix = config("long", "key", "US");
        Map<String, String> shortPrefixSecond = config("short-second", "key", "U");
        Map<String, String> source = config(null, "key", "USA");
        for (PriorityMode mode : PriorityMode.values()) {
            PriorityAssembler<Map<String, String>, Map<String, String>, String> assembler =
                    mapAssembler(Arrays.asList(shortPrefixFirst, longPrefix, shortPrefixSecond))
                            .initPriorityHandler(mode);
            assembler.addPriorityMatchFunction("key", value("key"), value("key"), String::startsWith);
            for (PriorityFetcher<Map<String, String>, Map<String, String>, String> fetcher
                    : Arrays.asList(assembler.create(), assembler.create().tree())) {
                assertEquals(Arrays.asList("short-first", "short-second", "long"),
                        ids(fetcher.match(source).getResult()));
                assertEquals(1, fetcher.match(source, true).size());
                assertEquals(Arrays.asList("short-first", "short-second", "long"),
                        ids(fetcher.match(source, true).get(0).getResult()));
            }
        }
    }

    @Test
    void rt004MutatingAResultDoesNotMutateTheFetcher() {
        Map<String, String> only = config("only", "key", "x");
        PriorityAssembler<Map<String, String>, Map<String, String>, String> assembler =
                mapAssembler(Collections.singletonList(only));
        addRule(assembler, "key");
        PriorityFetcher<Map<String, String>, Map<String, String>, String> fetcher = assembler.create();
        Map<String, String> source = config(null, "key", "x");

        List<Map<String, String>> firstResult = fetcher.match(source).getResult();
        firstResult.clear();

        assertEquals(Collections.singletonList("only"), ids(fetcher.match(source).getResult()));
    }

    @Test
    void rt005NonDensePriorityFailsWithDomainException() {
        Map<String, String> only = config("only", "key", "x");
        PriorityAssembler<Map<String, String>, Map<String, String>, String> assembler =
                mapAssembler(Collections.singletonList(only));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> assembler.add(PriorityMatchFunction.of("key", 5, value("key"), value("key"))).create());
        assertEquals("Priority must equal its zero-based registration index: expected 0 but was 5",
                exception.getMessage());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static PriorityAssembler<Map<String, String>, Map<String, String>, String> mapAssembler(
            List<Map<String, String>> configs) {
        return PriorityAssembler.from((Class) Map.class, (Class) Map.class, String.class)
                .initConfig(configs)
                .initPriorityHandler(PriorityMode.NUMBER_OF_MATCHES);
    }

    private static void addRules(PriorityAssembler<Map<String, String>, Map<String, String>, String> assembler,
                                 String... keys) {
        for (String key : keys) {
            addRule(assembler, key);
        }
    }

    private static void addRule(PriorityAssembler<Map<String, String>, Map<String, String>, String> assembler,
                                String key) {
        assembler.addPriorityMatchFunction(key, value(key), value(key));
    }

    private static java.util.function.Function<Map<String, String>, String> value(String key) {
        return map -> map.get(key);
    }

    private static String winner(PriorityFetcher<Map<String, String>, Map<String, String>, String> fetcher,
                                 Map<String, String> source) {
        return fetcher.match(source).getResult().get(0).get("id");
    }

    private static List<String> ids(List<Map<String, String>> configs) {
        List<String> ids = new ArrayList<>();
        for (Map<String, String> config : configs) {
            ids.add(config.get("id"));
        }
        return ids;
    }

    private static Map<String, String> config(String id, String... values) {
        Map<String, String> config = new LinkedHashMap<>();
        if (id != null) {
            config.put("id", id);
        }
        for (int i = 0; i < values.length; i += 2) {
            config.put(values[i], values[i + 1]);
        }
        return config;
    }
}
