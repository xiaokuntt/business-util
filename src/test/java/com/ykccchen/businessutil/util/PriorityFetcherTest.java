package com.ykccchen.businessutil.util;

import com.ykccchen.businessutil.match.*;
import com.ykccchen.businessutil.match.handler.PriorityMode;
import org.junit.Test;
import org.apache.commons.lang3.Range;


import java.io.Serializable;
import java.util.*;
import java.util.function.Function;

/**
 * @author ykccchen
 * @version 1.0
 * @description PriorityFetcher 使用方法测试类 {@link  PriorityFetcher}
 * @date 2024/12/16 13:21
 */
public class PriorityFetcherTest {

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
    public void testMatch() {
        // 这里使用有序的哈希MAP， 保证优先级的顺序的对的，这个逻辑必须
        List<Function<Map<String, String>, String>> mapByValue = new ArrayList<>();
        // key值为维度的获取方式， value值为价值，值越高价值越大
        mapByValue.add(map -> map.get("p1"));
        mapByValue.add(map -> map.get("p2"));
        mapByValue.add(map -> map.get("p3"));
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

        // 配置初始化    需要按顺序
        for (PriorityMatchProcessor<Map<String, String>, Map<String, String>, String> priorityMatchProcessor : priorityFetcher.getProcessorList()) {
            System.out.println(priorityMatchProcessor);
        }
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

    /**
     * 绝对价值
     */
    @Test
    public void testMatch2() {
        // 这里使用有序的哈希MAP， 保证优先级的顺序的对的，这个逻辑必须
        List<Function<Map<String, String>, String>> mapByValue = new ArrayList<>();
        // key值为维度的获取方式， value值为价值，值越高价值越大
        mapByValue.add(map -> map.get("p1"));
        mapByValue.add(map -> map.get("p2"));
        mapByValue.add(map -> map.get("p3"));
        //初始化配置优先级组合集
        PriorityAssembler<Map<String, String>, Map<String, String>, String> priorityAssembler = PriorityAssembler.from(new PriorityAssembler.TypeReference<Map<String, String>>() {
                }, new PriorityAssembler.TypeReference<Map<String, String>>() {
                }, new PriorityAssembler.TypeReference<String>() {
                })
                .initConfig(configList)
                .initPriorityHandler(PriorityMode.ABSOLUTE_VALUE);
        //初始化数据获取逻辑
        for (int i = 0; i < mapByValue.size(); i++) {
            priorityAssembler.addPriorityMatchFunction("配置"+ (i+1), mapByValue.get(i), mapByValue.get(i));
        }

        // 创建对象
        PriorityFetcher<Map<String, String>, Map<String, String>, String> priorityFetcher = priorityAssembler.create();

        // 配置初始化    需要按顺序
        for (PriorityMatchProcessor<Map<String, String>, Map<String, String>, String> priorityMatchProcessor : priorityFetcher.getProcessorList()) {
            System.out.println(priorityMatchProcessor);
        }
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

    /**
     * 全匹配
     */
    @Test
    public void testMatch3() {
        // 这里使用有序的哈希MAP， 保证优先级的顺序的对的，这个逻辑必须
        List<Function<Map<String, String>, String>> mapByValue = new ArrayList<>();
        // key值为维度的获取方式， value值为价值，值越高价值越大
        mapByValue.add(map -> map.get("p1"));
        mapByValue.add(map -> map.get("p2"));
        mapByValue.add(map -> map.get("p3"));
        //初始化配置优先级组合集
        PriorityAssembler<Map<String, String>, Map<String, String>, String> priorityAssembler = PriorityAssembler.from(new PriorityAssembler.TypeReference<Map<String, String>>() {
                }, new PriorityAssembler.TypeReference<Map<String, String>>() {
                }, new PriorityAssembler.TypeReference<String>() {
                })
                .initConfig(configList)
                .initPriorityHandler(PriorityMode.ABSOLUTE_VALUE);
        //初始化数据获取逻辑
        for (int i = 0; i < mapByValue.size(); i++) {
            priorityAssembler.addPriorityMatchFunction("配置"+ (i+1), mapByValue.get(i), mapByValue.get(i));
        }

        // 创建对象
        PriorityFetcher<Map<String, String>, Map<String, String>, String> priorityFetcher = priorityAssembler.create();

        // 配置初始化    需要按顺序
        for (PriorityMatchProcessor<Map<String, String>, Map<String, String>, String> priorityMatchProcessor : priorityFetcher.getProcessorList()) {
            System.out.println(priorityMatchProcessor);
        }
        System.out.println();
        // 匹配
        for (Map<String, String> req : reqList) {
            List<PriorityMatchResult<List<Map<String, String>>>> match = priorityFetcher.match(req, true);
            if (match != null) {
                System.out.println("需求：" + req.toString() + "， 配置：" + match.toString());
            } else {
                System.out.println("需求：" + req.toString() + "未命中配置");
            }
        }
    }



    /**
     * 通用配置
     */
    private static List<Map<String, Serializable>> configListRange;

    static {
        // 配置
        configListRange = new ArrayList<>();
        Map<String, Serializable> config1 = new HashMap<>();
        config1.put("p1", "p1-key");
        config1.put("value", "p1-value");
        configListRange.add(config1);

        Map<String, Serializable> config2 = new HashMap<>();
        config2.put("p2", "p2-key");
        config2.put("value", "p2-value");
        configListRange.add(config2);

        Map<String, Serializable> config3 = new HashMap<>();
        config3.put("p3", "p3-key");
        config3.put("value", "p3-value");
        configListRange.add(config3);

        Map<String, Serializable> config4 = new HashMap<>();
        config4.put("p3", "p3-key");
        config4.put("p1", "p1-key");
        config4.put("value", "p3|p2-value");
        configListRange.add(config4);

        Map<String, Serializable> config5 = new HashMap<>();
        config5.put("p3", "p3-key");
        config5.put("p2", "p2-key");
        config5.put("p1", "p1-key");
        config5.put("value", "p3|p2|p1-value");
        configListRange.add(config5);


        configListRange.add(new HashMap(){{
            this.put("p1", "p1-key");
            this.put("p4", Range.between(1,10));
            this.put("value", "p1|p4-value");
        }});

        configListRange.add(new HashMap(){{
            this.put("p2", "p2-key");
            this.put("p3", "p3-key");
            this.put("value", "p2|p3-value");
        }});


    }

    /**
     * 需求集合
     */
    private static List<Map<String, Serializable>> reqListRange;

    static {
        //需求
        reqListRange = new ArrayList<>();
        Map<String, Serializable> req1 = new HashMap<>();
        req1.put("p1", "p1-key");
        reqListRange.add(req1);

        Map<String, Serializable> req2 = new HashMap<>();
        req2.put("p2", "p2-key");
        reqListRange.add(req2);

        Map<String, Serializable> req3 = new HashMap<>();
        req3.put("p3", "p3-key");
        reqListRange.add(req3);

        Map<String, Serializable> req4 = new HashMap<>();
        req4.put("p3", "p3-key");
        req4.put("p1", "p1-key");
        reqListRange.add(req4);

        Map<String, Serializable> req5 = new HashMap<>();
        req5.put("p3", "p3-key");
        req5.put("p2", "p2-key");
        req5.put("p1", "p1-key");
        reqListRange.add(req5);

        Map<String, Serializable> req6 = new HashMap<>();
        req6.put("p3", "p3-key");
        req6.put("p2", "p2-key");
        reqListRange.add(req6);

        Map<String, Serializable> req7 = new HashMap<>();
        req7.put("p3", "p3-key");
        req7.put("p2", "XXX");
        req7.put("p1", "p1-key");
        reqListRange.add(req7);

        Map<String, Serializable> req8 = new HashMap<>();
        req8.put("p3", "XXX");
        reqListRange.add(req8);

        reqListRange.add(new HashMap(){{
            this.put("p3", null);
        }});
        reqListRange.add(new HashMap(){{
            this.put("p1", "p1-key");
            this.put("p4", 5);
        }});
    }

    /**
     * 区间匹配测试
     */
    @Test
    public void testMatchRange() {
        // 这里使用有序的哈希MAP， 保证优先级的顺序的对的，这个逻辑必须
        List<Function<Map<String, Serializable>, Serializable>> mapByValue = new ArrayList<>();
        // key值为维度的获取方式， value值为价值，值越高价值越大
        mapByValue.add(map -> map.get("p1"));
        mapByValue.add(map -> map.get("p2"));
        mapByValue.add(map -> map.get("p3"));
        mapByValue.add(map -> map.get("p4"));
        //初始化配置优先级组合集
        PriorityAssembler<Map<String, Serializable>, Map<String, Serializable>, Serializable> priorityAssembler = PriorityAssembler.from(new PriorityAssembler.TypeReference<Map<String, Serializable>>() {
                }, new PriorityAssembler.TypeReference<Map<String, Serializable>>() {
                }, new PriorityAssembler.TypeReference<Serializable>() {
                })
                .initConfig(configListRange)
                .initPriorityHandler(PriorityMode.ABSOLUTE_VALUE);
        //初始化数据获取逻辑
        for (int i = 0; i < mapByValue.size()-1; i++) {
            priorityAssembler.addPriorityMatchFunction("配置"+ (i+1), mapByValue.get(i), mapByValue.get(i));
        }
        priorityAssembler.addPriorityMatchFunction("配置"+ 4, mapByValue.get(3), mapByValue.get(3), (source, config) -> {
            if (config instanceof Range && source instanceof Integer) {
                Range<Integer> range = (Range) config;
                return range.contains((Integer) source);
            }
            return false;
        });


        // 创建对象
        PriorityFetcher<Map<String, Serializable>, Map<String, Serializable>, Serializable> priorityFetcher = priorityAssembler.create();

        // 配置初始化    需要按顺序
        for (PriorityMatchProcessor<Map<String, Serializable>, Map<String, Serializable>, Serializable> priorityMatchProcessor : priorityFetcher.getProcessorList()) {
            System.out.println(priorityMatchProcessor);
        }
        System.out.println();
        // 匹配
        for (Map<String, Serializable> req : reqListRange) {
            PriorityMatchResult<List<Map<String, Serializable>>> match = priorityFetcher.match(req);
            if (match != null) {
                System.out.println("需求：" + req.toString() + "， 配置：" + match.toString() +",配置值:"+match.getResult());
            } else {
                System.out.println("需求：" + req.toString() + "未命中配置");
            }
        }
    }
}
