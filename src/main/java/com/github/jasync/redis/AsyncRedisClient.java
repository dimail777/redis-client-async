package com.github.jasync.redis;

import java.util.concurrent.CompletableFuture;

public interface AsyncRedisClient {

    CompletableFuture<RedisReport> runCommand(String command, String key, Object... args);

    CompletableFuture<RedisReport> runScript(String script, Object... args);

    RedisQuery buildQuery();

    void shutdown();

}
