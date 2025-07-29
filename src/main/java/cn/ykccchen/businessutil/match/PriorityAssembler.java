package cn.ykccchen.businessutil.match;

import cn.ykccchen.businessutil.match.handler.PriorityHandler;
import cn.ykccchen.businessutil.match.handler.PriorityMode;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * @author ykccchen
 * @version 1.0
 * @description 优先级处理器，用作加载全部处理逻辑
 * @date 2025/7/17 13:59
 */
public class PriorityAssembler<S, C, K> {

    private final List<PriorityMatchFunction<S, C, K>> priorityList;
    private List<C> configList;
    private PriorityHandler priorityHandler;

    private PriorityAssembler(PriorityHandler priorityHandler) {
        this.priorityList = new ArrayList<>();
        this.priorityHandler = priorityHandler;
    }

    public static <S, C, K> PriorityAssembler<S, C, K> from(Class<S> s,
                                                            Class<C> c,
                                                            Class<K> k) {
        return new PriorityAssembler<>(PriorityMode.NUMBER_OF_MATCHES);
    }

    public static <S, C, K> PriorityAssembler<S, C, K> from(TypeReference<S> s,
                                                            TypeReference<C> c,
                                                            TypeReference<K> k) {
        return new PriorityAssembler<>(PriorityMode.NUMBER_OF_MATCHES);
    }

    public PriorityAssembler<S, C, K> initConfig(List<C> configList) {
        this.configList = configList;
        return this;
    }

    public PriorityAssembler<S, C, K> initPriorityHandler(PriorityHandler priorityHandler) {
        this.priorityHandler = priorityHandler;
        return this;
    }

    public PriorityAssembler<S, C, K> add(PriorityMatchFunction<S, C, K> matchFunction) {
        this.priorityList.add(matchFunction);
        return this;
    }

    public PriorityAssembler<S, C, K> addPriorityMatchFunction(String name,
                                                               Function<S, K> sourceGetter,
                                                               Function<C, K> configGetter) {
        this.priorityList.add(PriorityMatchFunction.of(name, priorityList.size(), sourceGetter, configGetter));
        return this;
    }

    public PriorityAssembler<S, C, K> addPriorityMatchFunction(Function<S, K> sourceGetter,
                                                               Function<C, K> configGetter) {
        return addPriorityMatchFunction(null, sourceGetter, configGetter);
    }

    public PriorityAssembler<S, C, K> addPriorityMatchFunction(String name,
                                                               Function<S, K> sourceGetter,
                                                               Function<C, K> configGetter,
                                                               BiPredicate<K, K> keyMatchFunction) {
        if (keyMatchFunction != null) {
            this.priorityList.add(PriorityMatchFunction.ofBoolean(name, priorityList.size(), sourceGetter, configGetter, keyMatchFunction));
        } else {
            addPriorityMatchFunction(name, sourceGetter, configGetter);
        }
        return this;
    }

    public PriorityAssembler<S, C, K> addPriorityMatchFunction(Function<S, K> sourceGetter,
                                                               Function<C, K> configGetter,
                                                               BiPredicate<K, K> keyMatchFunction) {
        return addPriorityMatchFunction(null, sourceGetter, configGetter, keyMatchFunction);
    }

    public PriorityFetcher<S, C, K> create() {
        if (configList == null) {
            throw new NullPointerException("PriorityAssembler Config list cannot be null!");
        }
        List<PriorityMatchProcessor<S, C, K>> processorList = priorityHandler.initPriorityHandlerList(priorityList);
        return PriorityFetcher
                .from(processorList, configList, priorityList)
                .pruning();
    }

    public abstract static class TypeReference<T> {
        private final Type type;

        protected TypeReference() {
            Type superClass = getClass().getGenericSuperclass();
            if (superClass instanceof Class) {
                throw new IllegalArgumentException("TypeReference must be parameterized");
            }
            this.type = ((ParameterizedType) superClass).getActualTypeArguments()[0];
        }

        public Type getType() {
            return type;
        }
    }

}
