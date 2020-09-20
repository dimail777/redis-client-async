package com.github.jasync.redis.engine;

import com.github.jasync.redis.RedisReport;
import io.netty.channel.Channel;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

public class AsyncRedisChannel {

    private final Channel nettyChannel;
    private final AtomicReference<ChannelInfo> channelInfo;
    private volatile long lastSeen;

    AsyncRedisChannel(Channel nettyChannel, boolean established) {
        this.nettyChannel = nettyChannel;
        this.channelInfo = new AtomicReference<>(new ChannelInfo(null, established));
        this.lastSeen = Instant.now().toEpochMilli();
    }

    void complete(RedisReport response, Throwable error) {
        boolean finish;
        do {
            ChannelInfo currentChannelInfo = channelInfo.get();
            if (currentChannelInfo.currentCommand != null) {
                if (response != null) {
                    currentChannelInfo.currentCommand.getListenableFuture().complete(response);
                } else if (error != null) {
                    currentChannelInfo.currentCommand.getListenableFuture().completeExceptionally(error);
                }
            }
            finish = channelInfo.compareAndSet(currentChannelInfo, new ChannelInfo(null, true));
        } while (!finish);
        lastSeen = Instant.now().toEpochMilli();
    }

    void send(AsyncRedisCommands asyncRedisCommands) {
        boolean finish;
        do {
            ChannelInfo currentChannelInfo = channelInfo.get();
            if (!currentChannelInfo.established) {
                asyncRedisCommands.getListenableFuture().completeExceptionally(
                        new RuntimeException("The channel has been closed")
                );
                return;
            }
            if (currentChannelInfo.currentCommand != null) {
                asyncRedisCommands.getListenableFuture().completeExceptionally(
                        new RuntimeException("The handler has been taken with another request")
                );
                return;
            }
            finish = channelInfo.compareAndSet(currentChannelInfo, new ChannelInfo(asyncRedisCommands, true));
        } while (!finish);
        nettyChannel.writeAndFlush(NEW_COMMAND);
        lastSeen = Instant.now().toEpochMilli();
    }

    boolean terminate(Throwable cause) {
        boolean done;
        do {
            ChannelInfo currentChannelInfo = channelInfo.get();
            if (currentChannelInfo.currentCommand != null) {
                cause = cause == null ? new RuntimeException("The channel has been closed") : cause;
                currentChannelInfo.currentCommand.getListenableFuture().completeExceptionally(cause);
            }
            if (!currentChannelInfo.established) {
                return false;
            }
            done = channelInfo.compareAndSet(currentChannelInfo, new ChannelInfo(null, false));
        } while (!done);
        return true;
    }

    boolean close() {
        boolean done = terminate(null);
        if (done) {
            nettyChannel.close();
        }
        return done;
    }

    boolean isNotEstablished() {
        return !channelInfo.get().established;
    }

    long getLastSeen() {
        return this.lastSeen;
    }

    AsyncRedisCommand nextCommand() {
        AsyncRedisCommands commands = channelInfo.get().currentCommand;
        if (commands == null) {
            return null;
        }
        return commands.getNextCommand();
    }

    Boolean isOnlyOneCommand() {
        AsyncRedisCommands commands = channelInfo.get().currentCommand;
        if (commands == null) {
            return null;
        }
        return commands.isSingle();
    }

    private static class ChannelInfo {
        final AsyncRedisCommands currentCommand;
        final boolean established;

        public ChannelInfo(AsyncRedisCommands currentCommand, boolean established) {
            this.currentCommand = currentCommand;
            this.established = established;
        }
    }

    private static final Object NEW_COMMAND = new Object();

}
