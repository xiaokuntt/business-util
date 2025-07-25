package com.ykccchen.businessutil.match.handler;

import com.ykccchen.businessutil.match.PriorityMatchFunction;
import com.ykccchen.businessutil.match.PriorityMatchProcessor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author ykccchen
 * @version 1.0
 * @description 配置优先级枚举类
 * @date 2025/5/29 13:38
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
                combine.sort((v1, v2) -> {
                    int i1 = 0;
                    for (PriorityMatchFunction<S, C, K> function : v1) {
                        i1 = i1 * 10 + (priorityMatchFunctionList.size() - function.getPriority());
                    }
                    Integer i2 = 0;
                    for (PriorityMatchFunction<S, C, K> function : v2) {
                        i2 = i2 * 10 + (priorityMatchFunctionList.size() - function.getPriority());
                    }
                    return i2.compareTo(i1);
                });
                functionList.addAll(combine.stream().map(PriorityMatchProcessor::new).collect(Collectors.toList()));
            }
            return functionList;
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
            functionList.sort((v1, v2) -> {
                int i1 = 0;
                for (PriorityMatchFunction<S, C, K> function : v1.getPriorityMatchFunctionList()) {
                    i1 += 1 << (priorityMatchFunctionList.size() - function.getPriority());
                }
                Integer i2 = 0;
                for (PriorityMatchFunction<S, C, K> function : v2.getPriorityMatchFunctionList()) {
                    i2 += 1 << (priorityMatchFunctionList.size() - function.getPriority());
                }
                return i2.compareTo(i1);
            });
            return functionList;
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


}
