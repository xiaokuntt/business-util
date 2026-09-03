package cn.ykccchen.businessutil.match;

import cn.ykccchen.businessutil.match.handler.PriorityMatchType;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Defines how one priority dimension extracts and compares keys.
 *
 * @author ykccchen
 * @version 1.0
 * @since 1.0
 */
public class PriorityMatchFunction<S, C, K> {

    private final String name;
    private final String uniqueId;

    private final PriorityMatchType type;

    /**
     * 0 优先级最高, 不能小于0
     */
    private final Integer priority;
    /**
     * 资源对象获取方式
     */
    private final Function<S, K> sourceGetter;
    /**
     * 订单对象获取方式
     */
    private final Function<C, K> configGetter;

    /**
     * Internal index-key preparer. Its value can be a stable representation
     * that intentionally differs from the public configured-value type K.
     */
    private final Function<K, Object> indexConfigPreparer;

    /**
     * 资源K与配置K是否匹配
     */
    private final BiPredicate<K, K> keyMatchFunction;


    private PriorityMatchFunction(String name,
                                  Integer priority,
                                  PriorityMatchType type,
                                  Function<S, K> sourceGetter,
                                  Function<C, K> configGetter,
                                  Function<K, Object> indexConfigPreparer,
                                  BiPredicate<K, K> keyMatchFunction) {
        if (priority == null || priority < 0) {
            throw new IllegalArgumentException("Priority must be a non-negative integer");
        }
        if (sourceGetter == null || configGetter == null || indexConfigPreparer == null) {
            throw new IllegalArgumentException("Source and config getters cannot be null");
        }
        if (type == PriorityMatchType.BOOLEAN && keyMatchFunction == null) {
            throw new IllegalArgumentException("Boolean key matcher cannot be null");
        }
        this.name = name;
        this.type = type;
        this.uniqueId = UUID.randomUUID().toString();
        this.priority = priority;
        this.sourceGetter = sourceGetter;
        this.configGetter = configGetter;
        this.indexConfigPreparer = indexConfigPreparer;
        this.keyMatchFunction = keyMatchFunction;
    }

    /**
     * 创建一个新的数据键。
     *
     * @param name 键的描述性名称，方便调试
     * @param priority 从零开始的优先级
     * @param sourceGetter 需求键提取器
     * @param configGetter 配置键提取器
     * @param <S> 需求类型
     * @param <C> 配置类型
     * @param <K> 匹配键类型
     * @return 新的匹配函数
     */
    public static <S, C, K> PriorityMatchFunction<S, C, K> of(String name,
                                                              Integer priority,
                                                              Function<S, K> sourceGetter,
                                                              Function<C, K> configGetter) {
        return new PriorityMatchFunction<>(name, priority, PriorityMatchType.COMMON,
                sourceGetter, configGetter, value -> value, null);
    }

    public static <S, C, K> PriorityMatchFunction<S, C, K> of(Integer priority,
                                                              Function<S, K> sourceGetter,
                                                              Function<C, K> configGetter) {
        return PriorityMatchFunction.of(null, priority, sourceGetter, configGetter);
    }

    public static <S, C, K> PriorityMatchFunction<S, C, K> ofBoolean(String name,
                                                                     Integer priority,
                                                                     Function<S, K> sourceGetter,
                                                                     Function<C, K> configGetter,
                                                                     BiPredicate<K, K> keyMatchFunction) {
        return new PriorityMatchFunction<>(name, priority, PriorityMatchType.BOOLEAN,
                sourceGetter, configGetter, value -> value, keyMatchFunction);
    }

    public static <S, C, K> PriorityMatchFunction<S, C, K> ofBoolean(Integer priority,
                                                                     Function<S, K> sourceGetter,
                                                                     Function<C, K> configGetter,
                                                                     BiPredicate<K, K> keyMatchFunction) {
        return PriorityMatchFunction.ofBoolean(null, priority, sourceGetter, configGetter, keyMatchFunction);
    }

