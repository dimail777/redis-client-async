package com.github.jasync.redis;

import java.util.concurrent.CompletableFuture;

public interface RedisQuery {

    RedisQuery withQuery(String command, String key, Object... args);

    RedisQuery withQuery(String command, String key, JavaRedisSerializer serializer, Object... args);

    RedisQuery withQuery(String command, String key, JavaRedisDeserializer deserializer, Object... args);

    RedisQuery withQuery(String command, String key, JavaRedisSerializer serializer, JavaRedisDeserializer deserializer, Object... args);

    RedisQuery withScript(String script, Object... args);

    RedisQuery withScript(String script, JavaRedisSerializer serializer, Object... args);

    RedisQuery withScript(String script, JavaRedisDeserializer deserializer, Object... args);

    RedisQuery withScript(String script, JavaRedisSerializer serializer, JavaRedisDeserializer deserializer, Object... args);

    CompletableFuture<RedisReport> proceed();

}
