package cn.ykccchen.businessutil.match;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author ykccchen
 * @version 1.0
 * @description priority get the executor
 * @date 2025/7/17 13:59
 */
public class PriorityFetcher<S, C, K> {

    private final PriorityMatchTree<S, C, K>[] tree;
    private List<PriorityMatchProcessor<S, C, K>> processorList;

    /**
     * 优先级树
     */
    private PriorityMatchProcessorTree<S, C, K> priorityMatchProcessorTree;

    private boolean useTreePriority = false;

    /**
     * Processor使用记录，uniqueId为标识
     * 用于剪枝\ 配置加载统计
     */
    private final Map<String, Integer> useRecordMap;

    private PriorityFetcher(List<PriorityMatchProcessor<S, C, K>> processorList,
                            List<PriorityMatchFunction<S, C, K>> prirotyList) {
        this.tree = new PriorityMatchTree[prirotyList.size()];
        for (int i = 0; i < prirotyList.size(); i++) {
            tree[i] = new PriorityMatchTree<>(1);
        }
        this.processorList = processorList;
        this.useRecordMap = new HashMap<>();
    }

    public PriorityMatchTree<S, C, K>[] getTree() {
        return tree;
    }

    public List<PriorityMatchProcessor<S, C, K>> getProcessorList() {
        return processorList;
    }

    /**
     * 需求匹配配置集，返回单优先级最高的配置集，可能是多个
     * 使用时要注意配置多的可能性
     *
     * @param source 需求信息
     * @return 单个优先级配置
     */
    public PriorityMatchResult<List<C>> match(S source) {
        List<PriorityMatchResult<List<C>>> match = match(source, false);
        return match.isEmpty() ? null : match.get(0);
    }

    /**
     * 需求匹配配置集，返回单优先级最高的配置集，可能是多个
     * 使用时要注意配置多的可能性
     *
     * @param source 需求信息
     * @return 单个优先级配置
     */
    public List<PriorityMatchResult<List<C>>> match(S source, boolean allPriority) {
        return useTreePriority ? matchTree(source, allPriority) : matchLevel(source, allPriority);
    }

    /**
     * 需求匹配配置集，返回单优先级最高的配置集，可能是多个
     * 使用时要注意配置多的可能性
     *
     * @param source      需求信息
     * @param allPriority 是否获取全部优先级
     * @return 单个优先级配置
     */
    private List<PriorityMatchResult<List<C>>> matchTree(S source, boolean allPriority) {
        List<PriorityMatchResult<List<C>>> matchResultList = new ArrayList<>();
        LinkedList<PriorityMatchFunction<S, C, K>> recordList = new LinkedList<>();
        for (PriorityMatchProcessorTree<S, C, K> value : priorityMatchProcessorTree.getPriorityMatchFunctionTree().values()) {
            PriorityMatchFunction<S, C, K> functionNode = value.getFunctionNode();
            recordList.add(functionNode);
            PriorityMatchTree<S, C, K> priorityMatchTree = tree[functionNode.getPriority()];
            List<K> kList = functionNode.matchSource(source, priorityMatchTree::getKeyList);
            for (PriorityMatchProcessorTree<S, C, K> childPriorityMatchFunctionTree : value.getPriorityMatchFunctionTree().values()) {
                if (childPriorityMatchFunctionTree.isBottom()) {
                    if (recursion(null, source, childPriorityMatchFunctionTree, priorityMatchTree, matchResultList, recordList, allPriority)) {
                        return matchResultList;
                    }
                }else if (!kList.isEmpty()){
                    for (K k : kList) {
                        if (recursion(k, source, childPriorityMatchFunctionTree, priorityMatchTree, matchResultList, recordList, allPriority)) {
                            return matchResultList;
                        }
                    }
                }
            }
            recordList.removeLast();
        }
        return matchResultList;
    }

    private boolean recursion(K k,
                           S source,
                           PriorityMatchProcessorTree<S, C, K> priorityMatchFunctionTree,
                           PriorityMatchTree<S, C, K> parentPriorityMatchTree,
                           List<PriorityMatchResult<List<C>>> matchResultList,
                           LinkedList<PriorityMatchFunction<S, C, K>> recordList,
                           boolean allPriority) {
        PriorityMatchFunction<S, C, K> functionNode = priorityMatchFunctionTree.getFunctionNode();

        if (priorityMatchFunctionTree.isBottom()) {
            List<K> kList = functionNode.matchSource(source, parentPriorityMatchTree::getConfigKeyList);
            for (K newK : kList) {
                List<C> configList = parentPriorityMatchTree.getConfigList(newK);
                if (!configList.isEmpty()) {
                    matchResultList.add(new PriorityMatchResult<>(PriorityMatchProcessor.initUniqueId(recordList),
                            PriorityMatchProcessor.initName(recordList),
                            parentPriorityMatchTree.getIndex(),
                            configList));
                    if (!allPriority) {
                        return true;
                    }
                }
            }
        } else {
            PriorityMatchTree<S, C, K> childTree = parentPriorityMatchTree.getChildTree(k, functionNode);
            if (childTree == null) {
                return false;
            }
            recordList.add(functionNode);
            List<K> kList = functionNode.matchSource(source, childTree::getKeyList);
            for (PriorityMatchProcessorTree<S, C, K> childPriorityMatchFunctionTree : priorityMatchFunctionTree.getPriorityMatchFunctionTree().values()) {
                if (childPriorityMatchFunctionTree.isBottom()) {
                    if (recursion(null, source, childPriorityMatchFunctionTree, childTree, matchResultList, recordList, allPriority)) {
                        return true;
                    }

                }else if (!kList.isEmpty()){
                    for (K newK : kList) {
                        if (recursion(newK, source, childPriorityMatchFunctionTree, childTree, matchResultList, recordList, allPriority)) {
                            return true;
                        }
                    }
                }
            }
            recordList.removeLast();
        }
        return false;
    }

