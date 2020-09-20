package com.github.jasync.redis.engine;

import com.github.jasync.redis.RedisReport;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;

public class AsyncRedisCommands {

    private final boolean isSingle;
    private final CompletableFuture<RedisReport> listenableFuture;
    private final Queue<AsyncRedisCommand> asyncRedisCommand;

    AsyncRedisCommands(Queue<AsyncRedisCommand> asyncRedisCommand, boolean isSingle, CompletableFuture<RedisReport> listenableFuture) {
        this.asyncRedisCommand = asyncRedisCommand;
        this.isSingle = isSingle;
        this.listenableFuture = listenableFuture;
    }

    boolean isSingle() {
        return isSingle;
    }

    CompletableFuture<RedisReport> getListenableFuture() {
        return listenableFuture;
    }

    AsyncRedisCommand getNextCommand() {
        return asyncRedisCommand.poll();
    }


}
