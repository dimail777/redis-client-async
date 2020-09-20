package com.github.jasync.redis.engine;

import com.github.jasync.redis.JavaRedisDeserializer;
import io.netty.handler.codec.redis.RedisMessage;

public class AsyncRedisCommand {

    private final JavaRedisDeserializer deserializer;
    private final RedisMessage request;

    public AsyncRedisCommand(RedisMessage request, JavaRedisDeserializer deserializer) {
        this.request = request;
        this.deserializer = deserializer;
    }

    public JavaRedisDeserializer getDeserializer() {
        return deserializer;
    }

    public RedisMessage getRequest() {
        return request;
    }
}
