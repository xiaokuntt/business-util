package cn.ykccchen.businessutil.match;

import java.util.Objects;

/**
 * One effective dimension in a configuration's complete match key.
 *
 * @param <K> match-key type
 * @author ykccchen
 * @since 1.0.2
 */
public final class DuplicateKeyPart<K> {
    private final int priority;
    private final String name;
    private final K key;
    private final Object diagnosticKey;

    DuplicateKeyPart(int priority, String name, K key) {
        this(priority, name, key, key);
    }

    DuplicateKeyPart(int priority, String name, K key, Object diagnosticKey) {
        this.priority = priority;
        this.name = name;
        this.key = key;
        this.diagnosticKey = diagnosticKey;
    }

    public int getPriority() {
        return priority;
    }

    public String getName() {
        return name;
    }

    public K getKey() {
        return key;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DuplicateKeyPart)) {
            return false;
        }
        DuplicateKeyPart<?> that = (DuplicateKeyPart<?>) other;
        return priority == that.priority && Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(priority, key);
    }

    @Override
    public String toString() {
        return "DuplicateKeyPart{" +
                "priority=" + priority +
                ", name='" + name + '\'' +
                ", key=" + DuplicateKeyDiagnostics.safeValue(diagnosticKey) +
                '}';
    }
}