    /**
     * 需求匹配配置集，返回单优先级最高的配置集，可能是多个
     * 使用时要注意配置多的可能性
     *
     * @param source 需求信息
     * @return 单个优先级配置
     */
    private List<PriorityMatchResult<List<C>>> matchLevel(S source, boolean allPriority) {
        List<PriorityMatchResult<List<C>>> allList = new ArrayList<>();
        // 逐层匹配
        for (PriorityMatchProcessor<S, C, K> priorityMatchProcessor : processorList) {
            List<PriorityMatchFunction<S, C, K>> priorityMatchFunctionList = priorityMatchProcessor.getPriorityMatchFunctionList();
            PriorityMatchFunction<S, C, K> functionHead = priorityMatchFunctionList.get(0);
            PriorityMatchTree<S, C, K> priorityMatchTree = tree[functionHead.getPriority()];
            List<K> kList;
            if (priorityMatchFunctionList.size() == 1) {
                kList = functionHead.matchSource(source, priorityMatchTree::getConfigKeyList);
            } else {
                kList = functionHead.matchSource(source, priorityMatchTree::getKeyList);
            }
            // 没匹配上退出当前循环
            if (kList.isEmpty()) {
                continue;
            }
            // 判断是否有多层
            if (priorityMatchFunctionList.size() > 1) {
                for (K k : kList) {
                    if (recursionLevel(k, source, priorityMatchProcessor, 1, priorityMatchTree, allList, allPriority)) {
                        return allList;
                    }
                }
            }else {
                for (K k : kList) {
                    // 该位置代码执行说明找到了叶子节点
                    List<C> configList = priorityMatchTree.getConfigList(k);
                    if (!configList.isEmpty()) {
                        allList.add(new PriorityMatchResult<>(priorityMatchProcessor.getUniqueId(),
                                priorityMatchProcessor.getName(),
                                priorityMatchTree.getIndex(),
                                configList));
                        if (!allPriority) {
                            // 如果不需要全部优先级配置，直接返回
                            return allList;
                        }
                    }
                }
            }

        }
        return allList;
    }
    private boolean recursionLevel(K k,
                           S source,
                           PriorityMatchProcessor<S, C, K> priorityMatchProcessor,
                           int index,
                           PriorityMatchTree<S, C, K> parentPriorityMatchTree,
                           List<PriorityMatchResult<List<C>>> matchResultList,
                           boolean allPriority) {
        PriorityMatchFunction<S, C, K> functionNode = priorityMatchProcessor.getPriorityMatchFunctionList().get(index);
        PriorityMatchTree<S, C, K> childTree = parentPriorityMatchTree.getChildTree(k, functionNode);
        if (childTree == null) {
            return false;
        }
        if (priorityMatchProcessor.getFunctionSize() == index + 1) {
            List<K> kList = functionNode.matchSource(source, childTree::getConfigKeyList);
            if (!kList.isEmpty()) {
                for (K newK : kList) {
                    List<C> configList = childTree.getConfigList(newK);
                    if (!configList.isEmpty()) {
                        matchResultList.add(new PriorityMatchResult<>(priorityMatchProcessor.getUniqueId(),
                                priorityMatchProcessor.getName(),
                                childTree.getIndex(),
                                configList));
                        if (!allPriority) {
                            return true;
                        }
                    }
                }

            }
        } else {

            List<K> kList = functionNode.matchSource(source, childTree::getKeyList);
            if (!kList.isEmpty()) {
                for (K newK : kList) {
                    if (recursionLevel(newK, source, priorityMatchProcessor, index + 1, childTree, matchResultList, allPriority)) {
                        return true;
                    }

                }
            }
        }
        return false;
    }

    public void useRecordCount(String id) {
        useRecordMap.put(id, useRecordMap.getOrDefault(id, 0) + 1);
    }

