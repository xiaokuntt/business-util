package com.ykccchen.businessutil.match.handler;

/**
 * @author ykccchen
 * @version 1.0
 * @description 配置优先级枚举类
 * @date 2025/5/29 13:38
 */
public enum PriorityMatchType {


    /**
     * 通用对象模式
     */
    COMMON("common", "通用对象模式"),
    /**
     * 布尔类型模式
     */
    BOOLEAN("BOOLEAN", "布尔类型模式"),
    ;



    private final String type;
    private final String desc;


    public String getType() {
        return type;
    }

    public String getDesc() {
        return desc;
    }

    PriorityMatchType(String model, String desc) {
        this.type = model;
        this.desc = desc;
    }


}
