package com.github.jasync.redis;

import java.util.List;

public interface RedisReport {

    Long getNumber();

    String getString();

    Object getType();

    <T> T getType(Class<T> cast);

    List<Object> getList();

}
