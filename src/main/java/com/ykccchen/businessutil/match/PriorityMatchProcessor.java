package com.ykccchen.businessutil.match;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author ykccchen
 * @version 1.0
 * @description 优先级处理程序，处理优先级匹配函数列表，表示这是一个水平配置上的全部处理流程
 * @date 2025/7/20 21:43
 */
public class PriorityMatchProcessor<S, C, K> {
    private final String name;

    private final String uniqueId;

    private final List<PriorityMatchFunction<S, C, K>> priorityMatchFunctionList;

    public PriorityMatchProcessor(List<PriorityMatchFunction<S, C, K>> priorityMatchFunctionList) {
        this.priorityMatchFunctionList = priorityMatchFunctionList;
        this.name = priorityMatchFunctionList
                .stream()
                .map(PriorityMatchFunction::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("_"));
        this.uniqueId = initUniqueId(priorityMatchFunctionList);
    }

    public static <S, C, K> String initUniqueId(List<PriorityMatchFunction<S, C, K>> priorityMatchFunctionList){
        return priorityMatchFunctionList
                .stream()
                .map(PriorityMatchFunction::getUniqueId)
                .collect(Collectors.joining("_"));
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
        PriorityMatchProcessor<S, C, K> priorityMatchProcessor = (PriorityMatchProcessor<S, C, K>) o;
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
