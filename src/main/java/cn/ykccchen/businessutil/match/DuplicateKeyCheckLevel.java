package cn.ykccchen.businessutil.match;

/**
 * Controls duplicate complete-key validation during fetcher creation.
 *
 * @author ykccchen
 * @since 1.0.2
 */
public enum DuplicateKeyCheckLevel {
    /** Do not collect or report duplicate keys. */
    OFF,
    /** Complete creation, retain the report, and log duplicate keys. */
    WARNING,
    /** Abort creation by throwing {@link DuplicateMatchKeyException}. */
    EXCEPTION
}
