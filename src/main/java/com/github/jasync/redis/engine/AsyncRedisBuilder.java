package com.github.jasync.redis.engine;

import com.github.jasync.redis.AsyncRedisClient;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class AsyncRedisBuilder {

    public static AsyncRedisClient start(AsyncRedisConfiguration configuration) {
        String host = configuration.getHost();
        if (host == null || host.length() == 0) {
            throw new IllegalStateException("The host can not be empty");
        }
        int port = configuration.getPort();
        if (port < 1) {
            throw new IllegalStateException("The port can not be less then 1");
        }
        int db = configuration.getDb();
        int maxChannels = configuration.getMaxActiveChannels();
        if (maxChannels < 1) {
            throw new IllegalStateException("The max-active-channels can not be less then 1");
        }
        int minChannels = configuration.getMinActiveChannels();
        if (minChannels < 0) {
            minChannels = 0;
        }
        if (minChannels > maxChannels) {
            maxChannels = minChannels;
        }
        long idleTimeout = configuration.getMaxIdleChannelTimeout();
        if (idleTimeout < 10000) {
            throw new IllegalStateException("The idle-channel-timeout can not be less then 10 seconds");
        }
        int consumers = configuration.getConsumers();
        if (consumers < 1) {
            consumers = 1;
        }
        long commandTimeout = configuration.getCommandTimeout();
        if (commandTimeout < 1000) {
            commandTimeout = 1000;
        }
        commandTimeout = TimeUnit.SECONDS.toSeconds(commandTimeout);
        int queueSize = configuration.getMaxPendingCommands();
        if (queueSize < 10) {
            queueSize = 10;
        }

        AsyncRedisChannelPool channelPool = new AsyncRedisChannelPool(host, port, db, maxChannels, minChannels, idleTimeout, consumers);
        AsyncRedisClientImpl asyncRedisClient = new AsyncRedisClientImpl(channelPool, new LinkedBlockingQueue<>(queueSize), consumers);

        channelPool.start((int) commandTimeout);
        asyncRedisClient.start(consumers);

        return asyncRedisClient;
    }

}
