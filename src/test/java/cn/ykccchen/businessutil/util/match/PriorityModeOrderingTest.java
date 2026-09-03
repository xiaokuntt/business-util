package cn.ykccchen.businessutil.util.match;

import cn.ykccchen.businessutil.match.PriorityAssembler;
import cn.ykccchen.businessutil.match.PriorityFetcher;
import cn.ykccchen.businessutil.match.handler.PriorityMode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.ykccchen.businessutil.util.match.MatchingFixtures.config;
import static cn.ykccchen.businessutil.util.match.MatchingFixtures.ids;
import static cn.ykccchen.businessutil.util.match.MatchingFixtures.pair;
import static cn.ykccchen.businessutil.util.match.MatchingFixtures.project;
import static cn.ykccchen.businessutil.util.match.MatchingFixtures.source;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PriorityModeOrderingTest {

    @Test
    void numberOfMatchesPrefersMoreDimensionsThenEarlierDimensions() {
        List<MatchingFixtures.Config> configs = Arrays.asList(
                config("AB", "x", "x", null, null),
                config("ACD", "x", null, "x", "x"),
                config("ABC", "x", "x", "x", null),
                config("ABD", "x", "x", null, "x"));

        MatchingFixtures.Source request = source("x", "x", "x", "x");
        for (PriorityFetcher<MatchingFixtures.Source, MatchingFixtures.Config, String> fetcher
                : pair(configs, PriorityMode.NUMBER_OF_MATCHES).both()) {
            assertEquals(Arrays.asList("ABC", "ABD", "ACD", "AB"), ids(fetcher.match(request, true)));
        }
    }

    @Test
    void absoluteValueUsesTheFirstDifferingDimensionAsTheTieBreaker() {
        List<MatchingFixtures.Config> configs = Arrays.asList(
                config("ACD", "x", null, "x", "x"),
                config("AB", "x", "x", null, null),
                config("ABD", "x", "x", null, "x"),
                config("ABC", "x", "x", "x", null));

        MatchingFixtures.Source request = source("x", "x", "x", "x");
        for (PriorityFetcher<MatchingFixtures.Source, MatchingFixtures.Config, String> fetcher
                : pair(configs, PriorityMode.ABSOLUTE_VALUE).both()) {
            assertEquals(Arrays.asList("ABC", "ABD", "AB", "ACD"), ids(fetcher.match(request, true)));
        }
    }

    @Test
    void defaultModeMatchesExplicitNumberOfMatches() {
        List<MatchingFixtures.Config> configs = Arrays.asList(
                config("AB", "x", "x", null, null),
                config("ACD", "x", null, "x", "x"));
        MatchingFixtures.Source request = source("x", "x", "x", "x");

        PriorityAssembler<MatchingFixtures.Source, MatchingFixtures.Config, String> defaultAssembler =
                PriorityAssembler.from(MatchingFixtures.Source.class, MatchingFixtures.Config.class, String.class)
                        .initConfig(configs)
                        .addPriorityMatchFunction("A", MatchingFixtures.Source::getA, MatchingFixtures.Config::getA)
                        .addPriorityMatchFunction("B", MatchingFixtures.Source::getB, MatchingFixtures.Config::getB)
                        .addPriorityMatchFunction("C", MatchingFixtures.Source::getC, MatchingFixtures.Config::getC)
                        .addPriorityMatchFunction("D", MatchingFixtures.Source::getD, MatchingFixtures.Config::getD);

        assertEquals(project(MatchingFixtures.assembler(configs, PriorityMode.NUMBER_OF_MATCHES)
                        .create().match(request, true)),
                project(defaultAssembler.create().match(request, true)));
    }

    @Test
    void twelveDimensionsAreComparedWithoutNumericEncoding() {
        Map<String, String> high = record("high");
        Map<String, String> low = record("low");
        Map<String, String> request = record(null);
        for (int i = 1; i <= 12; i++) {
            request.put("p" + i, "x");
        }
        for (int i = 1; i <= 11; i++) {
            high.put("p" + i, "x");
        }
        low.put("p1", "x");
        for (int i = 3; i <= 12; i++) {
            low.put("p" + i, "x");
        }

        PriorityAssembler<Map<String, String>, Map<String, String>, String> assembler =
                mapAssembler(Arrays.asList(high, low), PriorityMode.NUMBER_OF_MATCHES);
        addMapRules(assembler, 12);

        assertEquals("high", assembler.create().match(request).getResult().get(0).get("id"));
        assertEquals("high", assembler.create().tree().match(request).getResult().get(0).get("id"));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static PriorityAssembler<Map<String, String>, Map<String, String>, String> mapAssembler(
            List<Map<String, String>> configs, PriorityMode mode) {
        return PriorityAssembler.from((Class) Map.class, (Class) Map.class, String.class)
                .initConfig(configs)
                .initPriorityHandler(mode);
    }

    private static void addMapRules(PriorityAssembler<Map<String, String>, Map<String, String>, String> assembler,
                                    int count) {
        for (int i = 1; i <= count; i++) {
            final String key = "p" + i;
            assembler.addPriorityMatchFunction(key, map -> map.get(key), map -> map.get(key));
        }
    }

    private static Map<String, String> record(String id) {
        Map<String, String> result = new LinkedHashMap<>();
        if (id != null) {
            result.put("id", id);
        }
        return result;
    }
}
