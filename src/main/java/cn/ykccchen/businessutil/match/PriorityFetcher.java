package cn.ykccchen.businessutil.match;

import cn.ykccchen.businessutil.match.handler.PriorityHandler;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Executes priority-based configuration matching.
 *
 * @author ykccchen
 * @version 1.0
 * @since 1.0
 */
public class PriorityFetcher<S, C, K> {

    private static final Logger LOGGER = Logger.getLogger(PriorityFetcher.class.getName());

    private final PriorityMatchTree<S, C, K>[] tree;
    private List<PriorityMatchProcessor<S, C, K>> processorList;

    /**
     * 优先级树
     */
    private volatile PriorityMatchProcessorTree<S, C, K> priorityMatchProcessorTree;

    private volatile boolean useTreePriority = false;

    /**
     * Processor使用记录，uniqueId为标识
     * 用于剪枝\ 配置加载统计
     */
    private final Map<String, Integer> useRecordMap;

    /**
     * Distinct non-empty processor shapes actually present in configuration.
     */
    private final Map<String, List<PriorityMatchFunction<S, C, K>>> activeFunctionLists;

    private final Map<C, List<K>> indexedConfigValues;

    private final Map<C, String> indexedDefaultNameAndValues;

    private final PriorityNameAndValueHandler<S, C, K> priorityNameAndValueHandler;

    private final boolean useDefaultNameAndValueHandler;

    private DuplicateKeyCheckReport<C, K> duplicateKeyCheckReport;

    private PriorityFetcher(List<PriorityMatchProcessor<S, C, K>> processorList,
                            List<PriorityMatchFunction<S, C, K>> prirotyList,
                            DuplicateKeyCheckLevel duplicateKeyCheckLevel,
                            PriorityNameAndValueHandler<S, C, K> priorityNameAndValueHandler) {
        validatePriorityLayout(prirotyList);
        validateProcessorLayout(processorList, prirotyList);
        this.tree = new PriorityMatchTree[prirotyList.size()];
        for (int i = 0; i < prirotyList.size(); i++) {
            tree[i] = new PriorityMatchTree<>(1);
        }
        this.processorList = new ArrayList<>(processorList);
        this.useRecordMap = new HashMap<>();
        this.activeFunctionLists = new LinkedHashMap<>();
        this.indexedConfigValues = new IdentityHashMap<>();
        this.indexedDefaultNameAndValues = new IdentityHashMap<>();
        this.priorityNameAndValueHandler = priorityNameAndValueHandler;
        this.useDefaultNameAndValueHandler =
                priorityNameAndValueHandler instanceof DefaultPriorityNameAndValueHandler;
        this.duplicateKeyCheckReport = DuplicateKeyCheckReport.empty(duplicateKeyCheckLevel);
    }

    public PriorityMatchTree<S, C, K>[] getTree() {
        return tree.clone();
    }

    public List<PriorityMatchProcessor<S, C, K>> getProcessorList() {
        return Collections.unmodifiableList(processorList);
    }

