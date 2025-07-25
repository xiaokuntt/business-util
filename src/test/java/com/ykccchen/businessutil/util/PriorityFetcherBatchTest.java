package com.ykccchen.businessutil.util;

import com.ykccchen.businessutil.match.PriorityAssembler;
import com.ykccchen.businessutil.match.PriorityFetcher;
import com.ykccchen.businessutil.match.PriorityMatchProcessor;
import com.ykccchen.businessutil.match.PriorityMatchResult;
import com.ykccchen.businessutil.match.handler.PriorityMode;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

/**
 * @author ykccchen
 * @version 1.0
 * @description PriorityFetcher 使用方法测试类 {@link  PriorityFetcher}
 * @date 2024/12/16 13:21
 */
public class PriorityFetcherBatchTest {



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
    final int TOTAL_RECORDS = 100_000;
    final int PROPERTIES_COUNT = 12;
    final double NULL_PROBABILITY = 0.3; // 30%的概率为null
    @Test
    public void testMatch() {
        // 1. 预分配大小以提高性能
        List<Map<String, String>> configList = new ArrayList<>(TOTAL_RECORDS);
        // 2. 预生成属性键集合（避免循环中创建）
        List<String> propertyKeys = new ArrayList<>(PROPERTIES_COUNT);
        for (int i = 1; i <= PROPERTIES_COUNT; i++) {
            propertyKeys.add("p" + i);
        }
        // 3. 使用线程安全且高效的随机数生成器
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        // 4. 主生成逻辑
        for (int i = 0; i < TOTAL_RECORDS; i++) {
            Map<String, String> record = new HashMap<>(PROPERTIES_COUNT);

            for (String key : propertyKeys) {
                if (random.nextDouble() >= NULL_PROBABILITY) {
                    // 70%的概率生成值
                    record.put(key, "配置" + (i + 1) + "的" + key);
                }
                // else 30%的概率不添加（自然为null）
            }
            configList.add(record);
        }
        // 这里使用有序的哈希MAP， 保证优先级的顺序的对的，这个逻辑必须
        List<Function<Map<String, String>, String>> mapByValue = new ArrayList<>();
        // key值为维度的获取方式， value值为价值，值越高价值越大
        mapByValue.add(map -> map.get("p1"));
        mapByValue.add(map -> map.get("p2"));
        mapByValue.add(map -> map.get("p3"));
        mapByValue.add(map -> map.get("p4"));
        mapByValue.add(map -> map.get("p5"));
        mapByValue.add(map -> map.get("p6"));
        mapByValue.add(map -> map.get("p7"));
        mapByValue.add(map -> map.get("p8"));
        mapByValue.add(map -> map.get("p9"));
        mapByValue.add(map -> map.get("p10"));
        mapByValue.add(map -> map.get("p11"));
        mapByValue.add(map -> map.get("p12"));
        //初始化配置优先级组合集
        PriorityAssembler<Map<String, String>, Map<String, String>, String> priorityAssembler = PriorityAssembler.from(new PriorityAssembler.TypeReference<Map<String, String>>() {
                }, new PriorityAssembler.TypeReference<Map<String, String>>() {
                }, new PriorityAssembler.TypeReference<String>() {
                })
                .initConfig(configList)
                .initPriorityHandler(PriorityMode.NUMBER_OF_MATCHES);
        //初始化数据获取逻辑
        for (int i = 0; i < mapByValue.size(); i++) {
            priorityAssembler.addPriorityMatchFunction("配置"+ (i+1), mapByValue.get(i), mapByValue.get(i));
        }

        // 创建对象
        PriorityFetcher<Map<String, String>, Map<String, String>, String> priorityFetcher = priorityAssembler.create();

         //  配置初始化    需要按顺序
        for (PriorityMatchProcessor<Map<String, String>, Map<String, String>, String> priorityMatchProcessor : priorityFetcher.getProcessorList()) {
            System.out.println(priorityMatchProcessor);
        }
//        System.out.println(priorityFetcher.getProcessorList());
        System.out.println();
        // 匹配
        for (Map<String, String> req : reqList) {
            PriorityMatchResult<List<Map<String, String>>> match = priorityFetcher.match(req);
            if (match != null) {
                System.out.println("需求：" + req.toString() + "， 配置：" + match.toString());
            } else {
                System.out.println("需求：" + req.toString() + "未命中配置");
            }
        }
    }


}
