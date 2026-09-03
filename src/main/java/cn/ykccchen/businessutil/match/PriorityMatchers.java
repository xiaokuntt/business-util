package cn.ykccchen.businessutil.match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Factories for common source-to-configuration matching rules.
 *
 * <p>Every matcher receives the source value first and the configured value
 * second. Null and zero-length string operands never match.</p>
 *
 * @author ykccchen
 * @since 1.1.0
 */
public final class PriorityMatchers {

    private static final PriorityMatcher<Object, Object> EXACT = new PriorityMatcher<Object, Object>() {
        @Override
        public boolean matches(Object sourceValue, Object configuredValue) {
            return present(sourceValue) && present(configuredValue)
                    && Objects.equals(sourceValue, configuredValue);
        }

    };

    private static final PriorityMatcher<Object, Object> NOT_EXACT =
            new PriorityMatcher<Object, Object>() {
                @Override
                public boolean matches(Object sourceValue, Object configuredValue) {
                    return present(sourceValue) && present(configuredValue)
                            && !Objects.equals(sourceValue, configuredValue);
                }

            };

    private PriorityMatchers() {
    }

    @SuppressWarnings("unchecked")
    public static <T> PriorityMatcher<T, T> equal() {
        return (PriorityMatcher<T, T>) (PriorityMatcher<?, ?>) EXACT;
    }

    @SuppressWarnings("unchecked")
    public static <T> PriorityMatcher<T, T> notEqual() {
        return (PriorityMatcher<T, T>) (PriorityMatcher<?, ?>) NOT_EXACT;
    }

    public static <T extends Comparable<? super T>> PriorityMatcher<T, T> greaterThan() {
        return (sourceValue, configuredValue) ->
                comparableValuesPresent(sourceValue, configuredValue)
                        && sourceValue.compareTo(configuredValue) > 0;
    }

    public static <T extends Comparable<? super T>> PriorityMatcher<T, T> greaterThanOrEqual() {
        return (sourceValue, configuredValue) ->
                comparableValuesPresent(sourceValue, configuredValue)
                        && sourceValue.compareTo(configuredValue) >= 0;
    }

    public static <T extends Comparable<? super T>> PriorityMatcher<T, T> lessThan() {
        return (sourceValue, configuredValue) ->
                comparableValuesPresent(sourceValue, configuredValue)
                        && sourceValue.compareTo(configuredValue) < 0;
    }

    public static <T extends Comparable<? super T>> PriorityMatcher<T, T> lessThanOrEqual() {
        return (sourceValue, configuredValue) ->
                comparableValuesPresent(sourceValue, configuredValue)
                        && sourceValue.compareTo(configuredValue) <= 0;
    }

    public static <T extends Comparable<? super T>>
    PriorityMatcher<T, PriorityRange<T>> rangeContains() {
        return (sourceValue, configuredRange) ->
                present(sourceValue) && configuredRange != null
                        && configuredRange.contains(sourceValue);
    }

    public static <T extends Comparable<? super T>>
    PriorityMatcher<T, PriorityRange<T>> rangeNotContains() {
        return (sourceValue, configuredRange) ->
                present(sourceValue) && PriorityRange.isFiniteValue(sourceValue)
                        && configuredRange != null && !configuredRange.contains(sourceValue);
    }

    public static <N extends Number & Comparable<? super N>>
    PriorityMatcher<N, PriorityRange<N>> numberRangeContains() {
        return rangeContains();
    }

    public static <N extends Number & Comparable<? super N>>
    PriorityMatcher<N, PriorityRange<N>> numberRangeNotContains() {
        return rangeNotContains();
    }

    public static <T extends Comparable<? super T>>
    PriorityMatcher<T, PriorityRange<T>> timeRangeContains() {
        return rangeContains();
    }

    public static <T extends Comparable<? super T>>
    PriorityMatcher<T, PriorityRange<T>> timeRangeNotContains() {
        return rangeNotContains();
    }

