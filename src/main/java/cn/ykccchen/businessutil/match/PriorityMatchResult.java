package cn.ykccchen.businessutil.match;

/**
 * 匹配结果对象。
 *
 * @author ykccchen
 * @version 1.0
 * @since 1.0
 */
public class PriorityMatchResult<T> {

    private String uniqueId;
    private String name;
    private String nameAndValue;
    private Integer level;
    private T result;

    /**
     * Creates a result using the legacy signature. NAME:VALUE metadata is null.
     *
     * @param uniqueId processor identity
     * @param name joined dimension names
     * @param level number of active dimensions
     * @param result business result
     */
    public PriorityMatchResult(String uniqueId, String name, Integer level, T result) {
        this(uniqueId, name, null, level, result);
    }

    /**
     * Creates a result with concrete matched-dimension metadata.
     *
     * @param uniqueId processor identity
     * @param name joined dimension names
     * @param nameAndValue formatted NAME:VALUE path or paths
     * @param level number of active dimensions
     * @param result business result
     */
    public PriorityMatchResult(String uniqueId,
                               String name,
                               String nameAndValue,
                               Integer level,
                               T result) {
        this.uniqueId = uniqueId;
        this.name = name;
        this.nameAndValue = nameAndValue;
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

    /**
     * Returns the matched NAME:VALUE metadata. Dimensions use {@code _} and
     * distinct aggregated paths use {@code ;} with the default assembly rule.
     * Legacy directly constructed results may return null.
     *
     * @return formatted matched dimensions, an empty string, or null for the legacy constructor
     */
    public String getNameAndValue() {
        return nameAndValue;
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
                ", nameAndValue='" + nameAndValue + '\'' +
                ", level=" + level +
                '}';
    }
}
