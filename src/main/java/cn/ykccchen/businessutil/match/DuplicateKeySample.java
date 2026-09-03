package cn.ykccchen.businessutil.match;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A bounded diagnostic sample from a duplicate complete-key group.
 *
 * @param <C> configuration type
 * @param <K> match-key type
 * @author ykccchen
 * @since 1.0.2
 */
public final class DuplicateKeySample<C, K> {
    private final List<DuplicateKeyPart<K>> keyParts;
    private final C config;

    DuplicateKeySample(List<DuplicateKeyPart<K>> keyParts, C config) {
        this.keyParts = Collections.unmodifiableList(new ArrayList<>(keyParts));
        this.config = config;
    }

    public List<DuplicateKeyPart<K>> getKeyParts() {
        return keyParts;
    }

    public C getConfig() {
        return config;
    }

    @Override
    public String toString() {
        return "DuplicateKeySample{" +
                "keyParts=" + keyParts +
                ", config=" + DuplicateKeyDiagnostics.safeValue(config) +
                '}';
    }
}
