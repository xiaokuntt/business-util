package cn.ykccchen.businessutil.match.handler;

import cn.ykccchen.businessutil.match.PriorityMatchFunction;
import cn.ykccchen.businessutil.match.PriorityMatchProcessor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 内置的配置优先级排序模式。
 *
 * @author ykccchen
 * @version 1.0
 * @since 1.0
 */
public enum PriorityMode implements PriorityHandler {


    /**
     * A B C D 4个配置维度
     * A C D 优先级大于 A B
     * A B C  优先级大于 A B D
     * 3个维度一定大于2个维度
     */
    NUMBER_OF_MATCHES("NUMBER_OF_MATCHES", "配置数量优先") {
        @Override
        public <S, C, K> List<PriorityMatchProcessor<S, C, K>> initPriorityHandlerList(List<PriorityMatchFunction<S, C, K>> priorityMatchFunctionList) {
            List<PriorityMatchProcessor<S, C, K>> functionList = new ArrayList<>();
            for (int size = priorityMatchFunctionList.size(); size > 0; size--) {
                List<List<PriorityMatchFunction<S, C, K>>> combine = combine(priorityMatchFunctionList.size(), size, priorityMatchFunctionList);
                functionList.addAll(combine.stream().map(PriorityMatchProcessor::new).collect(Collectors.toList()));
            }
            functionList.sort(PriorityMode::compareNumberOfMatches);
            return functionList;
        }

        @Override
        public <S, C, K> List<PriorityMatchProcessor<S, C, K>> initPriorityHandlerList(
                List<PriorityMatchFunction<S, C, K>> priorityMatchFunctionList,
                Collection<List<PriorityMatchFunction<S, C, K>>> activeFunctionLists) {
            List<PriorityMatchProcessor<S, C, K>> processors = toProcessors(activeFunctionLists);
            processors.sort(PriorityMode::compareNumberOfMatches);
            return processors;
        }
    },
    /**
     * A B C D 4个配置维度
     * A B  优先级大于 A C D
     * A B C  优先级大于 A B
     * A B C  优先级大于 A B D
     * 绝对价值维度
     */
    ABSOLUTE_VALUE("ABSOLUTE_VALUE", "绝对价值优先") {
        @Override
        public <S, C, K> List<PriorityMatchProcessor<S, C, K>> initPriorityHandlerList(List<PriorityMatchFunction<S, C, K>> priorityMatchFunctionList) {
            List<PriorityMatchProcessor<S, C, K>> functionList = new ArrayList<>();
            for (int size = priorityMatchFunctionList.size(); size > 0; size--) {
                List<List<PriorityMatchFunction<S, C, K>>> combine = combine(priorityMatchFunctionList.size(), size, priorityMatchFunctionList);
                functionList.addAll(combine.stream().map(PriorityMatchProcessor::new).collect(Collectors.toList()));
            }
            functionList.sort(PriorityMode::compareAbsoluteValue);
            return functionList;
        }

        @Override
        public <S, C, K> List<PriorityMatchProcessor<S, C, K>> initPriorityHandlerList(
                List<PriorityMatchFunction<S, C, K>> priorityMatchFunctionList,
                Collection<List<PriorityMatchFunction<S, C, K>>> activeFunctionLists) {
            List<PriorityMatchProcessor<S, C, K>> processors = toProcessors(activeFunctionLists);
            processors.sort(PriorityMode::compareAbsoluteValue);
            return processors;
        }
    },
    ;


    private final String model;
    private final String desc;


    public String getModel() {
        return model;
    }

    public String getDesc() {
        return desc;
    }

    PriorityMode(String model, String desc) {
        this.model = model;
        this.desc = desc;
    }

    private static <S, C, K> List<PriorityMatchProcessor<S, C, K>> toProcessors(
            Collection<List<PriorityMatchFunction<S, C, K>>> activeFunctionLists) {
        Map<String, PriorityMatchProcessor<S, C, K>> uniqueProcessors = new LinkedHashMap<>();
        for (List<PriorityMatchFunction<S, C, K>> functionList : activeFunctionLists) {
            PriorityMatchProcessor<S, C, K> processor = new PriorityMatchProcessor<>(functionList);
            uniqueProcessors.putIfAbsent(processor.getUniqueId(), processor);
        }
        return new ArrayList<>(uniqueProcessors.values());
    }

    private static <S, C, K> int compareNumberOfMatches(PriorityMatchProcessor<S, C, K> left,
                                                         PriorityMatchProcessor<S, C, K> right) {
        int sizeComparison = Integer.compare(right.getFunctionSize(), left.getFunctionSize());
        return sizeComparison != 0 ? sizeComparison : comparePrioritySequence(left, right);
    }

    /**
     * Compares the presence vector from the highest-value dimension to the lowest.
     * This is equivalent to binary weighting without fixed-width integer overflow.
     */
    private static <S, C, K> int compareAbsoluteValue(PriorityMatchProcessor<S, C, K> left,
                                                       PriorityMatchProcessor<S, C, K> right) {
        List<PriorityMatchFunction<S, C, K>> leftFunctions = left.getPriorityMatchFunctionList();
        List<PriorityMatchFunction<S, C, K>> rightFunctions = right.getPriorityMatchFunctionList();
        int leftIndex = 0;
        int rightIndex = 0;
        while (leftIndex < leftFunctions.size() || rightIndex < rightFunctions.size()) {
            int leftPriority = leftIndex < leftFunctions.size()
                    ? leftFunctions.get(leftIndex).getPriority() : Integer.MAX_VALUE;
            int rightPriority = rightIndex < rightFunctions.size()
                    ? rightFunctions.get(rightIndex).getPriority() : Integer.MAX_VALUE;
            if (leftPriority != rightPriority) {
                return Integer.compare(leftPriority, rightPriority);
            }
            leftIndex++;
            rightIndex++;
        }
        return 0;
    }

    private static <S, C, K> int comparePrioritySequence(PriorityMatchProcessor<S, C, K> left,
                                                          PriorityMatchProcessor<S, C, K> right) {
        List<PriorityMatchFunction<S, C, K>> leftFunctions = left.getPriorityMatchFunctionList();
        List<PriorityMatchFunction<S, C, K>> rightFunctions = right.getPriorityMatchFunctionList();
        for (int index = 0; index < Math.min(leftFunctions.size(), rightFunctions.size()); index++) {
            int comparison = Integer.compare(leftFunctions.get(index).getPriority(),
                    rightFunctions.get(index).getPriority());
            if (comparison != 0) {
                return comparison;
            }
        }
        return Integer.compare(rightFunctions.size(), leftFunctions.size());
    }


}
