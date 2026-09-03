package cn.ykccchen.businessutil.match;

/**
 * Raised when duplicate complete keys are found at EXCEPTION level.
 *
 * @author ykccchen
 * @since 1.0.2
 */
public class DuplicateMatchKeyException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    private final DuplicateKeyCheckReport<?, ?> report;

    DuplicateMatchKeyException(DuplicateKeyCheckReport<?, ?> report) {
        super("Duplicate complete configuration keys detected: " + report);
        this.report = report;
    }

    public DuplicateKeyCheckReport<?, ?> getReport() {
        return report;
    }
}
