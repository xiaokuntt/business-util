package cn.ykccchen.businessutil.match;

import java.util.*;

/**
 * 优先级处理器的前缀树。
 *
 * @author ykccchen
 * @version 1.0
 * @since 1.0
 */
public class PriorityMatchProcessorTree<S, C, K> {

    private final int level;
    /**
     * 顶层为空
     */
    private final PriorityMatchFunction<S, C, K> node;
    private final Map<String, PriorityMatchProcessorTree<S, C, K>> priorityMatchFunctionTree;

    public static  <S, C, K>  PriorityMatchProcessorTree<S, C, K>  build(List<PriorityMatchProcessor<S, C, K>> priorityMatchProcessorList) {
        PriorityMatchProcessorTree<S, C, K> tree = new PriorityMatchProcessorTree<>(0, null);
        // 初始化头
        PriorityMatchProcessorTree<S, C, K> currentPriorityMatchProcessorTree;
        for (PriorityMatchProcessor<S, C, K> priorityMatchProcessor : priorityMatchProcessorList) {
            currentPriorityMatchProcessorTree = tree;
            List<PriorityMatchFunction<S, C, K>> priorityMatchFunctionList = priorityMatchProcessor.getPriorityMatchFunctionList();
            for (PriorityMatchFunction<S, C, K> priorityMatchFunction : priorityMatchFunctionList) {
                currentPriorityMatchProcessorTree = currentPriorityMatchProcessorTree.addChildNode(priorityMatchFunction);
            }
            // 特殊步骤，自己加入自己，这个是为了处理自己就是叶子节点，但是还有枝叶的情况
            currentPriorityMatchProcessorTree.addChildNode(priorityMatchFunctionList.get(priorityMatchFunctionList.size() - 1));
        }
        return tree;
    }
    public PriorityMatchProcessorTree(int level,
                                      PriorityMatchFunction<S, C, K> node) {
        this.level = level;
        this.priorityMatchFunctionTree = new LinkedHashMap<>();
        this.node = node;
    }

    public PriorityMatchProcessorTree<S, C, K> addChildNode(PriorityMatchFunction<S, C, K> priorityMatchFunction) {
        PriorityMatchProcessorTree<S, C, K> sckPriorityMatchProcessorTree = priorityMatchFunctionTree.get(priorityMatchFunction.getUniqueId());
        if (sckPriorityMatchProcessorTree == null) {
            sckPriorityMatchProcessorTree = new PriorityMatchProcessorTree<>(level + 1, priorityMatchFunction);
            priorityMatchFunctionTree.put(priorityMatchFunction.getUniqueId(), sckPriorityMatchProcessorTree);
        }
        return sckPriorityMatchProcessorTree;
    }

    public boolean isTop() {
        return level == 1;
    }
    public boolean isBottom() {
        return priorityMatchFunctionTree.isEmpty();
    }



    public PriorityMatchFunction<S, C, K> getFunctionNode() {
        return node;
    }

    public Map<String, PriorityMatchProcessorTree<S, C, K>> getPriorityMatchFunctionTree() {
        return priorityMatchFunctionTree;
    }


}
