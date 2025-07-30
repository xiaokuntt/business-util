package cn.ykccchen.businessutil.match;

import cn.ykccchen.businessutil.match.handler.PriorityMatchType;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author ykccchen
 * @version 1.0
 * @description
 * @date 2025/7/17 13:44
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
     * 资源K与配置K是否匹配
     */
    private final BiPredicate<K, K> keyMatchFunction;


    private PriorityMatchFunction(String name,
                                  Integer priority,
                                  PriorityMatchType type,
                                  Function<S, K> sourceGetter,
                                  Function<C, K> configGetter,
                                  BiPredicate<K, K> keyMatchFunction) {
        if (priority < 0) {
            throw new UnsupportedOperationException("Priority must be greater than to 0");
        }
        this.name = name;
        this.type = type;
        this.uniqueId = UUID.randomUUID().toString();
        this.priority = priority;
        this.sourceGetter = sourceGetter;
        this.configGetter = configGetter;
        this.keyMatchFunction = keyMatchFunction;
    }

    /**
     * 创建一个新的数据键。
     *
     * @param name 键的描述性名称，方便调试
     */
    public static <S, C, K> PriorityMatchFunction<S, C, K> of(String name,
                                                              Integer priority,
                                                              Function<S, K> sourceGetter,
                                                              Function<C, K> configGetter) {
        return new PriorityMatchFunction<>(name, priority, PriorityMatchType.COMMON, sourceGetter, configGetter, null);
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
        return new PriorityMatchFunction<>(name, priority, PriorityMatchType.BOOLEAN, sourceGetter, configGetter, keyMatchFunction);
    }

    public static <S, C, K> PriorityMatchFunction<S, C, K> ofBoolean(Integer priority,
                                                                     Function<S, K> sourceGetter,
                                                                     Function<C, K> configGetter,
                                                                     BiPredicate<K, K> keyMatchFunction) {
        return PriorityMatchFunction.ofBoolean(null, priority, sourceGetter, configGetter, keyMatchFunction);
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
        K sourceKey = sourceGetter.apply(source);
        if (sourceKey == null){
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

    public K matchConfig(C config) {
        if (config == null) {
            return null;
        }
        return configGetter.apply(config);
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
        PriorityMatchFunction<S, C, K> matchFunction = (PriorityMatchFunction<S, C, K>) o;
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
