package com.ykccchen.businessutil.match;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/**
 * @author ykccchen
 * @version 1.0
 * @description
 * @date 2025/7/17 13:44
 */
public class PriorityMatchFunction<S,C,K> {

    private final String name;
    private final String uniqueId;

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

    // 保证每个key实例的唯一性
    private PriorityMatchFunction(String name,
                                  Integer priority,
                                  Function<S, K> sourceGetter,
                                  Function<C, K> configGetter) {
        if (priority < 0){
            throw new UnsupportedOperationException("Priority must be greater than to 0");
        }
        this.name = name;
        this.uniqueId = UUID.randomUUID().toString();
        this.priority = priority;
        this.sourceGetter = sourceGetter;
        this.configGetter = configGetter;
    }
    /**
     * 创建一个新的数据键。
     * @param name 键的描述性名称，方便调试
     */
    public static <S,C,K> PriorityMatchFunction<S,C,K> of(String name,
                                                      Integer priority,
                                                      Function<S, K> sourceGetter,
                                                      Function<C, K> configGetter) {
        return new PriorityMatchFunction<>(name, priority, sourceGetter, configGetter);
    }

    public static <S,C,K> PriorityMatchFunction<S,C,K> of(Integer priority,
                                                      Function<S, K> sourceGetter,
                                                      Function<C, K> configGetter) {
        return PriorityMatchFunction.of(null, priority, sourceGetter, configGetter);
    }

    public  K matchSource(S source) {
        if (source == null ) {
            return null;
        }
        return sourceGetter.apply(source);
    }
    public K matchConfig(C config) {
        if (config == null) {
            return null;
        }
        return configGetter.apply(config);
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
        PriorityMatchFunction<S,C,K> matchFunction = (PriorityMatchFunction<S,C,K>) o;
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
