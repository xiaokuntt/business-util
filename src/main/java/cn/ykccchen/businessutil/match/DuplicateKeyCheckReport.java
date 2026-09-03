package cn.ykccchen.businessutil.match;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable result of duplicate complete-key validation.
 *
 * @param <C> configuration type
 * @param <K> match-key type
 * @author ykccchen
 * @since 1.0.2
 */
public final class DuplicateKeyCheckReport<C, K> {
    /** Maximum number of configuration samples retained in diagnostics. */
    public static final int MAX_SAMPLE_COUNT = 10;

    private final DuplicateKeyCheckLevel level;
    private final int duplicateGroupCount;
    private final int duplicateRecordCount;
    private final List<DuplicateKeySample<C, K>> samples;

    DuplicateKeyCheckReport(DuplicateKeyCheckLevel level,
                            int duplicateGroupCount,
                            int duplicateRecordCount,
                            List<DuplicateKeySample<C, K>> samples) {
        this.level = level;
        this.duplicateGroupCount = duplicateGroupCount;
        this.duplicateRecordCount = duplicateRecordCount;
        this.samples = Collections.unmodifiableList(new ArrayList<>(samples));
    }

    static <C, K> DuplicateKeyCheckReport<C, K> empty(DuplicateKeyCheckLevel level) {
        return new DuplicateKeyCheckReport<>(level, 0, 0, Collections.emptyList());
    }

    public DuplicateKeyCheckLevel getLevel() {
        return level;
    }

    public int getDuplicateGroupCount() {
        return duplicateGroupCount;
    }

    public int getDuplicateRecordCount() {
        return duplicateRecordCount;
    }

    public List<DuplicateKeySample<C, K>> getSamples() {
        return samples;
    }

    public boolean hasDuplicates() {
        return duplicateGroupCount > 0;
    }

    @Override
    public String toString() {
        return "DuplicateKeyCheckReport{" +
                "level=" + level +
                ", duplicateGroupCount=" + duplicateGroupCount +
                ", duplicateRecordCount=" + duplicateRecordCount +
                ", samples=" + samples +
                '}';
    }
}
