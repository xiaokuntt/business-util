package cn.ykccchen.businessutil.util.match;

import cn.ykccchen.businessutil.match.PriorityMatchers;
import cn.ykccchen.businessutil.match.PriorityRange;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriorityRangeTest {

    @Test
    void numericRangesHonorAllEndpointCombinations() {
        BigDecimal ten = decimal("10");
        BigDecimal fifteen = decimal("15");
        BigDecimal twenty = decimal("20");

        assertContains(PriorityRange.closed(ten, twenty), ten, fifteen, twenty);
        assertNotContains(PriorityRange.closed(ten, twenty), decimal("9"), decimal("21"));

        assertContains(PriorityRange.open(ten, twenty), fifteen);
        assertNotContains(PriorityRange.open(ten, twenty), ten, twenty);

        assertContains(PriorityRange.closedOpen(ten, twenty), ten, fifteen);
        assertNotContains(PriorityRange.closedOpen(ten, twenty), twenty);

        assertContains(PriorityRange.openClosed(ten, twenty), fifteen, twenty);
        assertNotContains(PriorityRange.openClosed(ten, twenty), ten);

        assertTrue(PriorityMatchers.<BigDecimal>numberRangeContains()
                .matches(fifteen, PriorityRange.closed(ten, twenty)));
        assertFalse(PriorityMatchers.<BigDecimal>numberRangeContains()
                .matches(null, PriorityRange.closed(ten, twenty)));
        assertFalse(PriorityMatchers.<BigDecimal>numberRangeContains()
                .matches(fifteen, null));
        assertTrue(PriorityMatchers.<BigDecimal>numberRangeNotContains()
                .matches(decimal("21"), PriorityRange.closed(ten, twenty)));
        assertFalse(PriorityMatchers.<BigDecimal>numberRangeNotContains()
                .matches(fifteen, PriorityRange.closed(ten, twenty)));
        assertFalse(PriorityMatchers.<BigDecimal>numberRangeNotContains()
                .matches(null, PriorityRange.closed(ten, twenty)));
        assertFalse(PriorityMatchers.<BigDecimal>numberRangeNotContains()
                .matches(fifteen, null));
        assertFalse(PriorityMatchers.<Double>numberRangeContains()
                .matches(Double.NaN, PriorityRange.closed(1D, 2D)));
        assertFalse(PriorityMatchers.<Double>numberRangeNotContains()
                .matches(Double.NaN, PriorityRange.closed(1D, 2D)));
    }

    @Test
    void oneSidedRangesAndExtremeValuesAreSupported() {
        assertTrue(PriorityRange.atLeast(Integer.MIN_VALUE).contains(Integer.MIN_VALUE));
        assertTrue(PriorityRange.atMost(Integer.MAX_VALUE).contains(Integer.MAX_VALUE));
        assertFalse(PriorityRange.greaterThan(10).contains(10));
        assertTrue(PriorityRange.greaterThan(10).contains(11));
        assertFalse(PriorityRange.lessThan(10).contains(10));
        assertTrue(PriorityRange.lessThan(10).contains(9));
    }

    @Test
    void invalidAndEffectivelyEmptyRangesFailFast() {
        assertThrows(IllegalArgumentException.class,
                () -> PriorityRange.closed(2, 1));
        assertThrows(IllegalArgumentException.class,
                () -> PriorityRange.open(1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> PriorityRange.closedOpen(1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> PriorityRange.openClosed(1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> PriorityRange.closed(null, 1));
        assertThrows(IllegalArgumentException.class,
                () -> PriorityRange.closed(1, null));
        assertThrows(IllegalArgumentException.class,
                () -> PriorityRange.atLeast(null));
        assertThrows(IllegalArgumentException.class,
                () -> PriorityRange.closed(Double.NaN, 2D));
        assertThrows(IllegalArgumentException.class,
                () -> PriorityRange.closed(1F, Float.POSITIVE_INFINITY));
    }

    @Test
    void timeRangesUseTheSameComparableBoundaryRules() {
        Instant start = Instant.parse("2026-09-03T10:00:00Z");
        Instant end = Instant.parse("2026-09-03T11:00:00Z");
        assertTrue(PriorityMatchers.<Instant>timeRangeContains()
                .matches(start, PriorityRange.closedOpen(start, end)));
        assertFalse(PriorityMatchers.<Instant>timeRangeContains()
                .matches(end, PriorityRange.closedOpen(start, end)));
        assertFalse(PriorityMatchers.<Instant>timeRangeContains()
                .matches(null, PriorityRange.closedOpen(start, end)));
        assertFalse(PriorityMatchers.<Instant>timeRangeContains().matches(start, null));
        assertTrue(PriorityMatchers.<Instant>timeRangeNotContains()
                .matches(end, PriorityRange.closedOpen(start, end)));
        assertFalse(PriorityMatchers.<Instant>timeRangeNotContains()
                .matches(start, PriorityRange.closedOpen(start, end)));
        assertFalse(PriorityMatchers.<Instant>timeRangeNotContains()
                .matches(null, PriorityRange.closedOpen(start, end)));
        assertFalse(PriorityMatchers.<Instant>timeRangeNotContains().matches(start, null));

        LocalDate day = LocalDate.of(2026, 9, 3);
        assertTrue(PriorityMatchers.<LocalDate>timeRangeContains()
                .matches(day, PriorityRange.closed(day, day)));

        LocalDateTime noon = LocalDateTime.of(2026, 9, 3, 12, 0);
        assertTrue(PriorityMatchers.<LocalDateTime>timeRangeContains()
                .matches(noon, PriorityRange.atLeast(noon)));
    }

    @Test
    void genericRangeMatchersRejectTopLevelEmptyStringValues() {
        PriorityRange<String> configured = PriorityRange.closed("", "z");

        assertFalse(PriorityMatchers.<String>rangeContains().matches("", configured));
        assertFalse(PriorityMatchers.<String>rangeNotContains().matches("", configured));
        assertTrue(PriorityMatchers.<String>rangeContains().matches("a", configured));
    }

    @Test
    void overlapRespectsOpenAndClosedTouchingEndpoints() {
        assertTrue(PriorityRange.closed(0, 10)
                .overlaps(PriorityRange.closed(10, 20)));
        assertFalse(PriorityRange.closedOpen(0, 10)
                .overlaps(PriorityRange.closed(10, 20)));
        assertFalse(PriorityRange.closed(0, 10)
                .overlaps(PriorityRange.openClosed(10, 20)));
        assertTrue(PriorityRange.closed(2, 8)
                .overlaps(PriorityRange.closed(0, 10)));
        assertFalse(PriorityRange.lessThan(0)
                .overlaps(PriorityRange.atLeast(0)));
        assertTrue(PriorityMatchers.<Integer>rangesOverlap()
                .matches(PriorityRange.closed(0, 10), PriorityRange.closed(5, 6)));
        assertTrue(PriorityMatchers.<Integer>rangesDisjoint()
                .matches(PriorityRange.closedOpen(0, 10), PriorityRange.closed(10, 20)));
        assertFalse(PriorityMatchers.<Integer>rangesDisjoint()
                .matches(PriorityRange.closed(0, 10), PriorityRange.closed(10, 20)));
        assertFalse(PriorityMatchers.<Integer>rangesOverlap()
                .matches(null, PriorityRange.closed(0, 10)));
        assertFalse(PriorityMatchers.<Integer>rangesOverlap()
                .matches(PriorityRange.closed(0, 10), null));
        assertFalse(PriorityMatchers.<Integer>rangesDisjoint()
                .matches(null, PriorityRange.closed(0, 10)));
        assertFalse(PriorityMatchers.<Integer>rangesDisjoint()
                .matches(PriorityRange.closed(0, 10), null));
    }

    @Test
    void rangeIsAnImmutableDiagnosticValue() {
        PriorityRange<Integer> first = PriorityRange.closedOpen(1, 3);
        PriorityRange<Integer> same = PriorityRange.closedOpen(1, 3);
        PriorityRange<Integer> different = PriorityRange.closed(1, 3);

        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertNotEquals(first, different);
        assertEquals("[1,3)", first.toString());
        assertEquals("(-∞,3)", PriorityRange.lessThan(3).toString());
        assertEquals("[1,+∞)", PriorityRange.atLeast(1).toString());
    }

    @SafeVarargs
    private static <T extends Comparable<? super T>> void assertContains(
            PriorityRange<T> range, T... values) {
        for (T value : values) {
            assertTrue(range.contains(value), range + " should contain " + value);
        }
    }

    @SafeVarargs
    private static <T extends Comparable<? super T>> void assertNotContains(
            PriorityRange<T> range, T... values) {
        for (T value : values) {
            assertFalse(range.contains(value), range + " should not contain " + value);
        }
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}