    public static <T extends Comparable<? super T>>
    PriorityMatcher<PriorityRange<T>, PriorityRange<T>> rangesOverlap() {
        return (sourceRange, configuredRange) ->
                sourceRange != null && configuredRange != null && sourceRange.overlaps(configuredRange);
    }

    public static <T extends Comparable<? super T>>
    PriorityMatcher<PriorityRange<T>, PriorityRange<T>> rangesDisjoint() {
        return (sourceRange, configuredRange) ->
                sourceRange != null && configuredRange != null && !sourceRange.overlaps(configuredRange);
    }

    public static PriorityMatcher<String, String> stringEqualsIgnoreCase() {
        return (sourceValue, configuredValue) ->
                stringsPresent(sourceValue, configuredValue)
                        && sourceValue.equalsIgnoreCase(configuredValue);
    }

    public static PriorityMatcher<String, String> stringNotEqualsIgnoreCase() {
        return (sourceValue, configuredValue) ->
                stringsPresent(sourceValue, configuredValue)
                        && !sourceValue.equalsIgnoreCase(configuredValue);
    }

    public static PriorityMatcher<String, String> stringStartsWith() {
        return (sourceValue, configuredValue) ->
                stringsPresent(sourceValue, configuredValue)
                        && sourceValue.startsWith(configuredValue);
    }

    public static PriorityMatcher<String, String> stringStartsWithIgnoreCase() {
        return (sourceValue, configuredValue) ->
                stringsPresent(sourceValue, configuredValue)
                        && sourceValue.length() >= configuredValue.length()
                        && sourceValue.regionMatches(true, 0, configuredValue, 0, configuredValue.length());
    }

    public static PriorityMatcher<String, String> stringNotStartsWith() {
        return (sourceValue, configuredValue) ->
                stringsPresent(sourceValue, configuredValue)
                        && !sourceValue.startsWith(configuredValue);
    }

    public static PriorityMatcher<String, String> stringNotStartsWithIgnoreCase() {
        return (sourceValue, configuredValue) ->
                stringsPresent(sourceValue, configuredValue)
                        && (sourceValue.length() < configuredValue.length()
                        || !sourceValue.regionMatches(true, 0,
                        configuredValue, 0, configuredValue.length()));
    }

    public static PriorityMatcher<String, String> stringEndsWith() {
        return (sourceValue, configuredValue) ->
                stringsPresent(sourceValue, configuredValue)
                        && sourceValue.endsWith(configuredValue);
    }

    public static PriorityMatcher<String, String> stringEndsWithIgnoreCase() {
        return (sourceValue, configuredValue) -> {
            if (!stringsPresent(sourceValue, configuredValue)
                    || sourceValue.length() < configuredValue.length()) {
                return false;
            }
            int sourceOffset = sourceValue.length() - configuredValue.length();
            return sourceValue.regionMatches(true, sourceOffset,
                    configuredValue, 0, configuredValue.length());
        };
    }

    public static PriorityMatcher<String, String> stringNotEndsWith() {
        return (sourceValue, configuredValue) ->
                stringsPresent(sourceValue, configuredValue)
                        && !sourceValue.endsWith(configuredValue);
    }

    public static PriorityMatcher<String, String> stringNotEndsWithIgnoreCase() {
        return (sourceValue, configuredValue) -> {
            if (!stringsPresent(sourceValue, configuredValue)) {
                return false;
            }
            if (sourceValue.length() < configuredValue.length()) {
                return true;
            }
            int sourceOffset = sourceValue.length() - configuredValue.length();
            return !sourceValue.regionMatches(true, sourceOffset,
                    configuredValue, 0, configuredValue.length());
        };
    }

    public static PriorityMatcher<String, String> stringContains() {
        return (sourceValue, configuredValue) ->
                stringsPresent(sourceValue, configuredValue)
                        && sourceValue.contains(configuredValue);
    }

    public static PriorityMatcher<String, String> stringContainsIgnoreCase() {
        return (sourceValue, configuredValue) ->
                stringsPresent(sourceValue, configuredValue)
                        && containsIgnoreCase(sourceValue, configuredValue);
    }

