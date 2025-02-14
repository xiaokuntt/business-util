package com.ykccchen.businessutil.dto;

/**
 * @author ykccchen
 * @version 1.0
 * @description 匹配结果对象
 * @date 2025/2/5 14:28
 */
public class KeyMatchResult<T> {
    private final String key;
    private final Integer level;
    private final T result;

    public KeyMatchResult(String key, Integer level, T result) {
        this.key = key;
        this.result = result;
        this.level = level;
    }

    public String getKey() {
        return key;
    }

    public T getResult() {
        return result;
    }

    public Integer getLevel() {
        return level;
    }

    @Override
    public String toString() {
        return "KeyMatchResult{" +
                "key='" + key + '\'' +
                ", level=" + level +
                ", result=" + result +
                '}';
    }
}