    /**
     * 基础的初始化逻辑
     *
     * @param prirotyList 配置优先级集合
     * @param configList
     * @param <S>
     * @param <C>
     * @param <K>
     * @return
     */
    public static <S, C, K> PriorityFetcher<S, C, K> from(List<PriorityMatchProcessor<S, C, K>> processorList,
                                                          List<C> configList,
                                                          List<PriorityMatchFunction<S, C, K>> prirotyList) {
        // 初始化最终对象
        PriorityFetcher<S, C, K> priorityFetcher = new PriorityFetcher<>(processorList, prirotyList);
        // 循环配置，设置key匹配情况
        for (C config : configList) {
            K k = null;
            PriorityMatchFunction<S, C, K> functionHead = null;
            PriorityMatchTree<S, C, K> priorityMatchTree = null;
            List<PriorityMatchFunction<S, C, K>> usePriorityMatchFunctionList = new ArrayList<>(prirotyList.size());
            for (PriorityMatchFunction<S, C, K> priorityMatchFunction : prirotyList) {
                K newK = priorityMatchFunction.matchConfig(config);
                // 如果为空说明该路由不匹配, 应该匹配其他场景的优先级
                if (newK == null) {
                    continue;
                }
                usePriorityMatchFunctionList.add(priorityMatchFunction);
                // 初始化头
                if (functionHead == null) {
                    functionHead = priorityMatchFunction;
                    priorityMatchTree = priorityFetcher.getTree()[functionHead.getPriority()];
                    k = newK;
                    continue;
                }
                // 基于顶层k生成子树节点数据
                priorityMatchTree = priorityMatchTree.initChildTree(k, priorityMatchFunction, prirotyList.size());
                // 将K替换为子节点的K
                k = newK;
            }
            // 3.当前 priorityMatchTree 已经是叶子节点数据, 增加数据
            if (k != null) {
                priorityMatchTree.addConfig(k, config);
                // 记录使用
                priorityFetcher.useRecordCount(PriorityMatchProcessor.initUniqueId(usePriorityMatchFunctionList));
            }
        }

        return priorityFetcher;
    }

    /**
     * 剪枝，移除没有加载配置的集合信息
     *
     * @return 当前对象
     */
    public PriorityFetcher<S, C, K> pruning() {
        // 剪枝操作
        processorList = processorList.stream()
                .filter(v -> this.useRecordMap.containsKey(v.getUniqueId()))
                .collect(Collectors.toList());
        return this;
    }

    /**
     * 转换树处理
     *
     * @return 当前对象
     */
    public PriorityFetcher<S, C, K> tree() {
        // 剪枝操作
        this.useTreePriority = true;
        this.priorityMatchProcessorTree = PriorityMatchProcessorTree.build(this.processorList);
        return this;
    }

    /**
     * 优先级匹配树对象
     * [0]         [1]          [2]
     */
    static class PriorityMatchTree<S, C, K> {

        /**
         * 树索引，顶层为0
         */
        private final Integer index;

        /**
         * K: 当前树层级数据
         * V: 子数据集
         */
        private final Map<K, PriorityMatchTree<S, C, K>[]> currentTree;

        /**
         * 叶子节点才存在配置
         */
        private final Map<K, List<C>> configMap;

        PriorityMatchTree(Integer index) {
            this.index = index;
            this.currentTree = new LinkedHashMap<>();
            this.configMap = new LinkedHashMap<>();
        }

        public Integer getIndex() {
            return index;
        }

        public void addConfig(K k, C config) {
            this.configMap.computeIfAbsent(k, k1 -> new ArrayList<>()).add(config);
        }

        public Collection<K> getKeyList() {
            return currentTree.keySet();
        }

        public Collection<K> getConfigKeyList() {
            return configMap.keySet();
        }

        public List<C> getConfigList(K k) {
            return configMap.getOrDefault(k, Collections.emptyList());
        }


        public PriorityMatchTree<S, C, K> getChildTree(K k, PriorityMatchFunction<S, C, K> childFunction) {
            PriorityMatchTree<S, C, K>[] priorityMatchTrees = currentTree.get(k);
            if (priorityMatchTrees == null) {
                return null;
            }
            return priorityMatchTrees[childFunction.getPriority()];
        }

        public PriorityMatchTree<S, C, K> initChildTree(K k, PriorityMatchFunction<S, C, K> childFunction, int prioritySize) {
            PriorityMatchTree<S, C, K>[] childPriorityMatchTreeArr = currentTree.computeIfAbsent(k, k1 -> new PriorityMatchTree[prioritySize]);
            PriorityMatchTree<S, C, K> childPriorityMatchTree = childPriorityMatchTreeArr[childFunction.getPriority()];
            // 当前节点没有数据，就做初始化
            if (childPriorityMatchTree == null) {
                childPriorityMatchTree = new PriorityMatchTree<>(this.index + 1);
                childPriorityMatchTreeArr[childFunction.getPriority()] = childPriorityMatchTree;
            }

            return childPriorityMatchTree;
        }
    }
}
