package cn.ykccchen.businessutil.match.handler;

import cn.ykccchen.businessutil.match.PriorityMatchFunction;
import cn.ykccchen.businessutil.match.PriorityMatchProcessor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 定义处理器组合的优先级排序策略。
 *
 * @author ykccchen
 * @version 1.0
 * @since 1.0
 */
public interface PriorityHandler {

    /**
     * 基于优先级算法处理方案
     * 转换为处理过后的优先级集合
     *
     * @param priorityMatchFunctionList 基本字段优先级
     * @param <S> 需求类型
     * @param <C> 配置类型
     * @param <K> 匹配键类型
     * @return 汇总后的优先级，
     */
    <S, C, K> List<PriorityMatchProcessor<S, C, K>> initPriorityHandlerList(List<PriorityMatchFunction<S, C, K>> priorityMatchFunctionList);

    /**
     * Orders only processor shapes that are present in the loaded configuration.
     *
     * <p>The default implementation preserves compatibility with custom handlers by
     * delegating to the original full-list method and filtering its result. Built-in
     * handlers override this method so they never materialize the complete power set.</p>
     *
     * @param priorityMatchFunctionList all registered functions
     * @param activeFunctionLists       distinct, non-empty function shapes in configuration
     * @param <S> source type
     * @param <C> configuration type
     * @param <K> key type
     * @return active processors in matching priority order
     */
    default <S, C, K> List<PriorityMatchProcessor<S, C, K>> initPriorityHandlerList(
            List<PriorityMatchFunction<S, C, K>> priorityMatchFunctionList,
            Collection<List<PriorityMatchFunction<S, C, K>>> activeFunctionLists) {
        if (activeFunctionLists.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> activeIds = activeFunctionLists.stream()
                .map(PriorityMatchProcessor::initUniqueId)
                .collect(Collectors.toCollection(HashSet::new));
        List<PriorityMatchProcessor<S, C, K>> processors = initPriorityHandlerList(priorityMatchFunctionList);
        if (processors == null) {
            throw new IllegalArgumentException("Priority handler returned a null processor list");
        }
        return processors.stream()
                .filter(processor -> {
                    if (processor == null) {
                        throw new IllegalArgumentException("Priority handler returned a null processor");
                    }
                    return activeIds.contains(processor.getUniqueId());
                })
                .collect(Collectors.toList());
    }

    default <S, C, K> List<List<PriorityMatchFunction<S, C, K>>> combine(int arrSize,
                                                                         int combineSize,
                                                                         List<PriorityMatchFunction<S, C, K>> list) {
        List<List<PriorityMatchFunction<S, C, K>>> res = new ArrayList<>();
        if (combineSize <= 0 || arrSize < combineSize) {
            res.add(list);
            return res;
        }
        // 从 1 开始是题目的设定
        Deque<PriorityMatchFunction<S, C, K>> path = new ArrayDeque<>();
        dfs(arrSize, combineSize, 1, path, res, list);
        return res;
    }

    default <S, C, K> void dfs(int arrSize,
                               int combineSize,
                               int begin,
                               Deque<PriorityMatchFunction<S, C, K>> path,
                               List<List<PriorityMatchFunction<S, C, K>>> res,
                               List<PriorityMatchFunction<S, C, K>> list) {
        // 递归终止条件是：path 的长度等于 k
        if (path.size() == combineSize) {
            res.add(new ArrayList<>(path));
            return;
        }

        // 遍历可能的搜索起点
        for (int i = begin; i <= arrSize; i++) {
            // 向路径变量里添加一个数
            path.addLast(list.get(i - 1));
            // 下一轮搜索，设置的搜索起点要加 1，因为组合数理不允许出现重复的元素
            dfs(arrSize, combineSize, i + 1, path, res, list);
            // 重点理解这里：深度优先遍历有回头的过程，因此递归之前做了什么，递归之后需要做相同操作的逆向操作
            path.removeLast();
        }
    }
}
