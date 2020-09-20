package com.github.jasync.redis.engine;
import com.github.jasync.redis.RedisReport;
import com.github.jasync.redis.JavaRedisDeserializer;
import com.github.jasync.redis.RedisQuery;
import com.github.jasync.redis.JavaRedisSerializer;
import com.github.jasync.redis.utils.RedisConverter;
import io.netty.handler.codec.redis.RedisMessage;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class AsyncRedisRequest implements RedisQuery {

    private int countOfCommands = 0;
    private final Queue<AsyncRedisCommand> listOfCommands;
    private final AsyncRedisClientImpl client;

    AsyncRedisRequest(AsyncRedisClientImpl client) {
        this.client = client;
        this.listOfCommands = new ConcurrentLinkedQueue<>();
    }

    @Override
    public RedisQuery withQuery(String command, String key, JavaRedisSerializer serializer, Object... args) {
        return add(RedisConverter.createRequest(command, key, serializer, args), null);
    }

    @Override
    public RedisQuery withQuery(String command, String key, JavaRedisDeserializer deserializer, Object... args) {
        return add(RedisConverter.createRequest(command, key, null, args), deserializer);
    }

    @Override
    public RedisQuery withQuery(String command, String key, JavaRedisSerializer serializer, JavaRedisDeserializer deserializer, Object... args) {
        return add(RedisConverter.createRequest(command, key, serializer, args), deserializer);
    }

    @Override
    public RedisQuery withQuery(String command, String key, Object... args) {
        return add(RedisConverter.createRequest(command, key, null, args), null);
    }

    @Override
    public RedisQuery withScript(String script, JavaRedisSerializer serializer, Object... args) {
        return add(RedisConverter.createRequest(script, null, serializer, args), null);
    }

    @Override
    public RedisQuery withScript(String script, JavaRedisDeserializer deserializer, Object... args) {
        return add(RedisConverter.createRequest(script, null, null, args), deserializer);
    }

    @Override
    public RedisQuery withScript(String script, JavaRedisSerializer serializer, JavaRedisDeserializer deserializer, Object... args) {
        return add(RedisConverter.createRequest(script, null, serializer, args), deserializer);
    }

    @Override
    public RedisQuery withScript(String script, Object... args) {
        return add(RedisConverter.createRequest(script, null, null, args), null);
    }

    @Override
    public CompletableFuture<RedisReport> proceed() {
        if (countOfCommands > 0) {
            return client.runQuery(listOfCommands, countOfCommands == 1);
        }
        throw new IllegalStateException("The list of commands can not be empty");
    }

    private RedisQuery add(RedisMessage newMessage, JavaRedisDeserializer deserializer) {
        listOfCommands.add(new AsyncRedisCommand(newMessage, deserializer));
        countOfCommands++;
        return this;
    }

}
