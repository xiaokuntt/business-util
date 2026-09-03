package cn.ykccchen.businessutil.match;

/**
 * Default formatter for matched dimensions.
 *
 * @param <S> source type
 * @param <C> configuration type
 * @param <K> match-key type
 * @author ykccchen
 * @since 1.0.2
 */
public final class DefaultPriorityNameAndValueHandler<S, C, K>
        implements PriorityNameAndValueHandler<S, C, K> {

    @Override
    public String handle(String defaultName,
                         int priority,
                         S source,
                         C config,
                         K sourceValue,
                         K matchedConfigValue) {
        String effectiveName = defaultName == null ? "priority[" + priority + "]" : defaultName;
        return effectiveName + ":" + DuplicateKeyDiagnostics.safeValue(matchedConfigValue);
    }
}
