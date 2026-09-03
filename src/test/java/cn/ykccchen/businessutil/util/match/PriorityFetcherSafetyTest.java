package cn.ykccchen.businessutil.util.match;

import cn.ykccchen.businessutil.match.PriorityAssembler;
import cn.ykccchen.businessutil.match.PriorityFetcher;
import cn.ykccchen.businessutil.match.PriorityMatchFunction;
import cn.ykccchen.businessutil.match.PriorityMatchProcessor;
import cn.ykccchen.businessutil.match.handler.PriorityHandler;
import cn.ykccchen.businessutil.match.handler.PriorityMode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static cn.ykccchen.businessutil.util.match.MatchingFixtures.config;
import static cn.ykccchen.businessutil.util.match.MatchingFixtures.ids;
import static cn.ykccchen.businessutil.util.match.MatchingFixtures.pair;
import static cn.ykccchen.businessutil.util.match.MatchingFixtures.project;
import static cn.ykccchen.businessutil.util.match.MatchingFixtures.source;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriorityFetcherSafetyTest {

    @Test
    void resultListsAreDefensiveCopies() {
        MatchingFixtures.Config config = config("only", "x", null, null, null);
        MatchingFixtures.Source request = source("x", null, null, null);
        for (PriorityFetcher<MatchingFixtures.Source, MatchingFixtures.Config, String> fetcher
                : pair(Collections.singletonList(config), PriorityMode.NUMBER_OF_MATCHES).both()) {
            List<MatchingFixtures.Config> result = fetcher.match(request).getResult();
            result.clear();
            result.add(config("caller-owned", "x", null, null, null));
            assertEquals(Collections.singletonList("only"), ids(fetcher.match(request)));
        }
    }

    @Test
    void processorAndFunctionCollectionsCannotBeMutatedExternally() {
        MatchingFixtures.Config config = config("only", "x", null, null, null);
        PriorityFetcher<MatchingFixtures.Source, MatchingFixtures.Config, String> fetcher =
                pair(Collections.singletonList(config), PriorityMode.NUMBER_OF_MATCHES).level();

        assertThrows(UnsupportedOperationException.class, () -> fetcher.getProcessorList().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> fetcher.getProcessorList().get(0).getPriorityMatchFunctionList().clear());

        List<PriorityMatchFunction<MatchingFixtures.Source, MatchingFixtures.Config, String>> sourceFunctions =
                new ArrayList<>();
        sourceFunctions.add(PriorityMatchFunction.of("A", 0,
                MatchingFixtures.Source::getA, MatchingFixtures.Config::getA));
        PriorityMatchProcessor<MatchingFixtures.Source, MatchingFixtures.Config, String> processor =
                new PriorityMatchProcessor<>(sourceFunctions);
        sourceFunctions.clear();
        assertEquals(1, processor.getFunctionSize());
    }

    @Test
    void invalidAssemblerInputsFailFastWithStableExceptions() {
        PriorityAssembler<MatchingFixtures.Source, MatchingFixtures.Config, String> assembler =
                PriorityAssembler.from(MatchingFixtures.Source.class, MatchingFixtures.Config.class, String.class)
                        .initConfig(Collections.singletonList(config("only", "x", null, null, null)));

        assertThrows(IllegalArgumentException.class, () -> assembler.initConfig(null));
        assertThrows(IllegalArgumentException.class, () -> assembler.initPriorityHandler(null));
        assertThrows(IllegalArgumentException.class, () -> assembler.add(null));
        assertThrows(IllegalArgumentException.class, () -> PriorityMatchFunction.of("negative", -1,
                MatchingFixtures.Source::getA, MatchingFixtures.Config::getA));
        assertThrows(IllegalArgumentException.class, () -> PriorityMatchFunction.of("null-priority", null,
                MatchingFixtures.Source::getA, MatchingFixtures.Config::getA));
        assertThrows(IllegalArgumentException.class, () -> PriorityMatchFunction.of("null-source", 0,
                null, MatchingFixtures.Config::getA));
        assertThrows(IllegalArgumentException.class, () -> PriorityMatchFunction.ofBoolean("null-matcher", 0,
                MatchingFixtures.Source::getA, MatchingFixtures.Config::getA, null));
        assertThrows(IllegalArgumentException.class, () -> assembler.addPriorityMatchFunction("null-matcher",
                MatchingFixtures.Source::getA, MatchingFixtures.Config::getA, null));
        assertThrows(IllegalArgumentException.class, () -> assembler.add(PriorityMatchFunction.of("A", 2,
                MatchingFixtures.Source::getA, MatchingFixtures.Config::getA)));
        assertThrows(IllegalArgumentException.class,
                () -> new PriorityMatchProcessor<>(Collections.singletonList(null)));
    }

    @Test
    void directFetcherFactoryAlsoValidatesPriorityLayout() {
        PriorityMatchFunction<MatchingFixtures.Source, MatchingFixtures.Config, String> bad =
                PriorityMatchFunction.of("A", 1, MatchingFixtures.Source::getA, MatchingFixtures.Config::getA);
        PriorityMatchProcessor<MatchingFixtures.Source, MatchingFixtures.Config, String> processor =
                new PriorityMatchProcessor<>(Collections.singletonList(bad));

        assertThrows(IllegalArgumentException.class, () -> PriorityFetcher.from(
                Collections.singletonList(processor),
                Collections.singletonList(config("only", "x", null, null, null)),
                Collections.singletonList(bad)));

        PriorityMatchFunction<MatchingFixtures.Source, MatchingFixtures.Config, String> registeredA =
                PriorityMatchFunction.of("A", 0, MatchingFixtures.Source::getA, MatchingFixtures.Config::getA);
        PriorityMatchFunction<MatchingFixtures.Source, MatchingFixtures.Config, String> registeredB =
                PriorityMatchFunction.of("B", 1, MatchingFixtures.Source::getB, MatchingFixtures.Config::getB);
        PriorityMatchFunction<MatchingFixtures.Source, MatchingFixtures.Config, String> foreignA =
                PriorityMatchFunction.of("foreign-A", 0,
                        MatchingFixtures.Source::getA, MatchingFixtures.Config::getA);

        assertThrows(IllegalArgumentException.class, () -> PriorityFetcher.from(
                Collections.singletonList(new PriorityMatchProcessor<>(Collections.singletonList(foreignA))),
                Collections.emptyList(), Arrays.asList(registeredA, registeredB)));
        assertThrows(IllegalArgumentException.class, () -> PriorityFetcher.from(
                Collections.singletonList(new PriorityMatchProcessor<>(Arrays.asList(registeredB, registeredA))),
                Collections.emptyList(), Arrays.asList(registeredA, registeredB)));
        assertThrows(IllegalArgumentException.class, () -> PriorityFetcher.from(
                Collections.singletonList(null), Collections.emptyList(),
                Arrays.asList(registeredA, registeredB)));
    }

    @Test
    void customHandlerOutputIsValidatedBeforeFetcherPublication() {
        PriorityHandler invalidHandler = new PriorityHandler() {
            @Override
            public <S, C, K> List<PriorityMatchProcessor<S, C, K>> initPriorityHandlerList(
                    List<PriorityMatchFunction<S, C, K>> functions) {
                return null;
            }
        };
        PriorityAssembler<MatchingFixtures.Source, MatchingFixtures.Config, String> assembler =
                MatchingFixtures.assembler(
                        Collections.singletonList(config("only", "x", null, null, null)),
                        PriorityMode.NUMBER_OF_MATCHES)
                        .initPriorityHandler(invalidHandler);

        assertThrows(IllegalArgumentException.class, assembler::create);
    }

    @Test
    void treeModeUsesVolatilePublicationForConcurrentReaders() throws Exception {
        assertTrue(Modifier.isVolatile(PriorityFetcher.class
                .getDeclaredField("useTreePriority").getModifiers()));
        assertTrue(Modifier.isVolatile(PriorityFetcher.class
                .getDeclaredField("priorityMatchProcessorTree").getModifiers()));
    }

    @Test
    void concurrentReadersObserveStableLevelAndTreeResults() throws Exception {
        List<MatchingFixtures.Config> configs = Arrays.asList(
                config("ABC", "x", "x", "x", null),
                config("AB", "x", "x", null, null),
                config("A", "x", null, null, null));
        MatchingFixtures.Source request = source("x", "x", "x", "x");
        for (PriorityMode mode : PriorityMode.values()) {
            MatchingFixtures.FetcherPair pair = pair(configs, mode);
            List<List<String>> expected = project(pair.level().match(request, true));
            List<String> expectedWinner = ids(pair.level().match(request));
            List<PriorityFetcher<MatchingFixtures.Source, MatchingFixtures.Config, String>> fetchers = pair.both();

            ExecutorService executor = Executors.newFixedThreadPool(8);
            CountDownLatch start = new CountDownLatch(1);
            try {
                List<Callable<List<List<String>>>> tasks = new ArrayList<>();
                for (int taskIndex = 0; taskIndex < 32; taskIndex++) {
                    final PriorityFetcher<MatchingFixtures.Source, MatchingFixtures.Config, String> fetcher =
                            fetchers.get(taskIndex % fetchers.size());
                    tasks.add(() -> {
                        start.await();
                        List<List<String>> last = null;
                        for (int iteration = 0; iteration < 100; iteration++) {
                            if (!expectedWinner.equals(ids(fetcher.match(request)))) {
                                throw new AssertionError("Concurrent winner changed");
                            }
                            last = project(fetcher.match(request, true));
                            if (!expected.equals(last)) {
                                throw new AssertionError("Concurrent result changed: " + last);
                            }
                        }
                        return last;
                    });
                }
                List<Future<List<List<String>>>> futures = new ArrayList<>();
                for (Callable<List<List<String>>> task : tasks) {
                    futures.add(executor.submit(task));
                }
                start.countDown();
                for (Future<List<List<String>>> future : futures) {
                    assertEquals(expected, future.get());
                }
            } finally {
                executor.shutdownNow();
            }
        }
    }
}
