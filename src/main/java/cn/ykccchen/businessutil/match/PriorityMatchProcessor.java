package cn.ykccchen.businessutil.match;

import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * 优先级处理程序，表示一个配置维度组合的完整处理流程。
 *
 * @author ykccchen
 * @version 1.0
 * @since 1.0
 */
public class PriorityMatchProcessor<S, C, K> {
    private final String name;

    private final String uniqueId;

    private final List<PriorityMatchFunction<S, C, K>> priorityMatchFunctionList;

    public PriorityMatchProcessor(List<PriorityMatchFunction<S, C, K>> priorityMatchFunctionList) {
        if (priorityMatchFunctionList == null || priorityMatchFunctionList.isEmpty()) {
            throw new IllegalArgumentException("Priority match processor requires at least one function");
        }
        if (priorityMatchFunctionList.contains(null)) {
            throw new IllegalArgumentException("Priority match processor functions cannot contain null");
        }
        this.priorityMatchFunctionList = Collections.unmodifiableList(new ArrayList<>(priorityMatchFunctionList));
        this.name = initName(this.priorityMatchFunctionList);
        this.uniqueId = initUniqueId(this.priorityMatchFunctionList);
    }

    public static <S, C, K> String initUniqueId(List<PriorityMatchFunction<S, C, K>> priorityMatchFunctionList){
        return priorityMatchFunctionList
                .stream()
                .map(PriorityMatchFunction::getUniqueId)
                .collect(Collectors.joining("_"));
    }
    public static <S, C, K> String initName(List<PriorityMatchFunction<S, C, K>> priorityMatchFunctionList){
        return priorityMatchFunctionList
                .stream()
                .map(PriorityMatchFunction::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("_"));
    }

    /**
     * Builds one complete NAME:VALUE path for a matched configuration.
     *
     * @param priorityMatchFunctionList active functions in priority order
     * @param source matching source
     * @param config matched configuration
     * @param sourceValues extracted source values aligned with the functions
     * @param matchedConfigValues indexed configuration values aligned with the functions
     * @param handler caller-supplied pair formatter
     * @param <S> source type
     * @param <C> configuration type
     * @param <K> match-key type
     * @return underscore-separated complete path, possibly empty
     */
    public static <S, C, K> String initNameAndValue(
            List<PriorityMatchFunction<S, C, K>> priorityMatchFunctionList,
            S source,
            C config,
            List<K> sourceValues,
            List<K> matchedConfigValues,
            PriorityNameAndValueHandler<S, C, K> handler) {
        if (priorityMatchFunctionList == null || sourceValues == null
                || matchedConfigValues == null || handler == null) {
            throw new IllegalArgumentException("Functions, source/config values, and handler cannot be null");
        }
        if (priorityMatchFunctionList.size() != sourceValues.size()
                || priorityMatchFunctionList.size() != matchedConfigValues.size()) {
            throw new IllegalArgumentException("Function and source/config value counts must be equal");
        }
        List<String> pairs = new ArrayList<>();
        for (int index = 0; index < priorityMatchFunctionList.size(); index++) {
            PriorityMatchFunction<S, C, K> function = priorityMatchFunctionList.get(index);
            String pair = handler.handle(function.getName(), function.getPriority(), source, config,
                    sourceValues.get(index), matchedConfigValues.get(index));
            if (pair != null) {
                pairs.add(pair);
            }
        }
        return String.join("_", pairs);
    }
    public int getFunctionSize() {
        return priorityMatchFunctionList.size();
    }

    public String getName() {
        return name;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public List<PriorityMatchFunction<S, C, K>> getPriorityMatchFunctionList() {
        return priorityMatchFunctionList;
    }


    @Override
    public int hashCode() {
        return Objects.hash(uniqueId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PriorityMatchProcessor<?, ?, ?> priorityMatchProcessor = (PriorityMatchProcessor<?, ?, ?>) o;
        return uniqueId.equals(priorityMatchProcessor.uniqueId);
    }

    @Override
    public String toString() {
        return "PriorityMatchProcessor{" +
                "name='" + name + '\'' +
                ", uniqueId='" + uniqueId + '\'' +
                '}';
    }
}
