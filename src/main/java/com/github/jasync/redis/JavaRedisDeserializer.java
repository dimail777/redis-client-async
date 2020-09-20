package com.github.jasync.redis;

public interface JavaRedisDeserializer {

    Object deserialize(byte[] bytes);

}