    /**
     * Returns the immutable duplicate-key report created with this fetcher.
     *
     * @return non-null report, including when checking is disabled or finds no duplicates
     */
    public DuplicateKeyCheckReport<C, K> getDuplicateKeyCheckReport() {
        return duplicateKeyCheckReport;
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
     * @param allPriority 是否获取全部优先级
     * @return 单个优先级配置
     */
    public List<PriorityMatchResult<List<C>>> match(S source, boolean allPriority) {
        SourceValueCache<S, C, K> sourceValues = new SourceValueCache<>(source);
        return useTreePriority && allPriority
                ? matchTree(source, true, sourceValues)
                : matchLevel(source, allPriority, sourceValues);
    }

    /**
     * 需求匹配配置集，返回单优先级最高的配置集，可能是多个
     * 使用时要注意配置多的可能性
     *
     * @param source      需求信息
     * @param allPriority 是否获取全部优先级
     * @return 单个优先级配置
     */
    private List<PriorityMatchResult<List<C>>> matchTree(
            S source,
            boolean allPriority,
            SourceValueCache<S, C, K> sourceValues) {
        List<PriorityMatchResult<List<C>>> matchResultList = new ArrayList<>();
        LinkedList<PriorityMatchFunction<S, C, K>> recordList = new LinkedList<>();
        for (PriorityMatchProcessorTree<S, C, K> value : priorityMatchProcessorTree.getPriorityMatchFunctionTree().values()) {
            PriorityMatchFunction<S, C, K> functionNode = value.getFunctionNode();
            recordList.add(functionNode);
            PriorityMatchTree<S, C, K> priorityMatchTree = tree[functionNode.getPriority()];
            List<Object> kList = functionNode.matchSourceIndexValue(sourceValues.get(functionNode),
                    priorityMatchTree::getKeyList);
            for (PriorityMatchProcessorTree<S, C, K> childPriorityMatchFunctionTree : value.getPriorityMatchFunctionTree().values()) {
                if (childPriorityMatchFunctionTree.isBottom()) {
                    recursion(null, source, childPriorityMatchFunctionTree, priorityMatchTree,
                            matchResultList, recordList, true, sourceValues);
                }else if (!kList.isEmpty()){
                    for (Object k : kList) {
                        recursion(k, source, childPriorityMatchFunctionTree, priorityMatchTree,
                                matchResultList, recordList, true, sourceValues);
                    }
                }
            }
            recordList.removeLast();
        }
        return orderAndAggregate(matchResultList, allPriority, source, sourceValues);
    }

    private boolean recursion(Object k,
                           S source,
                           PriorityMatchProcessorTree<S, C, K> priorityMatchFunctionTree,
                           PriorityMatchTree<S, C, K> parentPriorityMatchTree,
                           List<PriorityMatchResult<List<C>>> matchResultList,
                           LinkedList<PriorityMatchFunction<S, C, K>> recordList,
                           boolean allPriority,
                           SourceValueCache<S, C, K> sourceValues) {
        PriorityMatchFunction<S, C, K> functionNode = priorityMatchFunctionTree.getFunctionNode();

        if (priorityMatchFunctionTree.isBottom()) {
            List<Object> kList = functionNode.matchSourceIndexValue(sourceValues.get(functionNode),
                    parentPriorityMatchTree::getConfigKeyList);
            for (Object newK : kList) {
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
            List<Object> kList = functionNode.matchSourceIndexValue(
                    sourceValues.get(functionNode), childTree::getKeyList);
            for (PriorityMatchProcessorTree<S, C, K> childPriorityMatchFunctionTree : priorityMatchFunctionTree.getPriorityMatchFunctionTree().values()) {
                if (childPriorityMatchFunctionTree.isBottom()) {
                    if (recursion(null, source, childPriorityMatchFunctionTree, childTree, matchResultList,
                            recordList, allPriority, sourceValues)) {
                        return true;
                    }

                }else if (!kList.isEmpty()){
                    for (Object newK : kList) {
                        if (recursion(newK, source, childPriorityMatchFunctionTree, childTree, matchResultList,
                                recordList, allPriority, sourceValues)) {
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
    private List<PriorityMatchResult<List<C>>> matchLevel(
            S source,
            boolean allPriority,
            SourceValueCache<S, C, K> sourceValues) {
        List<PriorityMatchProcessor<S, C, K>> matchedProcessors = new ArrayList<>();
        List<List<C>> matchedConfigs = new ArrayList<>();
        for (PriorityMatchProcessor<S, C, K> processor : processorList) {
            List<C> matches = matchProcessor(processor, sourceValues);
            if (!matches.isEmpty()) {
                matchedProcessors.add(processor);
                matchedConfigs.add(matches);
                if (!allPriority) {
                    break;
                }
            }
        }
        List<PriorityMatchResult<List<C>>> results = new ArrayList<>(matchedProcessors.size());
        for (int index = 0; index < matchedProcessors.size(); index++) {
            results.add(toResult(matchedProcessors.get(index), matchedConfigs.get(index), source, sourceValues));
        }
        return results;
    }

    private List<C> matchProcessor(PriorityMatchProcessor<S, C, K> processor,
                                   SourceValueCache<S, C, K> sourceValues) {
        List<PriorityMatchFunction<S, C, K>> functions = processor.getPriorityMatchFunctionList();
        PriorityMatchFunction<S, C, K> head = functions.get(0);
        PriorityMatchTree<S, C, K> headTree = tree[head.getPriority()];
        List<C> matches = new ArrayList<>();
        if (functions.size() == 1) {
            for (Object key : head.matchSourceIndexValue(
                    sourceValues.get(head), headTree::getConfigKeyList)) {
                matches.addAll(headTree.getConfigList(key));
            }
            return matches;
        }
        for (Object key : head.matchSourceIndexValue(
                sourceValues.get(head), headTree::getKeyList)) {
            collectProcessorMatches(functions, 1, headTree, key, matches, sourceValues);
        }
        return matches;
    }

    private void collectProcessorMatches(List<PriorityMatchFunction<S, C, K>> functions,
                                         int index,
                                         PriorityMatchTree<S, C, K> parentTree,
                                         Object parentKey,
                                         List<C> matches,
                                         SourceValueCache<S, C, K> sourceValues) {
        PriorityMatchFunction<S, C, K> function = functions.get(index);
        PriorityMatchTree<S, C, K> childTree = parentTree.getChildTree(parentKey, function);
        if (childTree == null) {
            return;
        }
        if (functions.size() == index + 1) {
            for (Object key : function.matchSourceIndexValue(
                    sourceValues.get(function), childTree::getConfigKeyList)) {
                matches.addAll(childTree.getConfigList(key));
            }
            return;
        }
        for (Object key : function.matchSourceIndexValue(
                sourceValues.get(function), childTree::getKeyList)) {
            collectProcessorMatches(functions, index + 1, childTree, key, matches, sourceValues);
        }
    }

    private PriorityMatchResult<List<C>> toResult(PriorityMatchProcessor<S, C, K> processor,
                                                   List<C> matches,
                                                   S source,
                                                   SourceValueCache<S, C, K> sourceValues) {
        List<K> processorSourceValues = null;
        if (!useDefaultNameAndValueHandler) {
            processorSourceValues = new ArrayList<>(processor.getFunctionSize());
            for (PriorityMatchFunction<S, C, K> function : processor.getPriorityMatchFunctionList()) {
                processorSourceValues.add(sourceValues.get(function));
            }
        }
        LinkedHashSet<String> completePaths = new LinkedHashSet<>();
        for (C config : matches) {
            if (useDefaultNameAndValueHandler) {
                String completePath = indexedDefaultNameAndValues.get(config);
                if (completePath == null) {
                    throw missingNameAndValueMetadata(processor);
                }
                completePaths.add(completePath);
            } else {
                List<K> matchedConfigValues = indexedConfigValues.get(config);
                if (matchedConfigValues == null) {
                    throw missingNameAndValueMetadata(processor);
                }
                completePaths.add(PriorityMatchProcessor.initNameAndValue(
                        processor.getPriorityMatchFunctionList(), source, config, processorSourceValues,
                        matchedConfigValues, priorityNameAndValueHandler));
            }
        }
        String nameAndValue = String.join(";", completePaths);
        return new PriorityMatchResult<>(processor.getUniqueId(), processor.getName(), nameAndValue,
                processor.getFunctionSize(), new ArrayList<>(matches));
    }

    private IllegalStateException missingNameAndValueMetadata(
            PriorityMatchProcessor<S, C, K> processor) {
        return new IllegalStateException("Matched configuration key metadata is missing for processor "
                + processor.getUniqueId());
    }

    private List<PriorityMatchResult<List<C>>> orderAndAggregate(
            List<PriorityMatchResult<List<C>>> rawResults,
            boolean allPriority,
            S source,
            SourceValueCache<S, C, K> sourceValues) {
        Map<String, List<C>> matchesByProcessor = new LinkedHashMap<>();
        for (PriorityMatchResult<List<C>> rawResult : rawResults) {
            matchesByProcessor.computeIfAbsent(rawResult.getUniqueId(), ignored -> new ArrayList<>())
                    .addAll(rawResult.getResult());
        }
        List<PriorityMatchResult<List<C>>> orderedResults = new ArrayList<>();
        for (PriorityMatchProcessor<S, C, K> processor : processorList) {
            List<C> matches = matchesByProcessor.get(processor.getUniqueId());
            if (matches != null && !matches.isEmpty()) {
                orderedResults.add(toResult(processor, matches, source, sourceValues));
                if (!allPriority) {
                    break;
                }
            }
        }
        return orderedResults;
    }

    public void useRecordCount(String id) {
        useRecordMap.put(id, useRecordMap.getOrDefault(id, 0) + 1);
    }

    /**
     * 基础的初始化逻辑
     *
     * @param processorList 已按匹配优先级排序的处理器
     * @param configList 配置集合
     * @param prirotyList 从零开始连续注册的匹配函数
     * @param <S> 需求类型
     * @param <C> 配置类型
     * @param <K> 匹配键类型
     * @return 初始化后的获取器
     */
    public static <S, C, K> PriorityFetcher<S, C, K> from(List<PriorityMatchProcessor<S, C, K>> processorList,
                                                          List<C> configList,
                                                          List<PriorityMatchFunction<S, C, K>> prirotyList) {
        return from(processorList, configList, prirotyList, DuplicateKeyCheckLevel.OFF,
                new DefaultPriorityNameAndValueHandler<>());
    }

    private static <S, C, K> PriorityFetcher<S, C, K> from(
            List<PriorityMatchProcessor<S, C, K>> processorList,
            List<C> configList,
            List<PriorityMatchFunction<S, C, K>> prirotyList,
            DuplicateKeyCheckLevel duplicateKeyCheckLevel,
            PriorityNameAndValueHandler<S, C, K> priorityNameAndValueHandler) {
        if (processorList == null || configList == null) {
            throw new IllegalArgumentException("Processor and config lists cannot be null");
        }
        if (duplicateKeyCheckLevel == null) {
            throw new IllegalArgumentException("Duplicate key check level cannot be null");
        }
        if (priorityNameAndValueHandler == null) {
            throw new IllegalArgumentException("Priority name and value handler cannot be null");
        }
        // 初始化最终对象
        PriorityFetcher<S, C, K> priorityFetcher = new PriorityFetcher<>(processorList, prirotyList,
                duplicateKeyCheckLevel, priorityNameAndValueHandler);
        Map<List<DuplicateKeyPart<Object>>, DuplicateGroup<C, K>> duplicateGroups =
                duplicateKeyCheckLevel == DuplicateKeyCheckLevel.OFF ? null : new LinkedHashMap<>();
        // 循环配置，设置key匹配情况
        for (C config : configList) {
            Object indexKey = null;
            PriorityMatchFunction<S, C, K> functionHead = null;
            PriorityMatchTree<S, C, K> priorityMatchTree = null;
            List<PriorityMatchFunction<S, C, K>> usePriorityMatchFunctionList = new ArrayList<>(prirotyList.size());
            List<K> effectiveConfigValues = new ArrayList<>(prirotyList.size());
            List<Object> effectiveIndexValues = new ArrayList<>(prirotyList.size());
            List<DuplicateKeyPart<Object>> effectiveIndexKeyParts = duplicateGroups == null
                    ? null : new ArrayList<>(prirotyList.size());
            List<DuplicateKeyPart<K>> effectiveDisplayKeyParts = duplicateGroups == null
                    ? null : new ArrayList<>(prirotyList.size());
            for (PriorityMatchFunction<S, C, K> priorityMatchFunction : prirotyList) {
                K configuredValue = priorityMatchFunction.matchConfig(config);
                Object newIndexKey = priorityMatchFunction
                        .prepareIndexConfigValue(configuredValue);
                // 如果为空说明该路由不匹配, 或者是空字符, 应该匹配其他场景的优先级
                if (newIndexKey == null || Objects.equals("", newIndexKey)) {
                    continue;
                }
                usePriorityMatchFunctionList.add(priorityMatchFunction);
                effectiveConfigValues.add(configuredValue);
                effectiveIndexValues.add(newIndexKey);
                if (effectiveIndexKeyParts != null) {
                    effectiveIndexKeyParts.add(new DuplicateKeyPart<>(
                            priorityMatchFunction.getPriority(),
                            priorityMatchFunction.getName(), newIndexKey));
                    effectiveDisplayKeyParts.add(new DuplicateKeyPart<>(
                            priorityMatchFunction.getPriority(),
                            priorityMatchFunction.getName(), configuredValue, newIndexKey));
                }
                // 初始化头
                if (functionHead == null) {
                    functionHead = priorityMatchFunction;
                    priorityMatchTree = priorityFetcher.getTree()[functionHead.getPriority()];
                    indexKey = newIndexKey;
                    continue;
                }
                // 基于顶层k生成子树节点数据
                priorityMatchTree = priorityMatchTree.initChildTree(
                        indexKey, priorityMatchFunction, prirotyList.size());
                // 将K替换为子节点的K
                indexKey = newIndexKey;
            }
            // 3.当前 priorityMatchTree 已经是叶子节点数据, 增加数据
            if (indexKey != null) {
                priorityMatchTree.addConfig(indexKey, config);
                // 记录使用
                String processorId = PriorityMatchProcessor.initUniqueId(usePriorityMatchFunctionList);
                priorityFetcher.useRecordCount(processorId);
                priorityFetcher.activeFunctionLists.putIfAbsent(processorId,
                        Collections.unmodifiableList(new ArrayList<>(usePriorityMatchFunctionList)));
                List<K> indexedValues = Collections.unmodifiableList(new ArrayList<>(effectiveConfigValues));
                if (priorityFetcher.useDefaultNameAndValueHandler) {
                    String defaultNameAndValue = initDefaultNameAndValue(
                            usePriorityMatchFunctionList, effectiveIndexValues);
                    priorityFetcher.indexedDefaultNameAndValues
                            .putIfAbsent(config, defaultNameAndValue);
                } else {
                    priorityFetcher.indexedConfigValues
                            .putIfAbsent(config, indexedValues);
                }
                if (duplicateGroups != null) {
                    recordDuplicateCandidate(duplicateGroups, effectiveIndexKeyParts,
                            effectiveDisplayKeyParts, config);
                }
            }
        }

        if (duplicateGroups != null) {
            priorityFetcher.duplicateKeyCheckReport = buildDuplicateKeyCheckReport(
                    duplicateKeyCheckLevel, duplicateGroups);
            if (priorityFetcher.duplicateKeyCheckReport.hasDuplicates()) {
                if (duplicateKeyCheckLevel == DuplicateKeyCheckLevel.EXCEPTION) {
                    throw new DuplicateMatchKeyException(priorityFetcher.duplicateKeyCheckReport);
                }
                logDuplicateWarning(priorityFetcher.duplicateKeyCheckReport);
            }
        }

        return priorityFetcher;
    }

    static <S, C, K> PriorityFetcher<S, C, K> from(PriorityHandler priorityHandler,
                                                    List<C> configList,
                                                    List<PriorityMatchFunction<S, C, K>> priorityList) {
        return from(priorityHandler, configList, priorityList, DuplicateKeyCheckLevel.OFF,
                new DefaultPriorityNameAndValueHandler<>());
    }

    static <S, C, K> PriorityFetcher<S, C, K> from(PriorityHandler priorityHandler,
                                                    List<C> configList,
                                                    List<PriorityMatchFunction<S, C, K>> priorityList,
                                                    DuplicateKeyCheckLevel duplicateKeyCheckLevel,
                                                    PriorityNameAndValueHandler<S, C, K> priorityNameAndValueHandler) {
        if (priorityHandler == null) {
            throw new IllegalArgumentException("Priority handler cannot be null");
        }
        PriorityFetcher<S, C, K> priorityFetcher = from(Collections.emptyList(), configList, priorityList,
                duplicateKeyCheckLevel, priorityNameAndValueHandler);
        List<PriorityMatchProcessor<S, C, K>> processors = priorityHandler.initPriorityHandlerList(
                priorityList, priorityFetcher.activeFunctionLists.values());
        validateProcessorLayout(processors, priorityList);
        priorityFetcher.processorList = new ArrayList<>(processors);
        return priorityFetcher;
    }

    private static <S, C, K> String initDefaultNameAndValue(
            List<PriorityMatchFunction<S, C, K>> functions,
            List<Object> indexedValues) {
        List<String> pairs = new ArrayList<>(functions.size());
        for (int index = 0; index < functions.size(); index++) {
            PriorityMatchFunction<S, C, K> function = functions.get(index);
            String name = function.getName() == null
                    ? "priority[" + function.getPriority() + "]" : function.getName();
            pairs.add(name + ":" + DuplicateKeyDiagnostics.safeValue(indexedValues.get(index)));
        }
        return String.join("_", pairs);
    }

    private static <C, K> void recordDuplicateCandidate(
            Map<List<DuplicateKeyPart<Object>>, DuplicateGroup<C, K>> duplicateGroups,
            List<DuplicateKeyPart<Object>> effectiveIndexKeyParts,
            List<DuplicateKeyPart<K>> effectiveDisplayKeyParts,
            C config) {
        List<DuplicateKeyPart<Object>> indexKey = Collections.unmodifiableList(
                new ArrayList<>(effectiveIndexKeyParts));
        DuplicateGroup<C, K> group = duplicateGroups.get(indexKey);
        if (group == null) {
            group = new DuplicateGroup<>(Collections.unmodifiableList(
                    new ArrayList<>(effectiveDisplayKeyParts)));
            duplicateGroups.put(indexKey, group);
        }
        group.add(config);
    }

    private static <C, K> DuplicateKeyCheckReport<C, K> buildDuplicateKeyCheckReport(
            DuplicateKeyCheckLevel level,
            Map<List<DuplicateKeyPart<Object>>, DuplicateGroup<C, K>> duplicateGroups) {
        int duplicateGroupCount = 0;
        int duplicateRecordCount = 0;
        List<DuplicateKeySample<C, K>> samples = new ArrayList<>();
        for (DuplicateGroup<C, K> group : duplicateGroups.values()) {
            if (group.count < 2) {
                continue;
            }
            duplicateGroupCount++;
            duplicateRecordCount += group.count;
            for (C config : group.sampleConfigs) {
                if (samples.size() == DuplicateKeyCheckReport.MAX_SAMPLE_COUNT) {
                    break;
                }
                samples.add(new DuplicateKeySample<>(group.completeKey, config));
            }
        }
        return new DuplicateKeyCheckReport<>(level, duplicateGroupCount, duplicateRecordCount, samples);
    }

    private static void logDuplicateWarning(DuplicateKeyCheckReport<?, ?> report) {
        try {
            LOGGER.warning("Duplicate complete configuration keys detected: " + report);
        } catch (RuntimeException ignored) {
            // A broken application logging handler must not turn WARNING into EXCEPTION behavior.
        }
    }

    private static final class DuplicateGroup<C, K> {
        private final List<DuplicateKeyPart<K>> completeKey;
        private final List<C> sampleConfigs = new ArrayList<>();
        private int count;

        private DuplicateGroup(List<DuplicateKeyPart<K>> completeKey) {
            this.completeKey = completeKey;
        }

        private void add(C config) {
            count++;
            if (sampleConfigs.size() < DuplicateKeyCheckReport.MAX_SAMPLE_COUNT) {
                sampleConfigs.add(config);
            }
        }
    }

    private static final class SourceValueCache<S, C, K> {
        private final S source;
        private final Map<PriorityMatchFunction<S, C, K>, K> values = new IdentityHashMap<>();

        private SourceValueCache(S source) {
            this.source = source;
        }

        private K get(PriorityMatchFunction<S, C, K> function) {
            if (!values.containsKey(function)) {
                values.put(function, source == null ? null : function.getSourceGetter().apply(source));
            }
            return values.get(function);
        }
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
        PriorityMatchProcessorTree<S, C, K> builtTree = PriorityMatchProcessorTree.build(this.processorList);
        this.priorityMatchProcessorTree = builtTree;
        this.useTreePriority = true;
        return this;
    }

    private static <S, C, K> void validatePriorityLayout(
            List<PriorityMatchFunction<S, C, K>> priorityList) {
        if (priorityList == null) {
            throw new IllegalArgumentException("Priority function list cannot be null");
        }
        for (int index = 0; index < priorityList.size(); index++) {
            PriorityMatchFunction<S, C, K> function = priorityList.get(index);
            if (function == null) {
                throw new IllegalArgumentException("Priority function at index " + index + " cannot be null");
            }
            if (!Objects.equals(index, function.getPriority())) {
                throw new IllegalArgumentException("Priority must equal its zero-based registration index: expected "
                        + index + " but was " + function.getPriority());
            }
        }
    }

    private static <S, C, K> void validateProcessorLayout(
            List<PriorityMatchProcessor<S, C, K>> processors,
            List<PriorityMatchFunction<S, C, K>> priorityList) {
        if (processors == null) {
            throw new IllegalArgumentException("Processor list cannot be null");
        }
        Set<String> processorIds = new HashSet<>();
        for (int processorIndex = 0; processorIndex < processors.size(); processorIndex++) {
            PriorityMatchProcessor<S, C, K> processor = processors.get(processorIndex);
            if (processor == null) {
                throw new IllegalArgumentException("Processor at index " + processorIndex + " cannot be null");
            }
            if (!processorIds.add(processor.getUniqueId())) {
                throw new IllegalArgumentException("Duplicate processor at index " + processorIndex);
            }
            int previousPriority = -1;
            for (PriorityMatchFunction<S, C, K> function : processor.getPriorityMatchFunctionList()) {
                int priority = function.getPriority();
                if (priority <= previousPriority) {
                    throw new IllegalArgumentException("Processor functions must follow ascending priority order");
                }
                if (priority >= priorityList.size() || !function.equals(priorityList.get(priority))) {
                    throw new IllegalArgumentException("Processor contains a function outside the registered priority list");
                }
                previousPriority = priority;
            }
        }
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
        private final Map<Object, PriorityMatchTree<S, C, K>[]> currentTree;

        /**
         * 叶子节点才存在配置
         */
        private final Map<Object, List<C>> configMap;

        PriorityMatchTree(Integer index) {
            this.index = index;
            this.currentTree = new LinkedHashMap<>();
            this.configMap = new LinkedHashMap<>();
        }

        public Integer getIndex() {
            return index;
        }

        public void addConfig(Object k, C config) {
            this.configMap.computeIfAbsent(k, k1 -> new ArrayList<>()).add(config);
        }

        public Collection<Object> getKeyList() {
            return currentTree.keySet();
        }

        public Collection<Object> getConfigKeyList() {
            return configMap.keySet();
        }

        public List<C> getConfigList(Object k) {
            return configMap.getOrDefault(k, Collections.emptyList());
        }


        public PriorityMatchTree<S, C, K> getChildTree(
                Object k, PriorityMatchFunction<S, C, K> childFunction) {
            PriorityMatchTree<S, C, K>[] priorityMatchTrees = currentTree.get(k);
            if (priorityMatchTrees == null) {
                return null;
            }
            return priorityMatchTrees[childFunction.getPriority()];
        }

        public PriorityMatchTree<S, C, K> initChildTree(
                Object k, PriorityMatchFunction<S, C, K> childFunction, int prioritySize) {
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
