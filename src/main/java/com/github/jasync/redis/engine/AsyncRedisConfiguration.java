package com.github.jasync.redis.engine;

import java.util.concurrent.TimeUnit;

public class AsyncRedisConfiguration {

    private String host = "127.0.0.1";
    private int port = 6379, db = -1;
    private int maxActiveChannels = 2, minActiveChannels = 1;
    private int consumers = 1;
    private long maxIdleChannelTimeout = 60000;
    private int maxPendingCommands = 10000;
    private long commandTimeout = 10000;

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getDb() {
        return db;
    }

    public int getMaxActiveChannels() {
        return maxActiveChannels;
    }

    public int getMinActiveChannels() {
        return minActiveChannels;
    }

    public long getMaxIdleChannelTimeout() {
        return maxIdleChannelTimeout;
    }

    public int getMaxPendingCommands() {
        return maxPendingCommands;
    }

    public long getCommandTimeout() {
        return commandTimeout;
    }

    public int getConsumers() {
        return consumers;
    }

    public AsyncRedisConfiguration withHost(String host) {
        this.host = host;
        return this;
    }

    public AsyncRedisConfiguration withPort(int port) {
        this.port = port;
        return this;
    }

    public AsyncRedisConfiguration withDb(int db) {
        this.db = db;
        return this;
    }

    public AsyncRedisConfiguration withMaxActiveChannels(int maxActiveChannels) {
        this.maxActiveChannels = maxActiveChannels;
        return this;
    }

    public AsyncRedisConfiguration withMinActiveChannels(int minActiveChannels) {
        this.minActiveChannels = minActiveChannels;
        return this;
    }

    public AsyncRedisConfiguration withMaxIdleChannelTimeout(long maxIdleChannelTimeout, TimeUnit timeUnit) {
        this.maxIdleChannelTimeout = timeUnit.toMillis(maxIdleChannelTimeout);
        return this;
    }

    public AsyncRedisConfiguration withMaxPendingCommands(int maxPendingCommands) {
        this.maxPendingCommands = maxPendingCommands;
        return this;
    }

    public AsyncRedisConfiguration withCommandTimeout(long commandTimeout, TimeUnit timeUnit) {
        this.commandTimeout = timeUnit.toMillis(commandTimeout);
        return this;
    }

    public AsyncRedisConfiguration withConsumers(int consumers) {
        this.consumers = consumers;
        return this;
    }
}