    /**
     * Creates a rule whose source and configured values may have different types.
     *
     * <p>Both value types must extend the common key type {@code K}. Use
     * {@code Object} as the assembler key type for heterogeneous dimensions.</p>
     *
     * @param name dimension name
     * @param priority zero-based priority
     * @param sourceGetter source-value extractor
     * @param configGetter configured-value extractor
     * @param matcher typed matcher, receiving source then configured value
     * @param <S> source type
     * @param <C> configuration type
     * @param <K> common key supertype
     * @param <SV> source-value type
     * @param <CV> configured-value type
     * @return a new match function
     */
    @SuppressWarnings("unchecked")
    public static <S, C, K, SV extends K, CV extends K> PriorityMatchFunction<S, C, K> ofMatcher(
            String name,
            Integer priority,
            Function<S, SV> sourceGetter,
            Function<C, CV> configGetter,
            PriorityMatcher<SV, CV> matcher) {
        if (sourceGetter == null || configGetter == null) {
            throw new IllegalArgumentException("Source and config getters cannot be null");
        }
        if (matcher == null) {
            throw new IllegalArgumentException("Priority matcher cannot be null");
        }
        Function<S, K> adaptedSourceGetter = source -> sourceGetter.apply(source);
        Function<C, K> adaptedConfigGetter = config -> configGetter.apply(config);
        Function<K, Object> adaptedIndexConfigPreparer = configuredValue ->
                PriorityMatchers.prepareIndexConfigValue(
                        matcher, configuredValue);
        BiPredicate<K, K> adaptedMatcher = PriorityMatchers.isExactMatcher(matcher)
                ? null
                : (sourceValue, configuredValue) ->
                matcher.matches((SV) sourceValue, (CV) configuredValue);
        PriorityMatchType type = adaptedMatcher == null
                ? PriorityMatchType.COMMON : PriorityMatchType.BOOLEAN;
        return new PriorityMatchFunction<>(name, priority, type,
                adaptedSourceGetter, adaptedConfigGetter,
                adaptedIndexConfigPreparer, adaptedMatcher);
    }


    /**
     * 匹配对应key
     * @param source 资源
     * @param kListSupplier 批量存在的key值，用于模糊匹配的场景，可以为空
     * @return 匹配K
     */
    public List<K> matchSource(S source, Supplier<Collection<K>> kListSupplier) {
        if (source == null) {
            return Collections.emptyList();
        }
        return matchSourceValue(sourceGetter.apply(source), kListSupplier);
    }

    List<K> matchSourceValue(K sourceKey, Supplier<Collection<K>> kListSupplier) {
        if (sourceKey == null || Objects.equals("", sourceKey)){
            return Collections.emptyList();
        }
        // key匹配模式存在，走key匹配模式
        if (keyMatchFunction != null){
            List<K> keyList = new ArrayList<>();
            for (K k : kListSupplier.get()) {
                if (keyMatchFunction.test(sourceKey, k)){
                    keyList.add(k);
                }
            }
            return keyList;
        }
        return Collections.singletonList(sourceKey);
    }

    /**
     * Matches a source value against internal index keys. Index keys use
     * {@code Object} deliberately so configuration preparation never pretends
     * that a wrapper is the caller's concrete K type.
     */
    @SuppressWarnings("unchecked")
    List<Object> matchSourceIndexValue(K sourceKey,
                                       Supplier<Collection<Object>> keySupplier) {
        if (sourceKey == null || Objects.equals("", sourceKey)) {
            return Collections.emptyList();
        }
        if (keyMatchFunction != null) {
            List<Object> keys = new ArrayList<>();
            for (Object key : keySupplier.get()) {
                if (keyMatchFunction.test(sourceKey, (K) key)) {
                    keys.add(key);
                }
            }
            return keys;
        }
        return Collections.<Object>singletonList(sourceKey);
    }

    public K matchConfig(C config) {
        if (config == null) {
            return null;
        }
        return configGetter.apply(config);
    }

    Object prepareIndexConfigValue(K configuredValue) {
        return indexConfigPreparer.apply(configuredValue);
    }

    public PriorityMatchType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public Function<S, K> getSourceGetter() {
        return sourceGetter;
    }

    public Function<C, K> getConfigGetter() {
        return configGetter;
    }


    public Integer getPriority() {
        return priority;
    }

    @Override
    public int hashCode() {
        return Objects.hash(uniqueId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PriorityMatchFunction<?, ?, ?> matchFunction = (PriorityMatchFunction<?, ?, ?>) o;
        return uniqueId.equals(matchFunction.uniqueId);
    }

    @Override
    public String toString() {
        return "PriorityMatchFunction{" +
                "name='" + name + '\'' +
                ", uniqueId='" + uniqueId + '\'' +
                '}';
    }
}
