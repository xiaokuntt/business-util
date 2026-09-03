package cn.ykccchen.businessutil.match;

/**
 * Formats one matched dimension for {@link PriorityMatchResult#getNameAndValue()}.
 * Implementations may replace both the displayed name and value.
 *
 * @param <S> source type
 * @param <C> configuration type
 * @param <K> match-key type
 * @author ykccchen
 * @since 1.0.2
 */
@FunctionalInterface
public interface PriorityNameAndValueHandler<S, C, K> {

    /**
     * Formats one matched dimension.
     *
     * @param defaultName registered dimension name, possibly null
     * @param priority zero-based dimension priority
     * @param source matching source
     * @param config matched configuration
     * @param sourceValue value extracted from the source
     * @param matchedConfigValue actual indexed configuration value that matched
     * @return rendered pair, or null to omit this dimension
     */
    String handle(String defaultName,
                  int priority,
                  S source,
                  C config,
                  K sourceValue,
                  K matchedConfigValue);
}
