package cn.ykccchen.businessutil.match.handler;

/**
 * 配置键的匹配类型。
 *
 * @author ykccchen
 * @version 1.0
 * @since 1.0
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
