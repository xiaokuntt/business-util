package cn.ykccchen.businessutil.util.match;

import cn.ykccchen.businessutil.match.DuplicateKeyCheckLevel;
import cn.ykccchen.businessutil.match.PriorityAssembler;
import cn.ykccchen.businessutil.match.PriorityFetcher;
import cn.ykccchen.businessutil.match.PriorityMatchFunction;
import cn.ykccchen.businessutil.match.PriorityMatchProcessor;
import cn.ykccchen.businessutil.match.PriorityMatchResult;
import cn.ykccchen.businessutil.match.PriorityMatcher;
import cn.ykccchen.businessutil.match.PriorityMatchers;
import cn.ykccchen.businessutil.match.PriorityRange;
import cn.ykccchen.businessutil.match.handler.PriorityMode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuiltInPriorityMatchersTest {

    @Test
    void equalityAndComparableMatchersCoverBothSidesAndMissingValues() {
        assertTrue(PriorityMatchers.<String>equal().matches("A", "A"));
        assertFalse(PriorityMatchers.<String>equal().matches("A", "B"));
        assertFalse(PriorityMatchers.<String>equal().matches(null, null));
        assertFalse(PriorityMatchers.<String>equal().matches("", ""));

        assertTrue(PriorityMatchers.<String>notEqual().matches("A", "B"));
        assertFalse(PriorityMatchers.<String>notEqual().matches("A", "A"));
        assertFalse(PriorityMatchers.<String>notEqual().matches(null, "A"));
        assertFalse(PriorityMatchers.<String>notEqual().matches("A", ""));

        ArrayList<String> mutableConfigured = new ArrayList<>(Arrays.asList("A", null, ""));
        PriorityMatcher<ArrayList<String>, ArrayList<String>> collectionNotEqual =
                PriorityMatchers.notEqual();
        assertSame(mutableConfigured,
                collectionNotEqual.prepareConfigValue(mutableConfigured));

        assertTrue(PriorityMatchers.stringNotEqualsIgnoreCase().matches("A", "B"));
        assertFalse(PriorityMatchers.stringNotEqualsIgnoreCase().matches("A", "a"));
        assertFalse(PriorityMatchers.stringNotEqualsIgnoreCase().matches("", "a"));

        assertTrue(PriorityMatchers.<Integer>greaterThan().matches(11, 10));
        assertFalse(PriorityMatchers.<Integer>greaterThan().matches(10, 10));
        assertFalse(PriorityMatchers.<Integer>greaterThan().matches(9, 10));
        assertFalse(PriorityMatchers.<Integer>greaterThan().matches(null, 10));
        assertTrue(PriorityMatchers.<Integer>greaterThanOrEqual().matches(10, 10));
        assertTrue(PriorityMatchers.<Integer>greaterThanOrEqual().matches(11, 10));
        assertFalse(PriorityMatchers.<Integer>greaterThanOrEqual().matches(9, 10));
        assertFalse(PriorityMatchers.<Integer>greaterThanOrEqual().matches(10, null));
        assertTrue(PriorityMatchers.<Integer>lessThan().matches(9, 10));
        assertFalse(PriorityMatchers.<Integer>lessThan().matches(10, 10));
        assertFalse(PriorityMatchers.<Integer>lessThan().matches(11, 10));
        assertFalse(PriorityMatchers.<Integer>lessThan().matches(null, 10));
        assertTrue(PriorityMatchers.<Integer>lessThanOrEqual().matches(10, 10));
        assertTrue(PriorityMatchers.<Integer>lessThanOrEqual().matches(9, 10));
        assertFalse(PriorityMatchers.<Integer>lessThanOrEqual().matches(11, 10));
        assertFalse(PriorityMatchers.<Integer>lessThanOrEqual().matches(10, null));

        assertFalse(PriorityMatchers.<String>greaterThan().matches("", "a"));
        assertTrue(PriorityMatchers.<String>greaterThan().matches("b", " "));
        assertFalse(PriorityMatchers.<Double>greaterThan().matches(Double.NaN, 1D));
        assertFalse(PriorityMatchers.<Double>lessThan().matches(1D, Double.POSITIVE_INFINITY));
    }

    @Test
    void stringMatchersCoverCaseWhitespaceAndMissingValues() {
        assertTrue(PriorityMatchers.stringStartsWith().matches("BusinessUtil", "Business"));
        assertTrue(PriorityMatchers.stringStartsWithIgnoreCase().matches("BusinessUtil", "business"));
        assertTrue(PriorityMatchers.stringNotStartsWith().matches("BusinessUtil", "Other"));
        assertTrue(PriorityMatchers.stringNotStartsWithIgnoreCase().matches("BusinessUtil", "OTHER"));
        assertTrue(PriorityMatchers.stringEndsWith().matches("BusinessUtil", "Util"));
        assertTrue(PriorityMatchers.stringEndsWithIgnoreCase().matches("BusinessUtil", "util"));
        assertTrue(PriorityMatchers.stringNotEndsWith().matches("BusinessUtil", "Other"));
        assertTrue(PriorityMatchers.stringNotEndsWithIgnoreCase().matches("BusinessUtil", "OTHER"));
        assertTrue(PriorityMatchers.stringContains().matches("BusinessUtil", "ness"));
        assertTrue(PriorityMatchers.stringContainsIgnoreCase().matches("BusinessUtil", "NESS"));
        assertTrue(PriorityMatchers.stringNotContains().matches("BusinessUtil", "other"));
        assertTrue(PriorityMatchers.stringNotContainsIgnoreCase().matches("BusinessUtil", "OTHER"));
        assertTrue(PriorityMatchers.stringEqualsIgnoreCase().matches("TITLE", "title"));
        assertTrue(PriorityMatchers.stringContains().matches("a b", " "));

        assertFalse(PriorityMatchers.stringStartsWith().matches("", ""));
        assertFalse(PriorityMatchers.stringEndsWith().matches("value", ""));
        assertFalse(PriorityMatchers.stringContains().matches(null, "a"));
        assertFalse(PriorityMatchers.stringNotContains().matches("value", null));
        assertFalse(PriorityMatchers.stringNotContains().matches("", "x"));
        assertFalse(PriorityMatchers.stringNotStartsWith().matches(null, "x"));
        assertFalse(PriorityMatchers.stringNotEndsWith().matches("value", ""));

        assertFalse(PriorityMatchers.stringEqualsIgnoreCase().matches("TITLE", "other"));
        assertFalse(PriorityMatchers.stringNotEqualsIgnoreCase().matches("TITLE", "title"));
        assertFalse(PriorityMatchers.stringStartsWithIgnoreCase().matches("TITLE", "other"));
        assertFalse(PriorityMatchers.stringNotStartsWithIgnoreCase().matches("TITLE", "ti"));
        assertFalse(PriorityMatchers.stringEndsWithIgnoreCase().matches("TITLE", "other"));
        assertFalse(PriorityMatchers.stringNotEndsWithIgnoreCase().matches("TITLE", "tle"));
        assertFalse(PriorityMatchers.stringContainsIgnoreCase().matches("TITLE", "other"));
        assertFalse(PriorityMatchers.stringNotContainsIgnoreCase().matches("TITLE", "itl"));

        assertMissingStringOperandsNeverMatch(PriorityMatchers.stringEqualsIgnoreCase());
        assertMissingStringOperandsNeverMatch(PriorityMatchers.stringNotEqualsIgnoreCase());
        assertMissingStringOperandsNeverMatch(PriorityMatchers.stringStartsWith());
        assertMissingStringOperandsNeverMatch(PriorityMatchers.stringStartsWithIgnoreCase());
        assertMissingStringOperandsNeverMatch(PriorityMatchers.stringNotStartsWith());
        assertMissingStringOperandsNeverMatch(PriorityMatchers.stringNotStartsWithIgnoreCase());
        assertMissingStringOperandsNeverMatch(PriorityMatchers.stringEndsWith());
        assertMissingStringOperandsNeverMatch(PriorityMatchers.stringEndsWithIgnoreCase());
        assertMissingStringOperandsNeverMatch(PriorityMatchers.stringNotEndsWith());
        assertMissingStringOperandsNeverMatch(PriorityMatchers.stringNotEndsWithIgnoreCase());
        assertMissingStringOperandsNeverMatch(PriorityMatchers.stringContains());
        assertMissingStringOperandsNeverMatch(PriorityMatchers.stringContainsIgnoreCase());
        assertMissingStringOperandsNeverMatch(PriorityMatchers.stringNotContains());
        assertMissingStringOperandsNeverMatch(PriorityMatchers.stringNotContainsIgnoreCase());
    }

    @Test
    void ignoreCaseMatchersAreIndependentOfDefaultLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            assertTrue(PriorityMatchers.stringEqualsIgnoreCase().matches("TITLE", "title"));
            assertTrue(PriorityMatchers.stringStartsWithIgnoreCase().matches("TITLE", "ti"));
            assertTrue(PriorityMatchers.stringContainsIgnoreCase().matches("TITLE", "itl"));
            assertTrue(PriorityMatchers.stringEndsWithIgnoreCase().matches("TITLE", "tle"));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void regexUsesCompiledConfigurationPatterns() {
        PriorityMatcher<CharSequence, Pattern> matcher = PriorityMatchers.stringMatchesRegex();
        assertTrue(matcher.matches("order-123", Pattern.compile("order-\\d+")));
        assertFalse(matcher.matches("order-x", Pattern.compile("order-\\d+")));
        assertTrue(PriorityMatchers.stringNotMatchesRegex()
                .matches("order-x", Pattern.compile("order-\\d+")));
        assertFalse(PriorityMatchers.stringNotMatchesRegex()
                .matches("order-123", Pattern.compile("order-\\d+")));
        assertFalse(matcher.matches("", Pattern.compile(".*")));
        assertFalse(matcher.matches("anything", Pattern.compile("")));
        assertFalse(matcher.matches(null, Pattern.compile(".*")));
        assertFalse(matcher.matches("anything", null));
        assertFalse(PriorityMatchers.stringNotMatchesRegex()
                .matches("", Pattern.compile(".*")));
        assertFalse(PriorityMatchers.stringNotMatchesRegex()
                .matches("anything", Pattern.compile("")));
        assertFalse(PriorityMatchers.stringNotMatchesRegex()
                .matches(null, Pattern.compile(".*")));
        assertFalse(PriorityMatchers.stringNotMatchesRegex()
                .matches("anything", null));
    }

    @Test
    void elementAndCollectionElementMatchersAreSymmetric() {
        List<String> configured = Arrays.asList("A", "B");
        assertTrue(PriorityMatchers.<String>elementInCollection().matches("A", configured));
        assertFalse(PriorityMatchers.<String>elementInCollection().matches("C", configured));
        assertTrue(PriorityMatchers.<String>elementNotInCollection().matches("C", configured));
        assertFalse(PriorityMatchers.<String>elementNotInCollection().matches("A", configured));
        assertFalse(PriorityMatchers.<String>elementNotInCollection().matches(null, configured));
        assertTrue(PriorityMatchers.<String>elementNotInCollection()
                .matches("A", Collections.emptyList()));
        assertFalse(PriorityMatchers.<String>elementInCollection().matches("A", null));
        assertFalse(PriorityMatchers.<String>elementNotInCollection().matches("A", null));

        assertTrue(PriorityMatchers.<String>collectionContainsElement().matches(configured, "A"));
        assertFalse(PriorityMatchers.<String>collectionContainsElement().matches(configured, "C"));
        assertTrue(PriorityMatchers.<String>collectionNotContainsElement().matches(configured, "C"));
        assertFalse(PriorityMatchers.<String>collectionNotContainsElement().matches(configured, "A"));
        assertFalse(PriorityMatchers.<String>collectionNotContainsElement().matches(configured, null));
        assertTrue(PriorityMatchers.<String>collectionNotContainsElement()
                .matches(Collections.emptyList(), "A"));
        assertFalse(PriorityMatchers.<String>collectionContainsElement().matches(null, "A"));
        assertFalse(PriorityMatchers.<String>collectionNotContainsElement().matches(null, "A"));
        assertFalse(PriorityMatchers.<String>collectionContainsElement().matches(configured, ""));
        assertFalse(PriorityMatchers.<String>collectionNotContainsElement().matches(configured, ""));
    }

    @Test
    void collectionSetOperationsFollowMathematicalEmptySemantics() {
        Collection<String> ab = Arrays.asList("A", "B");
        Collection<String> bc = Arrays.asList("B", "C");
        Collection<String> abc = Arrays.asList("A", "B", "C");
        Collection<String> empty = Collections.emptyList();

        assertTrue(PriorityMatchers.<String>collectionIntersects().matches(ab, bc));
        assertFalse(PriorityMatchers.<String>collectionIntersects().matches(ab,
                Collections.singletonList("C")));
        assertFalse(PriorityMatchers.<String>collectionIntersects().matches(ab, empty));

        assertTrue(PriorityMatchers.<String>collectionContainsAll().matches(abc, ab));
        assertFalse(PriorityMatchers.<String>collectionContainsAll().matches(ab, abc));
        assertTrue(PriorityMatchers.<String>collectionContainsAll().matches(ab, empty));

        assertTrue(PriorityMatchers.<String>collectionContainedBy().matches(ab, abc));
        assertFalse(PriorityMatchers.<String>collectionContainedBy().matches(abc, ab));
        assertTrue(PriorityMatchers.<String>collectionContainedBy().matches(empty, ab));

        assertTrue(PriorityMatchers.<String>collectionDisjoint().matches(ab,
                Collections.singletonList("C")));
        assertFalse(PriorityMatchers.<String>collectionDisjoint().matches(ab, bc));
        assertTrue(PriorityMatchers.<String>collectionDisjoint().matches(ab, empty));

        assertFalse(PriorityMatchers.<String>collectionIntersects().matches(null, ab));
        assertFalse(PriorityMatchers.<String>collectionIntersects().matches(ab, null));
        assertFalse(PriorityMatchers.<String>collectionContainsAll().matches(null, empty));
        assertFalse(PriorityMatchers.<String>collectionContainsAll().matches(ab, null));
        assertFalse(PriorityMatchers.<String>collectionContainedBy().matches(null, ab));
        assertFalse(PriorityMatchers.<String>collectionContainedBy().matches(ab, null));
        assertFalse(PriorityMatchers.<String>collectionDisjoint().matches(null, ab));
        assertFalse(PriorityMatchers.<String>collectionDisjoint().matches(ab, null));
    }

    @Test
    void missingCollectionMembersAreIgnoredButWhitespaceIsSignificant() {
        Collection<String> configured = Arrays.asList(null, "", " ", "A");
        assertFalse(PriorityMatchers.<String>elementInCollection().matches("", configured));
        assertTrue(PriorityMatchers.<String>elementInCollection().matches(" ", configured));
        assertTrue(PriorityMatchers.<String>collectionIntersects()
                .matches(Arrays.asList(null, "", " "), configured));
        assertFalse(PriorityMatchers.<String>collectionIntersects()
                .matches(Arrays.asList(null, ""), Arrays.asList(null, "")));
    }

    @Test
    void configuredCollectionsAreSnapshottedWithoutMutatingTheCaller() {
        List<String> mutableList = new ArrayList<>(Arrays.asList("A", null, "", "B"));
        PriorityMatcher<String, Collection<String>> membership = PriorityMatchers.elementInCollection();
        Collection<String> preparedList = membership.prepareConfigValue(mutableList);
        assertEquals(Arrays.asList("A", "B"), new ArrayList<>(preparedList));
        assertEquals(Arrays.asList("A", null, "", "B"), mutableList);
        assertThrows(UnsupportedOperationException.class, () -> preparedList.add("C"));
        mutableList.clear();
        assertEquals(Arrays.asList("A", "B"), new ArrayList<>(preparedList));

        Set<String> mutableSet = new LinkedHashSet<>(Arrays.asList("A", null, "", "B"));
        Collection<String> preparedSet = membership.prepareConfigValue(mutableSet);
        assertTrue(preparedSet instanceof Set);
        assertEquals(new LinkedHashSet<>(Arrays.asList("A", "B")), preparedSet);
        assertThrows(UnsupportedOperationException.class, () -> preparedSet.add("C"));
    }

    @Test
    void exactMatcherAlsoSnapshotsCollectionConfigurationKeys() {
        List<String> configured = new ArrayList<>(Arrays.asList("A", null, "", "B"));
        ExactCollectionRule rule = new ExactCollectionRule(configured);
        PriorityFetcher<ExactCollectionRequest, ExactCollectionRule, Collection<String>> fetcher =
                PriorityAssembler.from(
                                new PriorityAssembler.TypeReference<ExactCollectionRequest>() { },
                                new PriorityAssembler.TypeReference<ExactCollectionRule>() { },
                                new PriorityAssembler.TypeReference<Collection<String>>() { })
                        .initConfig(Collections.singletonList(rule))
                        .addPriorityMatcher("tags", ExactCollectionRequest::getTags,
                                ExactCollectionRule::getTags, PriorityMatchers.equal())
                        .create();
        configured.clear();

        assertSame(rule, fetcher.match(new ExactCollectionRequest(Arrays.asList("A", null, "", "B")))
                .getResult().get(0));
        assertEquals("tags:[A, null, , B]", fetcher.match(
                new ExactCollectionRequest(Arrays.asList("A", null, "", "B"))).getNameAndValue());
    }

    @Test
    void notEqualMatcherSnapshotsCollectionInsideTheFetcherIndex() {
        List<String> configured = new ArrayList<>(Arrays.asList("A", null, ""));
        ExactCollectionRule rule = new ExactCollectionRule(configured);
        PriorityFetcher<ExactCollectionRequest, ExactCollectionRule, Object> fetcher =
                PriorityAssembler.from(ExactCollectionRequest.class,
                                ExactCollectionRule.class, Object.class)
                        .initConfig(Collections.singletonList(rule))
                        .addPriorityMatcher("tags", ExactCollectionRequest::getTags,
                                ExactCollectionRule::getTags,
                                PriorityMatchers.<Collection<String>>notEqual())
                        .create();
        configured.clear();

        assertNull(fetcher.match(new ExactCollectionRequest(
                Arrays.asList("A", null, ""))));
        assertSame(rule, fetcher.match(new ExactCollectionRequest(
                Collections.singletonList("B"))).getResult().get(0));
    }

    @Test
    void concreteCollectionTypesNeverReceiveInternalIndexWrappers() {
        ArrayList<String> configured = new ArrayList<>(Collections.singletonList("A"));
        ConcreteListRule rule = new ConcreteListRule(configured);
        PriorityMatchFunction<ConcreteListRequest, ConcreteListRule, ArrayList<String>> function =
                PriorityMatchFunction.ofMatcher("tags", 0,
                        ConcreteListRequest::getTags, ConcreteListRule::getTags,
                        PriorityMatchers.equal());

        assertSame(configured, function.matchConfig(rule));
        assertSame(configured, function.getConfigGetter().apply(rule));

        PriorityFetcher<ConcreteListRequest, ConcreteListRule, ArrayList<String>> fetcher =
                PriorityAssembler.from(ConcreteListRequest.class,
                                ConcreteListRule.class, castArrayListClass())
                        .initConfig(Collections.singletonList(rule))
                        .add(function)
                        .initPriorityNameAndValueHandler(
                                (name, priority, source, config, sourceValue, configValue) ->
                                        name + ":" + configValue.get(0))
                        .create();

        assertEquals("tags:A", fetcher.match(new ConcreteListRequest(
                new ArrayList<>(Collections.singletonList("A")))).getNameAndValue());
    }

    @Test
    void equalityMatchersRejectCollectionsWithoutListOrSetEqualitySemantics() {
        Collection<String> queue = new ArrayDeque<>(Collections.singletonList("A"));
        ExactCollectionRule rule = new ExactCollectionRule(queue);

        IllegalArgumentException equalFailure = assertThrows(IllegalArgumentException.class,
                () -> PriorityAssembler.from(ExactCollectionRequest.class,
                                ExactCollectionRule.class, Object.class)
                        .initConfig(Collections.singletonList(rule))
                        .addPriorityMatcher("tags", ExactCollectionRequest::getTags,
                                ExactCollectionRule::getTags,
                                PriorityMatchers.<Collection<String>>equal())
                        .create());
        assertTrue(equalFailure.getMessage().contains("List or Set"));

        assertThrows(IllegalArgumentException.class,
                () -> PriorityAssembler.from(ExactCollectionRequest.class,
                                ExactCollectionRule.class, Object.class)
                        .initConfig(Collections.singletonList(rule))
                        .addPriorityMatcher("tags", ExactCollectionRequest::getTags,
                                ExactCollectionRule::getTags,
                                PriorityMatchers.<Collection<String>>notEqual())
                        .create());
    }

    @Test
    void heterogeneousBuiltInsIntegrateAcrossModesAndEngines() {
        List<Rule> rules = Arrays.asList(
                new Rule("full", PriorityRange.closed(decimal("10"), decimal("20")),
                        "api", Arrays.asList("VIP", "STAFF")),
                new Rule("amount-prefix", PriorityRange.closed(decimal("10"), decimal("20")),
                        "api", null),
                new Rule("role", null, null, Collections.singletonList("VIP")));
        Request request = new Request(decimal("15"), "api/v1", "VIP");

        for (PriorityMode mode : PriorityMode.values()) {
            PriorityFetcher<Request, Rule, Object> level = assembler(rules, mode).create();
            PriorityFetcher<Request, Rule, Object> tree = assembler(rules, mode).create().tree();

            assertEquals(project(level.match(request, true)), project(tree.match(request, true)));
            assertEquals(Arrays.asList("full", "amount-prefix", "role"),
                    ids(level.match(request, true)));
            assertEquals("amount:[10,20]_path:api_roles:[VIP, STAFF]",
                    level.match(request).getNameAndValue());
        }
    }

    @Test
    void mutableConfiguredCollectionCannotCorruptTheBuiltIndex() {
        List<String> roles = new ArrayList<>(Collections.singletonList("VIP"));
        Rule rule = new Rule("role", null, null, roles);
        PriorityFetcher<Request, Rule, Object> fetcher =
                assembler(Collections.singletonList(rule), PriorityMode.NUMBER_OF_MATCHES).create();
        roles.clear();
        roles.add("OTHER");

        PriorityMatchResult<List<Rule>> result =
                fetcher.match(new Request(null, null, "VIP"));
        assertEquals(Collections.singletonList("role"), ids(Collections.singletonList(result)));
        assertEquals("roles:[VIP]", result.getNameAndValue());
    }

    @Test
    void preparedCollectionsParticipateInDuplicateChecking() {
        List<String> firstRoles = new ArrayList<>(Arrays.asList("VIP", null, ""));
        List<String> secondRoles = new ArrayList<>(Collections.singletonList("VIP"));
        PriorityFetcher<Request, Rule, Object> fetcher = assembler(Arrays.asList(
                new Rule("first", null, null, firstRoles),
                new Rule("second", null, null, secondRoles)), PriorityMode.NUMBER_OF_MATCHES)
                .initDuplicateKeyCheck(DuplicateKeyCheckLevel.WARNING)
                .create();

        assertEquals(1, fetcher.getDuplicateKeyCheckReport().getDuplicateGroupCount());
        assertEquals(2, fetcher.getDuplicateKeyCheckReport().getDuplicateRecordCount());
        assertTrue(fetcher.getDuplicateKeyCheckReport().getSamples().get(0)
                .toString().contains("key=[VIP]"));
    }

    @Test
    void matcherValidationAndExceptionsAreStable() {
        PriorityAssembler<Request, Rule, Object> assembler = PriorityAssembler
                .from(Request.class, Rule.class, Object.class)
                .initConfig(Collections.emptyList());
        assertThrows(IllegalArgumentException.class, () -> assembler.addPriorityMatcher(
                "bad", Request::getAmount, Rule::getAmount, null));
        assertThrows(IllegalArgumentException.class, () -> assembler.addPriorityMatcher(
                "bad", null, Rule::getAmount, PriorityMatchers.equal()));
        assertThrows(IllegalArgumentException.class, () -> assembler.addPriorityMatcher(
                "bad", Request::getAmount, null, PriorityMatchers.equal()));
        assertThrows(IllegalArgumentException.class, () -> PriorityMatchFunction.ofMatcher(
                "bad", 0, Request::getAmount, Rule::getAmount, null));

        RuntimeException sentinel = new RuntimeException("sentinel");
        PriorityFetcher<Request, Rule, Object> fetcher = PriorityAssembler
                .from(Request.class, Rule.class, Object.class)
                .initConfig(Collections.singletonList(
                        new Rule("one", PriorityRange.closed(decimal("1"), decimal("2")),
                                null, null)))
                .addPriorityMatcher("amount", Request::getAmount, Rule::getAmount,
                        (source, configured) -> {
                            throw sentinel;
                        })
                .create();
        assertSame(sentinel, assertThrows(RuntimeException.class,
                () -> fetcher.match(new Request(decimal("1"), null, null))));
    }

    @Test
    void directMatcherFactoryUsesTheExistingFetcherIndex() {
        PriorityMatchFunction<Request, Rule, Object> function = PriorityMatchFunction.ofMatcher(
                "amount", 0, Request::getAmount, Rule::getAmount,
                PriorityMatchers.<BigDecimal>numberRangeContains());
        PriorityMatchProcessor<Request, Rule, Object> processor =
                new PriorityMatchProcessor<>(Collections.singletonList(function));
        PriorityFetcher<Request, Rule, Object> fetcher = PriorityFetcher.from(
                Collections.singletonList(processor),
                Collections.singletonList(new Rule("one",
                        PriorityRange.closed(decimal("1"), decimal("2")), null, null)),
                Collections.singletonList(function));

        assertEquals("one", fetcher.match(new Request(decimal("1.5"), null, null))
                .getResult().get(0).id);
    }

    @Test
    void statelessBuiltInsAreStableForConcurrentReaders() throws Exception {
        PriorityFetcher<Request, Rule, Object> fetcher = assembler(Collections.singletonList(
                new Rule("one", PriorityRange.closed(decimal("10"), decimal("20")),
                        "api", Collections.singletonList("VIP"))),
                PriorityMode.NUMBER_OF_MATCHES).create().tree();
        Request request = new Request(decimal("15"), "api/v1", "VIP");
        ExecutorService executor = Executors.newFixedThreadPool(6);
        try {
            List<Callable<String>> calls = new ArrayList<>();
            for (int index = 0; index < 24; index++) {
                calls.add(() -> {
                    String last = null;
                    for (int iteration = 0; iteration < 100; iteration++) {
                        last = fetcher.match(request, true).get(0).getNameAndValue();
                    }
                    return last;
                });
            }
            for (Future<String> future : executor.invokeAll(calls)) {
                assertEquals("amount:[10,20]_path:api_roles:[VIP]", future.get());
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private static PriorityAssembler<Request, Rule, Object> assembler(
            List<Rule> rules, PriorityMode mode) {
        return PriorityAssembler.from(Request.class, Rule.class, Object.class)
                .initConfig(rules)
                .initPriorityHandler(mode)
                .addPriorityMatcher("amount", Request::getAmount, Rule::getAmount,
                        PriorityMatchers.<BigDecimal>numberRangeContains())
                .addPriorityMatcher("path", Request::getPath, Rule::getPathPrefix,
                        PriorityMatchers.stringStartsWith())
                .addPriorityMatcher("roles", Request::getRole, Rule::getRoles,
                        PriorityMatchers.<String>elementInCollection());
    }

    private static List<List<String>> project(List<PriorityMatchResult<List<Rule>>> results) {
        List<List<String>> projected = new ArrayList<>();
        for (PriorityMatchResult<List<Rule>> result : results) {
            List<String> row = new ArrayList<>();
            row.add(result.getName());
            row.add(result.getNameAndValue());
            row.addAll(result.getResult().stream().map(rule -> rule.id).collect(Collectors.toList()));
            projected.add(row);
        }
        return projected;
    }

    private static List<String> ids(List<PriorityMatchResult<List<Rule>>> results) {
        List<String> ids = new ArrayList<>();
        for (PriorityMatchResult<List<Rule>> result : results) {
            ids.addAll(result.getResult().stream().map(rule -> rule.id).collect(Collectors.toList()));
        }
        return ids;
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }

    @SuppressWarnings("unchecked")
    private static Class<ArrayList<String>> castArrayListClass() {
        return (Class<ArrayList<String>>) (Class<?>) ArrayList.class;
    }

    private static void assertMissingStringOperandsNeverMatch(
            PriorityMatcher<String, String> matcher) {
        assertFalse(matcher.matches(null, "x"));
        assertFalse(matcher.matches("", "x"));
        assertFalse(matcher.matches("x", null));
        assertFalse(matcher.matches("x", ""));
    }

    private static final class Request {
        private final BigDecimal amount;
        private final String path;
        private final String role;

        private Request(BigDecimal amount, String path, String role) {
            this.amount = amount;
            this.path = path;
            this.role = role;
        }

        private BigDecimal getAmount() {
            return amount;
        }

        private String getPath() {
            return path;
        }

        private String getRole() {
            return role;
        }
    }

    private static final class Rule {
        private final String id;
        private final PriorityRange<BigDecimal> amount;
        private final String pathPrefix;
        private final Collection<String> roles;

        private Rule(String id,
                     PriorityRange<BigDecimal> amount,
                     String pathPrefix,
                     Collection<String> roles) {
            this.id = id;
            this.amount = amount;
            this.pathPrefix = pathPrefix;
            this.roles = roles;
        }

        private PriorityRange<BigDecimal> getAmount() {
            return amount;
        }

        private String getPathPrefix() {
            return pathPrefix;
        }

        private Collection<String> getRoles() {
            return roles;
        }
    }

    private static final class ExactCollectionRequest {
        private final Collection<String> tags;

        private ExactCollectionRequest(Collection<String> tags) {
            this.tags = tags;
        }

        private Collection<String> getTags() {
            return tags;
        }
    }

    private static final class ExactCollectionRule {
        private final Collection<String> tags;

        private ExactCollectionRule(Collection<String> tags) {
            this.tags = tags;
        }

        private Collection<String> getTags() {
            return tags;
        }
    }

    private static final class ConcreteListRequest {
        private final ArrayList<String> tags;

        private ConcreteListRequest(ArrayList<String> tags) {
            this.tags = tags;
        }

        private ArrayList<String> getTags() {
            return tags;
        }
    }

    private static final class ConcreteListRule {
        private final ArrayList<String> tags;

        private ConcreteListRule(ArrayList<String> tags) {
            this.tags = tags;
        }

        private ArrayList<String> getTags() {
            return tags;
        }
    }
}
