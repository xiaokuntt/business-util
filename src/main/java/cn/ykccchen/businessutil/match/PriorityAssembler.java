package cn.ykccchen.businessutil.match;

import cn.ykccchen.businessutil.match.handler.PriorityHandler;
import cn.ykccchen.businessutil.match.handler.PriorityMode;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * 优先级处理器，用作加载全部处理逻辑。
 *
 * @author ykccchen
 * @version 1.0
 * @since 1.0
 */
public class PriorityAssembler<S, C, K> {

    private final List<PriorityMatchFunction<S, C, K>> priorityList;
    private List<C> configList;
    private PriorityHandler priorityHandler;
    private DuplicateKeyCheckLevel duplicateKeyCheckLevel;
    private PriorityNameAndValueHandler<S, C, K> priorityNameAndValueHandler;

    private PriorityAssembler(PriorityHandler priorityHandler) {
        this.priorityList = new ArrayList<>();
        this.priorityHandler = priorityHandler;
        this.duplicateKeyCheckLevel = DuplicateKeyCheckLevel.OFF;
        this.priorityNameAndValueHandler = new DefaultPriorityNameAndValueHandler<>();
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
        if (configList == null) {
            throw new IllegalArgumentException("PriorityAssembler config list cannot be null");
        }
        this.configList = new ArrayList<>(configList);
        return this;
    }

    public PriorityAssembler<S, C, K> initPriorityHandler(PriorityHandler priorityHandler) {
        if (priorityHandler == null) {
            throw new IllegalArgumentException("Priority handler cannot be null");
        }
        this.priorityHandler = priorityHandler;
        return this;
    }

    /**
     * Configures duplicate complete-key validation for {@link #create()}.
     *
     * @param level OFF, WARNING, or EXCEPTION
     * @return this assembler
     */
    public PriorityAssembler<S, C, K> initDuplicateKeyCheck(DuplicateKeyCheckLevel level) {
        if (level == null) {
            throw new IllegalArgumentException("Duplicate key check level cannot be null");
        }
        this.duplicateKeyCheckLevel = level;
        return this;
    }

    /**
     * Configures how matched dimensions are rendered in result NAME:VALUE metadata.
     *
     * @param handler caller implementation used for each matched dimension
     * @return this assembler
     */
    public PriorityAssembler<S, C, K> initPriorityNameAndValueHandler(
            PriorityNameAndValueHandler<S, C, K> handler) {
        if (handler == null) {
            throw new IllegalArgumentException("Priority name and value handler cannot be null");
        }
        this.priorityNameAndValueHandler = handler;
        return this;
    }

    public PriorityAssembler<S, C, K> add(PriorityMatchFunction<S, C, K> matchFunction) {
        if (matchFunction == null) {
            throw new IllegalArgumentException("Priority match function cannot be null");
        }
        int expectedPriority = priorityList.size();
        if (!Objects.equals(expectedPriority, matchFunction.getPriority())) {
            throw new IllegalArgumentException("Priority must equal its zero-based registration index: expected "
                    + expectedPriority + " but was " + matchFunction.getPriority());
        }
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
        this.priorityList.add(PriorityMatchFunction.ofBoolean(name, priorityList.size(),
                sourceGetter, configGetter, keyMatchFunction));
        return this;
    }

    public PriorityAssembler<S, C, K> addPriorityMatchFunction(Function<S, K> sourceGetter,
                                                               Function<C, K> configGetter,
                                                               BiPredicate<K, K> keyMatchFunction) {
        return addPriorityMatchFunction(null, sourceGetter, configGetter, keyMatchFunction);
    }

    /**
     * Adds a rule whose source and configured values may have different types.
     *
     * <p>The distinct method name intentionally avoids ambiguity with the legacy
     * {@link BiPredicate} overload when callers use a lambda.</p>
     *
     * @param name dimension name
     * @param sourceGetter source-value extractor
     * @param configGetter configured-value extractor
     * @param matcher typed matcher, receiving source then configured value
     * @param <SV> source-value type
     * @param <CV> configured-value type
     * @return this assembler
     */
    public <SV extends K, CV extends K> PriorityAssembler<S, C, K> addPriorityMatcher(
            String name,
            Function<S, SV> sourceGetter,
            Function<C, CV> configGetter,
            PriorityMatcher<SV, CV> matcher) {
        priorityList.add(PriorityMatchFunction.<S, C, K, SV, CV>ofMatcher(
                name, priorityList.size(), sourceGetter, configGetter, matcher));
        return this;
    }

    public <SV extends K, CV extends K> PriorityAssembler<S, C, K> addPriorityMatcher(
            Function<S, SV> sourceGetter,
            Function<C, CV> configGetter,
            PriorityMatcher<SV, CV> matcher) {
        return addPriorityMatcher(null, sourceGetter, configGetter, matcher);
    }

    public PriorityFetcher<S, C, K> create() {
        if (configList == null) {
            throw new IllegalStateException("PriorityAssembler config list must be initialized before create()");
        }
        return PriorityFetcher.from(priorityHandler, configList, priorityList, duplicateKeyCheckLevel,
                priorityNameAndValueHandler);
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
