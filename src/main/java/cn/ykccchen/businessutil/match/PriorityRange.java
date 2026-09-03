package cn.ykccchen.businessutil.match;

import java.util.Objects;

/**
 * Immutable comparable interval used by built-in range matchers.
 *
 * @param <T> endpoint type
 * @author ykccchen
 * @since 1.1.0
 */
public final class PriorityRange<T extends Comparable<? super T>> {

    private final T lower;
    private final T upper;
    private final boolean lowerInclusive;
    private final boolean upperInclusive;

    private PriorityRange(T lower, boolean lowerInclusive, T upper, boolean upperInclusive) {
        if (lower == null && upper == null) {
            throw new IllegalArgumentException("A priority range requires at least one endpoint");
        }
        rejectNonFinite(lower);
        rejectNonFinite(upper);
        if (lower != null && upper != null) {
            int comparison = lower.compareTo(upper);
            if (comparison > 0) {
                throw new IllegalArgumentException("Range lower endpoint must not exceed its upper endpoint");
            }
            if (comparison == 0 && (!lowerInclusive || !upperInclusive)) {
                throw new IllegalArgumentException("A range with equal endpoints must be closed at both ends");
            }
        }
        this.lower = lower;
        this.upper = upper;
        this.lowerInclusive = lower != null && lowerInclusive;
        this.upperInclusive = upper != null && upperInclusive;
    }

    public static <T extends Comparable<? super T>> PriorityRange<T> closed(T lower, T upper) {
        requireBounded(lower, upper);
        return new PriorityRange<>(lower, true, upper, true);
    }

    public static <T extends Comparable<? super T>> PriorityRange<T> open(T lower, T upper) {
        requireBounded(lower, upper);
        return new PriorityRange<>(lower, false, upper, false);
    }

    public static <T extends Comparable<? super T>> PriorityRange<T> closedOpen(T lower, T upper) {
        requireBounded(lower, upper);
        return new PriorityRange<>(lower, true, upper, false);
    }

    public static <T extends Comparable<? super T>> PriorityRange<T> openClosed(T lower, T upper) {
        requireBounded(lower, upper);
        return new PriorityRange<>(lower, false, upper, true);
    }

    public static <T extends Comparable<? super T>> PriorityRange<T> atLeast(T lower) {
        return new PriorityRange<>(requireEndpoint(lower), true, null, false);
    }

    public static <T extends Comparable<? super T>> PriorityRange<T> greaterThan(T lower) {
        return new PriorityRange<>(requireEndpoint(lower), false, null, false);
    }

    public static <T extends Comparable<? super T>> PriorityRange<T> atMost(T upper) {
        return new PriorityRange<>(null, false, requireEndpoint(upper), true);
    }

    public static <T extends Comparable<? super T>> PriorityRange<T> lessThan(T upper) {
        return new PriorityRange<>(null, false, requireEndpoint(upper), false);
    }

    public T getLower() {
        return lower;
    }

    public T getUpper() {
        return upper;
    }

    public boolean isLowerInclusive() {
        return lowerInclusive;
    }

    public boolean isUpperInclusive() {
        return upperInclusive;
    }

    public boolean contains(T value) {
        if (value == null || !isFiniteValue(value)) {
            return false;
        }
        if (lower != null) {
            int comparison = value.compareTo(lower);
            if (comparison < 0 || comparison == 0 && !lowerInclusive) {
                return false;
            }
        }
        if (upper != null) {
            int comparison = value.compareTo(upper);
            if (comparison > 0 || comparison == 0 && !upperInclusive) {
                return false;
            }
        }
        return true;
    }

    public boolean overlaps(PriorityRange<T> other) {
        if (other == null) {
            return false;
        }
        if (upper != null && other.lower != null) {
            int comparison = upper.compareTo(other.lower);
            if (comparison < 0 || comparison == 0 && !(upperInclusive && other.lowerInclusive)) {
                return false;
            }
        }
        if (other.upper != null && lower != null) {
            int comparison = other.upper.compareTo(lower);
            if (comparison < 0 || comparison == 0 && !(other.upperInclusive && lowerInclusive)) {
                return false;
            }
        }
        return true;
    }

    private static void requireBounded(Object lower, Object upper) {
        if (lower == null || upper == null) {
            throw new IllegalArgumentException("Bounded range endpoints cannot be null");
        }
    }

    private static <T> T requireEndpoint(T endpoint) {
        if (endpoint == null) {
            throw new IllegalArgumentException("Range endpoint cannot be null");
        }
        return endpoint;
    }

    private static void rejectNonFinite(Object endpoint) {
        if (!isFiniteValue(endpoint)) {
            throw new IllegalArgumentException("Floating-point range endpoints must be finite");
        }
    }

    static boolean isFiniteValue(Object value) {
        if (value instanceof Double) {
            double number = (Double) value;
            return !Double.isNaN(number) && !Double.isInfinite(number);
        }
        if (value instanceof Float) {
            float number = (Float) value;
            return !Float.isNaN(number) && !Float.isInfinite(number);
        }
        return true;
    }

    @Override
    public boolean equals(Object value) {
        if (this == value) {
            return true;
        }
        if (!(value instanceof PriorityRange)) {
            return false;
        }
        PriorityRange<?> other = (PriorityRange<?>) value;
        return lowerInclusive == other.lowerInclusive
                && upperInclusive == other.upperInclusive
                && Objects.equals(lower, other.lower)
                && Objects.equals(upper, other.upper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lower, upper, lowerInclusive, upperInclusive);
    }

    @Override
    public String toString() {
        return (lowerInclusive ? "[" : "(")
                + (lower == null ? "-∞" : lower)
                + ","
                + (upper == null ? "+∞" : upper)
                + (upperInclusive ? "]" : ")");
    }
}
