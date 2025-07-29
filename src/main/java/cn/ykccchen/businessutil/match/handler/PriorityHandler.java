package cn.ykccchen.businessutil.match.handler;

import cn.ykccchen.businessutil.match.PriorityMatchFunction;
import cn.ykccchen.businessutil.match.PriorityMatchProcessor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * @author ykccchen
 * @version 1.0
 * @description 优先级处理
 * @date 2025/5/29 13:50
 */
public interface PriorityHandler {

    /**
     * 基于优先级算法处理方案
     * 转换为处理过后的优先级集合
     *
     * @param priorityMatchFunctionList 基本字段优先级
     * @return 汇总后的优先级，
     */
    <S, C, K> List<PriorityMatchProcessor<S, C, K>> initPriorityHandlerList(List<PriorityMatchFunction<S, C, K>> priorityMatchFunctionList);

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