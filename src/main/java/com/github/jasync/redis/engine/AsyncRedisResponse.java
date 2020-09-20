package com.github.jasync.redis.engine;

import com.github.jasync.redis.RedisReport;

import java.util.List;

public final class AsyncRedisResponse implements RedisReport {

    private Object result;

    public AsyncRedisResponse(Object value) {
        this.result = value;
    }

    public Long getNumber() {
        return result instanceof Long ? (Long) result : null;
    }

    public String getString() {
        return result instanceof String ? (String) result : null;
    }

    public Object getType() {
        return result;
    }

    public <T> T getType(Class<T> cast) {
        return cast.cast(result);
    }

    @SuppressWarnings("unchecked")
    public List<Object> getList() {
        if (result == null) {
            return null;
        }
        return List.class.isAssignableFrom(result.getClass()) ? (List<Object>) result : null;
    }

}