    public static PriorityMatcher<String, String> stringNotContains() {
        return (sourceValue, configuredValue) ->
                stringsPresent(sourceValue, configuredValue)
                        && !sourceValue.contains(configuredValue);
    }

    public static PriorityMatcher<String, String> stringNotContainsIgnoreCase() {
        return (sourceValue, configuredValue) ->
                stringsPresent(sourceValue, configuredValue)
                        && !containsIgnoreCase(sourceValue, configuredValue);
    }

    /**
     * Matches the complete source character sequence against a compiled pattern.
     *
     * @return full-string regular-expression matcher
     */
    public static PriorityMatcher<CharSequence, Pattern> stringMatchesRegex() {
        return (sourceValue, configuredPattern) ->
                sourceValue != null && sourceValue.length() > 0
                        && configuredPattern != null
                        && !configuredPattern.pattern().isEmpty()
                        && configuredPattern.matcher(sourceValue).matches();
    }

    public static PriorityMatcher<CharSequence, Pattern> stringNotMatchesRegex() {
        return (sourceValue, configuredPattern) ->
                sourceValue != null && sourceValue.length() > 0
                        && configuredPattern != null
                        && !configuredPattern.pattern().isEmpty()
                        && !configuredPattern.matcher(sourceValue).matches();
    }

    public static <E> PriorityMatcher<E, Collection<E>> elementInCollection() {
        return new CollectionConfigMatcher<E, E>() {
            @Override
            public boolean matches(E sourceValue, Collection<E> configuredValues) {
                return present(sourceValue) && configuredValues != null
                        && containsPresent(configuredValues, sourceValue);
            }
        };
    }

    public static <E> PriorityMatcher<E, Collection<E>> elementNotInCollection() {
        return new CollectionConfigMatcher<E, E>() {
            @Override
            public boolean matches(E sourceValue, Collection<E> configuredValues) {
                return present(sourceValue) && configuredValues != null
                        && !containsPresent(configuredValues, sourceValue);
            }
        };
    }

    public static <E> PriorityMatcher<Collection<E>, E> collectionContainsElement() {
        return (sourceValues, configuredValue) ->
                sourceValues != null && present(configuredValue)
                        && containsPresent(sourceValues, configuredValue);
    }

    public static <E> PriorityMatcher<Collection<E>, E> collectionNotContainsElement() {
        return (sourceValues, configuredValue) ->
                sourceValues != null && present(configuredValue)
                        && !containsPresent(sourceValues, configuredValue);
    }

    public static <E> PriorityMatcher<Collection<E>, Collection<E>> collectionIntersects() {
        return new CollectionConfigMatcher<Collection<E>, E>() {
            @Override
            public boolean matches(Collection<E> sourceValues, Collection<E> configuredValues) {
                return sourceValues != null && configuredValues != null
                        && intersects(sourceValues, configuredValues);
            }
        };
    }

    public static <E> PriorityMatcher<Collection<E>, Collection<E>> collectionContainsAll() {
        return new CollectionConfigMatcher<Collection<E>, E>() {
            @Override
            public boolean matches(Collection<E> sourceValues, Collection<E> configuredValues) {
                return sourceValues != null && configuredValues != null
                        && containsAllPresent(sourceValues, configuredValues);
            }
        };
    }

    public static <E> PriorityMatcher<Collection<E>, Collection<E>> collectionContainedBy() {
        return new CollectionConfigMatcher<Collection<E>, E>() {
            @Override
            public boolean matches(Collection<E> sourceValues, Collection<E> configuredValues) {
                return sourceValues != null && configuredValues != null
                        && containsAllPresent(configuredValues, sourceValues);
            }
        };
    }

    public static <E> PriorityMatcher<Collection<E>, Collection<E>> collectionDisjoint() {
        return new CollectionConfigMatcher<Collection<E>, E>() {
            @Override
            public boolean matches(Collection<E> sourceValues, Collection<E> configuredValues) {
                return sourceValues != null && configuredValues != null
                        && !intersects(sourceValues, configuredValues);
            }
        };
    }

