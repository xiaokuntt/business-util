package cn.ykccchen.businessutil.match;

/**
 * Matches a source value against a possibly different configuration-value type.
 *
 * <p>The argument order is always source first and configured value second.
 * Implementations should be deterministic, side-effect free, and thread-safe.</p>
 *
 * @param <SV> source-value type
 * @param <CV> configured-value type
 * @author ykccchen
 * @since 1.1.0
 */
@FunctionalInterface
public interface PriorityMatcher<SV, CV> {

    /**
     * Tests one source/configuration value pair.
     *
     * @param sourceValue source value
     * @param configuredValue configured value
     * @return true when the configured key matches the source
     */
    boolean matches(SV sourceValue, CV configuredValue);

    /**
     * Prepares a configured value before it becomes an index key.
     *
     * <p>The default implementation preserves the original value. Implementations
     * may return a stable immutable copy, as the collection matchers do.</p>
     *
     * @param configuredValue value extracted from configuration
     * @return prepared value used for indexing and matching
     */
    default CV prepareConfigValue(CV configuredValue) {
        return configuredValue;
    }
}
