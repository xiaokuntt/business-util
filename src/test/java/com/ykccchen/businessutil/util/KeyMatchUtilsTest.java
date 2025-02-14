package com.ykccchen.businessutil.util;

import com.ykccchen.businessutil.dto.KeyMatchResult;
import com.ykccchen.businessutil.util.KeyMatchUtils;
import org.junit.Test;

import java.util.*;
import java.util.function.Function;

/**
 * @author ykccchen
 * @version 1.0
 * @description KeyMatchUtils 使用方法测试类 {@link  KeyMatchUtils}
 * @date 2024/12/16 13:21
 */
public class KeyMatchUtilsTest {

    /**
     * 通用配置
     */
    private static List<Map<String, String>> configList;
    static {
        // 配置
        configList = new ArrayList<>();
        Map<String, String> config1 = new HashMap<>();
        config1.put("p1", "p1-key");
        config1.put("value", "p1-value");
        configList.add(config1);

        Map<String, String> config2 = new HashMap<>();
        config2.put("p2", "p2-key");
        config2.put("value", "p2-value");
        configList.add(config2);

        Map<String, String> config3 = new HashMap<>();
        config3.put("p3", "p3-key");
        config3.put("value", "p3-value");
        configList.add(config3);

        Map<String, String> config4 = new HashMap<>();
        config4.put("p3", "p3-key");
        config4.put("p1", "p1-key");
        config4.put("value", "p3|p2-value");
        configList.add(config4);

        Map<String, String> config5 = new HashMap<>();
        config5.put("p3", "p3-key");
        config5.put("p2", "p2-key");
        config5.put("p1", "p1-key");
        config5.put("value", "p3|p2|p1-value");
        configList.add(config5);


    }
    /**
     * 需求集合
     */
    private static List<Map<String, String>> reqList;
    static {
        //需求
        reqList = new ArrayList<>();
        Map<String, String> req1 = new HashMap<>();
        req1.put("p1", "p1-key");
        reqList.add(req1);

        Map<String, String> req2 = new HashMap<>();
        req2.put("p2", "p2-key");
        reqList.add(req2);

        Map<String, String> req3 = new HashMap<>();
        req3.put("p3", "p3-key");
        reqList.add(req3);

        Map<String, String> req4 = new HashMap<>();
        req4.put("p3", "p3-key");
        req4.put("p1", "p1-key");
        reqList.add(req4);

        Map<String, String> req5 = new HashMap<>();
        req5.put("p3", "p3-key");
        req5.put("p2", "p2-key");
        req5.put("p1", "p1-key");
        reqList.add(req5);

        Map<String, String> req6 = new HashMap<>();
        req6.put("p3", "p3-key");
        req6.put("p2", "p2-key");
        reqList.add(req6);

        Map<String, String> req7 = new HashMap<>();
        req7.put("p3", "p3-key");
        req7.put("p2", "XXX");
        req7.put("p1", "p1-key");
        reqList.add(req7);

        Map<String, String> req8 = new HashMap<>();
        req8.put("p3", "XXX");
        reqList.add(req8);
    }


    @Test
    public void testMatch(){
        // 这里使用有序的哈希MAP， 保证优先级的顺序的对的，这个逻辑必须
        Map<Function<Map<String, String>, String>, Integer> mapByValue = new LinkedHashMap<>();
        // key值为维度的获取方式， value值为价值，值越高价值越大
        mapByValue.put(map->map.get("p3"), 2);
        mapByValue.put(map->map.get("p2"), 1);
        mapByValue.put(map->map.get("p1"), 0);
        //初始化配置优先级组合集
        List<List<Function<Map<String, String>, String>>> priorityList = KeyMatchUtils.initMatchFunctionPriority(mapByValue);
        // 配置初始化    需要按顺序
        Map<String, Map<String, String>> configMap = KeyMatchUtils.initMatchFunctionConfigMap(new ArrayList<>(mapByValue.keySet()), configList);
        for (Map.Entry<String, Map<String, String>> stringMapEntry : configMap.entrySet()) {
            System.out.println(stringMapEntry);
        }
        System.out.println();
        // 匹配
        for (Map<String, String> req : reqList) {
            KeyMatchResult<Map<String, String>> config = KeyMatchUtils.matchKey(req, priorityList, configMap);
           if (config != null){
               System.out.println("需求："+ req.toString() + "， 配置："+config.toString());
           }else {
               System.out.println("需求："+ req.toString() + "未命中配置");
           }
        }
    }

    /**
     * 全匹配测试
     */
    @Test
    public void testMatch2(){
        // 这里使用有序的哈希MAP， 保证优先级的顺序的对的，这个逻辑必须
        Map<Function<Map<String, String>, String>, Integer> mapByValue = new LinkedHashMap<>();
        // key值为维度的获取方式， value值为价值，值越高价值越大
        mapByValue.put(map->map.get("p3"), 3);
        mapByValue.put(map->map.get("p2"), 2);
        mapByValue.put(map->map.get("p1"), 1);
        //初始化配置优先级组合集
        List<List<Function<Map<String, String>, String>>> priorityList = KeyMatchUtils.initMatchFunctionPriority(mapByValue);
        // 配置初始化    需要按顺序
        Map<String, Map<String, String>> configMap = KeyMatchUtils.initMatchFunctionConfigMap(new ArrayList<>(mapByValue.keySet()), configList);
        for (Map.Entry<String, Map<String, String>> stringMapEntry : configMap.entrySet()) {
            System.out.println(stringMapEntry);
        }
        System.out.println();
        // 匹配
        for (Map<String, String> req : reqList) {
            KeyMatchResult<Map<String, String>> config = KeyMatchUtils.matchKey(req, priorityList, configMap, true);
            if (config != null){
                System.out.println("需求："+ req.toString() + "， 配置："+ config.toString());
            }else {
                System.out.println("需求："+ req.toString() + "未命中配置");
            }
        }
    }
}