    static boolean isExactMatcher(PriorityMatcher<?, ?> matcher) {
        return matcher == EXACT;
    }

    /**
     * Prepares an index key without weakening {@link PriorityMatcher}'s public
     * configured-value return type. Equality matchers are generic over any
     * {@code T}; therefore a collection wrapper cannot safely be returned as
     * that same {@code T} when callers select a concrete class such as
     * {@link ArrayList}. The internal index works with the assembler's common
     * key supertype and can safely store the immutable snapshot here.
     */
    static Object prepareIndexConfigValue(PriorityMatcher<?, ?> matcher,
                                          Object configuredValue) {
        if ((matcher == EXACT || matcher == NOT_EXACT)
                && configuredValue instanceof Collection) {
            return immutableEqualityCopy((Collection<?>) configuredValue);
        }
        return prepareWithMatcher(matcher, configuredValue);
    }

    @SuppressWarnings("unchecked")
    private static Object prepareWithMatcher(PriorityMatcher<?, ?> matcher,
                                             Object configuredValue) {
        return ((PriorityMatcher<Object, Object>) matcher)
                .prepareConfigValue(configuredValue);
    }

    private static boolean stringsPresent(String sourceValue, String configuredValue) {
        return sourceValue != null && !sourceValue.isEmpty()
                && configuredValue != null && !configuredValue.isEmpty();
    }

    private static boolean present(Object value) {
        return value != null && !Objects.equals("", value);
    }

    private static boolean comparableValuesPresent(Object sourceValue, Object configuredValue) {
        return present(sourceValue) && present(configuredValue)
                && PriorityRange.isFiniteValue(sourceValue)
                && PriorityRange.isFiniteValue(configuredValue);
    }

    private static boolean containsIgnoreCase(String sourceValue, String configuredValue) {
        int lastOffset = sourceValue.length() - configuredValue.length();
        for (int offset = 0; offset <= lastOffset; offset++) {
            if (sourceValue.regionMatches(true, offset,
                    configuredValue, 0, configuredValue.length())) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsPresent(Collection<?> values, Object expected) {
        if (!present(expected)) {
            return false;
        }
        for (Object value : values) {
            if (present(value) && Objects.equals(value, expected)) {
                return true;
            }
        }
        return false;
    }

    private static boolean intersects(Collection<?> sourceValues, Collection<?> configuredValues) {
        for (Object configuredValue : configuredValues) {
            if (containsPresent(sourceValues, configuredValue)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAllPresent(Collection<?> sourceValues,
                                              Collection<?> configuredValues) {
        for (Object configuredValue : configuredValues) {
            if (present(configuredValue) && !containsPresent(sourceValues, configuredValue)) {
                return false;
            }
        }
        return true;
    }

    private static <E> Collection<E> immutableCopy(Collection<E> values) {
        if (values == null) {
            return null;
        }
        if (values instanceof Set) {
            Set<E> copy = new LinkedHashSet<>();
            for (E value : values) {
                if (present(value)) {
                    copy.add(value);
                }
            }
            return Collections.unmodifiableSet(copy);
        }
        List<E> copy = new ArrayList<>(values.size());
        for (E value : values) {
            if (present(value)) {
                copy.add(value);
            }
        }
        return Collections.unmodifiableList(copy);
    }

    private static <E> Collection<E> immutableEqualityCopy(Collection<E> values) {
        if (values instanceof Set) {
            return Collections.unmodifiableSet(new LinkedHashSet<>(values));
        }
        if (values instanceof List) {
            return Collections.unmodifiableList(new ArrayList<>(values));
        }
        throw new IllegalArgumentException("equal/notEqual collection configuration values "
                + "must implement List or Set so snapshotting preserves Java equality semantics");
    }

    private abstract static class CollectionConfigMatcher<SV, E>
            implements PriorityMatcher<SV, Collection<E>> {

        @Override
        public final Collection<E> prepareConfigValue(Collection<E> configuredValue) {
            return immutableCopy(configuredValue);
        }
    }
}
