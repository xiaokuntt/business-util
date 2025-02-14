package com.ykccchen.businessutil.util;



import com.ykccchen.businessutil.dto.KeyMatchResult;

import java.util.*;
import java.util.function.Function;

/**
 * @author ykccchen
 * @version 1.0
 * @description key多维度匹配工具
 * @date 2024/9/18 17:23
 */
public class KeyMatchUtils {

    public static final String DEFAULT_SPLIT_STRING = "-";

    /**
     * @param obj               对象
     * @param functionMatchList 匹配函数
     * @param configMap         配置map
     * @return config配置，可能是空的，等于没匹配到
     */
    public static <T, V> KeyMatchResult<T> matchKey(V obj,
                                    List<List<Function<V, String>>> functionMatchList,
                                    Map<String, T> configMap) {
        return matchKey(obj, functionMatchList, configMap, DEFAULT_SPLIT_STRING, false);
    }

    /**
     * @param obj               对象
     * @param functionMatchList 匹配函数
     * @param configMap         配置map
     * @param fullMatchFlag     完全key匹配，当对象参与匹配的字段不为空时会加入key的生成规则，获取配置时必须完全匹配，不向下低优先级兼容
     * @return config配置，可能是空的，等于没匹配到
     */
    public static <T, V> KeyMatchResult<T> matchKey(V obj,
                                    List<List<Function<V, String>>> functionMatchList,
                                    Map<String, T> configMap,
                                    boolean fullMatchFlag) {
        return matchKey(obj, functionMatchList, configMap, DEFAULT_SPLIT_STRING, fullMatchFlag);
    }

    /**
     * @param obj               对象
     * @param functionMatchList 匹配函数
     * @param configMap         配置map
     * @param split             配置分隔符
     * @return config配置，可能是空的，等于没匹配到
     */
    public static <T, V> KeyMatchResult<T> matchKey(V obj,
                                                    List<List<Function<V, String>>> functionMatchList,
                                                    Map<String, T> configMap,
                                                    String split) {
        return matchKey(obj, functionMatchList, configMap, split, false);
    }

    /**
     * @param obj               对象
     * @param functionMatchList 匹配函数
     * @param configMap         配置map
     * @param split             字段分隔符，最后一个值的分隔符不需要去掉
     * @param fullMatchFlag     完全key匹配，当对象参与匹配的字段不为空时会加入key的生成规则，获取配置时必须完全匹配，不向下低优先级兼容
     * @return config配置，可能是空的，等于没匹配到
     */
    public static <T, V>  KeyMatchResult<T> matchKey(V obj,
                                    List<List<Function<V, String>>> functionMatchList,
                                    Map<String, T> configMap,
                                    String split,
                                    boolean fullMatchFlag) {
        // 跳过当前优先级
        boolean skipCurPriority = false;
        int level = 0;
        for (List<Function<V, String>> functions : functionMatchList) {
            level++;
            StringBuilder key = new StringBuilder();
            for (Function<V, String> function : functions) {
                String apply = function.apply(obj);
                // 为空当前优先级可以跳过
                if (apply == null || "".equals(apply)){
                    skipCurPriority = true;
                    break;
                }
                key.append(apply).append(split);
            }
            // 是否满足跳过条件：优先级获取key是空的
            if (skipCurPriority){
                skipCurPriority = false;
                continue;
            }
            T config = configMap.get(key.toString());
            if (config != null || fullMatchFlag) {
                return new KeyMatchResult<>(key.toString(), level, config);
            }
        }
        return null;
    }

    /**
     * Map<Function<PlanOrderDTO, String>, Integer> mapByValue = new HashMap<>();
     * mapByValue.put(PlanOrderDTO::getClientId, 4);
     * mapByValue.put(PlanOrderDTO::getScreenSize, 3);
     * mapByValue.put(PlanOrderDTO::getProductModel, 2);
     * mapByValue.put(PlanOrderDTO::getFactoryId, 1);
     * <p>
     * 多组合情况
     *
     * @param mapByValue 匹配集合优先级
     * @return 优先级集合
     */
    public static <T> List<List<Function<T, String>>> initMatchFunctionPriority(Map<Function<T, String>, Integer> mapByValue) {
        List<List<Function<T, String>>> functionList = new ArrayList<>();


        List<Function<T, String>> a = new ArrayList<>(mapByValue.keySet());


        for (int size = a.size(); size > 0; size--) {
            List<List<Function<T, String>>> combine = combine(a.size(), size, a);
            combine.sort((v1, v2) -> {
                int i1 = 0;
                for (Function<T, String> function : v1) {
                    i1 += 1 << mapByValue.get(function);
                }
                Integer i2 = 0;
                for (Function<T, String> function : v2) {
                    i2 += 1 << mapByValue.get(function);
                }
                return i2.compareTo(i1);
            });

            functionList.addAll(combine);
        }
        return functionList;


    }

    /**
     * generate config-map，if key is blank, skip this
     * @param keyFunctionList  key get function, need match priority consistency
     * @param configList config-list
     * @return config-map
     * @param <T>
     */
    public static <T> Map<String, T> initMatchFunctionConfigMap(List<Function<T, String>> keyFunctionList, List<T> configList) {
        return initMatchFunctionConfigMap(keyFunctionList, configList, null);
    }
    public static <T> Map<String, T> initMatchFunctionConfigMap(List<Function<T, String>> keyFunctionList, List<T> configList, String defaultKey) {
        Map<String, T> configMap = new HashMap<>();
        for (T t : configList) {
            StringBuilder keyMatch = new StringBuilder();
            for (Function<T, String> function : keyFunctionList) {
                String key = function.apply(t);
                if (key != null && !key.equals("")){
                    keyMatch.append(key).append(DEFAULT_SPLIT_STRING);
                }
            }
            if ( (keyMatch.length() == 0) && defaultKey != null){
                keyMatch.append(defaultKey);
            }
            configMap.put(keyMatch.toString(), t);
        }
        return configMap;
    }
    private static <T> List<List<Function<T, String>>> combine(int arrSize,
                                                               int combineSize,
                                                               List<Function<T, String>> list) {
        List<List<Function<T, String>>> res = new ArrayList<>();
        if (combineSize <= 0 || arrSize < combineSize) {
            res.add(list);
            return res;
        }
        // 从 1 开始是题目的设定
        Deque<Function<T, String>> path = new ArrayDeque<>();
        dfs(arrSize, combineSize, 1, path, res, list);
        return res;
    }

    private static <T> void dfs(int arrSize,
                                int combineSize,
                                int begin,
                                Deque<Function<T, String>> path,
                                List<List<Function<T, String>>> res,
                                List<Function<T, String>> list) {
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
