package cn.ykccchen.businessutil.util.match;

import cn.ykccchen.businessutil.match.PriorityFetcher;
import cn.ykccchen.businessutil.match.PriorityMatchResult;
import cn.ykccchen.businessutil.match.handler.PriorityMode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static cn.ykccchen.businessutil.util.match.MatchingFixtures.config;
import static cn.ykccchen.businessutil.util.match.MatchingFixtures.ids;
import static cn.ykccchen.businessutil.util.match.MatchingFixtures.pair;
import static cn.ykccchen.businessutil.util.match.MatchingFixtures.project;
import static cn.ykccchen.businessutil.util.match.MatchingFixtures.source;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PriorityFetcherContractTest {

    @Nested
    class ExactMatching {

        @Test
        void returnsTheMostSpecificMatchAndFallsBackByConfiguredShape() {
            List<MatchingFixtures.Config> configs = Arrays.asList(
                    config("ABCD", "a", "b", "c", "d"),
                    config("ABC", "a", "b", "c", null),
                    config("A", "a", null, null, null));

            for (PriorityMode mode : PriorityMode.values()) {
                MatchingFixtures.FetcherPair pair = pair(configs, mode);
                for (PriorityFetcher<MatchingFixtures.Source, MatchingFixtures.Config, String> fetcher : pair.both()) {
                    assertEquals(Collections.singletonList("ABCD"),
                            ids(fetcher.match(source("a", "b", "c", "d"))));
                    assertEquals(Collections.singletonList("ABC"),
                            ids(fetcher.match(source("a", "b", "c", "other"))));
                    assertEquals(Collections.singletonList("A"),
                            ids(fetcher.match(source("a", "other", "other", "other"))));
                    assertNull(fetcher.match(source("other", "b", "c", "d")));
                }
            }
        }

        @Test
        void aggregatesDuplicateConfigsInInputOrder() {
            List<MatchingFixtures.Config> configs = Arrays.asList(
                    config("first", "a", "b", null, null),
                    config("second", "a", "b", null, null));

            for (PriorityMode mode : PriorityMode.values()) {
                for (PriorityFetcher<MatchingFixtures.Source, MatchingFixtures.Config, String> fetcher
                        : pair(configs, mode).both()) {
                    assertEquals(Arrays.asList("first", "second"),
                            ids(fetcher.match(source("a", "b", null, null))));
                    assertEquals(1, fetcher.match(source("a", "b", null, null), true).size());
                }
            }
        }
    }

    @Nested
    class MissingValues {

        @Test
        void treatsNullAndEmptyStringsAsMissingDimensions() {
            List<MatchingFixtures.Config> configs = Arrays.asList(
                    config("AB", "a", "b", null, null),
                    config("A", "a", null, null, null),
                    config("ignored-empty", "", null, null, null));

            for (PriorityMode mode : PriorityMode.values()) {
                for (PriorityFetcher<MatchingFixtures.Source, MatchingFixtures.Config, String> fetcher
                        : pair(configs, mode).both()) {
                    assertEquals(Collections.singletonList("A"),
                            ids(fetcher.match(source("a", "", null, null))));
                    assertNull(fetcher.match(null));
                    assertEquals(Collections.emptyList(), fetcher.match(null, true));
                }
            }
        }

        @Test
        void nullConfigElementsAreIgnoredAndWhitespaceRemainsAnExactKey() {
            List<MatchingFixtures.Config> configs = Arrays.asList(
                    null,
                    config("ignored-null", null, null, null, null),
                    config("ignored-empty", "", null, null, null),
                    config("null-A-with-B", null, "b", null, null),
                    config("empty-A-with-B", "", "b", null, null),
                    config("space", " ", null, null, null),
                    config("tab", "\t", null, null, null),
                    config("valid", "x", null, null, null),
                    null);

            for (PriorityMode mode : PriorityMode.values()) {
                for (PriorityFetcher<MatchingFixtures.Source, MatchingFixtures.Config, String> fetcher
                        : pair(configs, mode).both()) {
                    assertEquals(2, fetcher.getProcessorList().size());
                    assertNull(fetcher.match(source(null, null, null, null)));
                    assertNull(fetcher.match(source("", null, null, null)));
                    assertEquals(Collections.singletonList("space"),
                            ids(fetcher.match(source(" ", null, null, null))));
                    assertEquals(Collections.singletonList("space"),
                            ids(fetcher.match(source(" ", null, null, null), true)));
                    assertEquals(Collections.singletonList("tab"),
                            ids(fetcher.match(source("\t", null, null, null))));
                    assertEquals(Collections.singletonList("tab"),
                            ids(fetcher.match(source("\t", null, null, null), true)));
                    assertEquals(Collections.singletonList("valid"),
                            ids(fetcher.match(source("x", null, null, null))));
                    PriorityMatchResult<List<MatchingFixtures.Config>> bOnly =
                            fetcher.match(source("not-a", "b", null, null));
                    assertEquals("B", bOnly.getName());
                    assertEquals(1, bOnly.getLevel());
                    assertEquals(Arrays.asList("null-A-with-B", "empty-A-with-B"), ids(bOnly));
                    List<PriorityMatchResult<List<MatchingFixtures.Config>>> allBOnly =
                            fetcher.match(source("not-a", "b", null, null), true);
                    assertEquals(1, allBOnly.size());
                    assertEquals("B", allBOnly.get(0).getName());
                    assertEquals(1, allBOnly.get(0).getLevel());
                    assertEquals(Arrays.asList("null-A-with-B", "empty-A-with-B"),
                            ids(allBOnly.get(0)));
                }

                PriorityFetcher<MatchingFixtures.Source, MatchingFixtures.Config, String> onlyNulls =
                        MatchingFixtures.assembler(Arrays.asList(null, null), mode).create();
                assertEquals(0, onlyNulls.getProcessorList().size());
                assertNull(onlyNulls.match(source("x", null, null, null)));
                assertEquals(Collections.emptyList(), onlyNulls.match(source("x", null, null, null), true));
            }
        }

        @Test
        void emptyConfigurationsAndEmptyRulesReturnNoMatch() {
            PriorityFetcher<MatchingFixtures.Source, MatchingFixtures.Config, String> emptyConfigFetcher =
                    MatchingFixtures.assembler(Collections.emptyList(), PriorityMode.NUMBER_OF_MATCHES).create();
            assertNull(emptyConfigFetcher.match(source("a", "b", "c", "d")));
            assertEquals(Collections.emptyList(), emptyConfigFetcher.match(source("a", "b", "c", "d"), true));

            PriorityFetcher<MatchingFixtures.Source, MatchingFixtures.Config, String> emptyRuleFetcher =
                    cn.ykccchen.businessutil.match.PriorityAssembler
                            .from(MatchingFixtures.Source.class, MatchingFixtures.Config.class, String.class)
                            .initConfig(Collections.singletonList(config("ignored", "a", null, null, null)))
                            .create();
            assertNull(emptyRuleFetcher.match(source("a", null, null, null)));
            assertEquals(0, emptyRuleFetcher.getProcessorList().size());
        }
    }

    @Test
    void levelAndTreeProduceStructurallyIdenticalResults() {
        List<MatchingFixtures.Config> configs = Arrays.asList(
                config("ABC-bad", "1", "1", "bad", null),
                config("AB", "1", "1", null, null),
                config("ACD", "1", null, "ok", "1"),
                config("A", "1", null, null, null));

        for (PriorityMode mode : PriorityMode.values()) {
            MatchingFixtures.FetcherPair pair = pair(configs, mode);
            MatchingFixtures.Source request = source("1", "1", "ok", "1");
            PriorityMatchResult<List<MatchingFixtures.Config>> levelWinner = pair.level().match(request);
            PriorityMatchResult<List<MatchingFixtures.Config>> treeWinner = pair.tree().match(request);
            assertEquals(ids(levelWinner), ids(treeWinner));
            assertEquals(project(pair.level().match(request, true)), project(pair.tree().match(request, true)));
        }
    }
}
