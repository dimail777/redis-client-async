package com.github.jasync.redis;

public interface JavaRedisSerializer {

    byte[] serialize(Object o);

}
