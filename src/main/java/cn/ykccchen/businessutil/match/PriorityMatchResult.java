package cn.ykccchen.businessutil.match;

/**
 * @author ykccchen
 * @version 1.0
 * @description 匹配结果对象
 * @date 2025/2/5 14:28
 */
public class PriorityMatchResult<T> {

    private String uniqueId;
    private String name;
    private Integer level;
    private T result;

    public PriorityMatchResult(String uniqueId, String name, Integer level, T result) {
        this.uniqueId = uniqueId;
        this.name = name;
        this.result = result;
        this.level = level;
    }

    private PriorityMatchResult() {
    }

    public String getName() {
        return name;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public T getResult() {
        return result;
    }

    public Integer getLevel() {
        return level;
    }

    @Override
    public String toString() {
        return "PriorityMatchResult{" +
                "uniqueId='" + uniqueId + '\'' +
                ", name='" + name + '\'' +
                ", level=" + level +
                '}';
    }
}
