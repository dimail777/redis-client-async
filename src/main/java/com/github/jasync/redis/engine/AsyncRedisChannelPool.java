package com.github.jasync.redis.engine;

import com.github.jasync.redis.RedisReport;
import com.github.jasync.redis.utils.NettyChannelInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AsyncRedisChannelPool {

    private final AtomicBoolean started;
    private final String host;
    private final int maxChannels, minChannels;
    private final long idleTimeout;
    private final int port;
    private final int db;
    private final AtomicInteger channels;
    private final ReadWriteLock cleanerLock;
    private final Bootstrap bootstrap;
    private final EventLoopGroup workerGroup;
    private final ConcurrentLinkedDeque<AsyncRedisChannel> persistentChannels;
    private final ScheduledExecutorService scheduler;
    private final LinkedBlockingQueue<AsyncRedisChannel> reverseChannels;

    AsyncRedisChannelPool(String host, int port, int db, int maxChannels, int minChannels, long idleTimeout, int consumers) {
        this.bootstrap = new Bootstrap();
        this.host = host;
        this.port = port;
        this.db = db;
        this.minChannels = minChannels;
        this.maxChannels = maxChannels;
        this.idleTimeout = idleTimeout;
        this.channels = new AtomicInteger(0);
        this.persistentChannels = new ConcurrentLinkedDeque<>();
        this.workerGroup = getLoopGroup(consumers);
        this.reverseChannels = new LinkedBlockingQueue<>(maxChannels);
        if (minChannels == maxChannels) {
            this.scheduler = null;
        } else {
            this.scheduler = Executors.newSingleThreadScheduledExecutor();
        }
        this.cleanerLock = new ReentrantReadWriteLock();
        this.started = new AtomicBoolean(false);
    }

    int getDb() {
        return db;
    }

    AsyncRedisChannel nextChannel() throws InterruptedException {
        AsyncRedisChannel channel;
        do {
            if (!started.get()) {
                throw new IllegalStateException("The channel-pool has been stopped");
            }
            channel = persistentChannels.pollFirst();
            if (channel == null) {
                channel = tryCreateChannel();
            }
        } while (channel == null);
        return channel;
    }

    void updateChannelState(AsyncRedisChannel channel) {
        reverseChannels.add(channel);
    }

    void releaseChannel(AsyncRedisChannel channel, RedisReport response, Throwable cause) {
        channel.complete(response, cause);
        if (!started.get()) {
            forceClose(channel);
            return;
        }
        persistentChannels.addFirst(channel);
    }

    void terminateChannel(AsyncRedisChannel channel, Throwable cause) {
        boolean done = channel.terminate(cause);
        if (done) {
            channels.decrementAndGet();
        }
    }

    void start(int commandTimeout) {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        initBootstrap(commandTimeout);
        if (scheduler != null) {
            this.scheduler.scheduleAtFixedRate(this::clean, idleTimeout / 2000, idleTimeout / 2000, TimeUnit.SECONDS);
        }
    }

    void stop() {
        if (!started.compareAndSet(true, false)) {
            return;
        }
        scheduler.shutdownNow();
        AsyncRedisChannel asyncRedisChannel = persistentChannels.pollLast();
        while (asyncRedisChannel != null) {
            forceClose(asyncRedisChannel);
            asyncRedisChannel = persistentChannels.pollLast();
        }
        workerGroup.shutdownGracefully();
    }

    private AsyncRedisChannel tryCreateChannel() throws InterruptedException {
        if (channels.get() >= maxChannels) {
            return null;
        }
        AsyncRedisChannel channel;
        cleanerLock.readLock().lock();
        try {
            channel = persistentChannels.pollFirst();
            if (channel != null) {
                return channel;
            }
            boolean increased;
            do {
                int current = channels.get();
                if (current >= maxChannels) {
                    return null;
                }
                increased = channels.compareAndSet(current, current + 1);
            } while (!increased);
        } finally {
            cleanerLock.readLock().unlock();
        }
        return makeChannel();
    }

    private AsyncRedisChannel makeChannel() throws InterruptedException {
        AsyncRedisChannel channel;
        do {
            try {
                bootstrap.connect(host, port);
            } catch (Exception ex) {
                ex.printStackTrace();
                Thread.sleep(1000);
                channel = null;
                continue;
            }
            channel = reverseChannels.take();
            if (channel.isNotEstablished()) {
                new RuntimeException("The channel has not been initialized").printStackTrace();
                Thread.sleep(1000);
                channel = null;
            }
        } while (channel == null);
        return channel;
    }

    private void clean() {
        cleanerLock.writeLock().lock();
        AsyncRedisChannel asyncRedisChannel = persistentChannels.pollLast();
        while (asyncRedisChannel != null) {
            if (isExpired(asyncRedisChannel) && channels.get() > minChannels) {
                forceClose(asyncRedisChannel);
                cleanerLock.writeLock().unlock();
            } else {
                persistentChannels.addLast(asyncRedisChannel);
                break;
            }
            cleanerLock.writeLock().lock();
            asyncRedisChannel = persistentChannels.pollLast();
        }
        cleanerLock.writeLock().unlock();
    }

    private void forceClose(AsyncRedisChannel channel) {
        boolean closed = channel.close();
        if (closed) {
            channels.decrementAndGet();
        }
    }

    private boolean isExpired(AsyncRedisChannel asyncRedisChannel) {
        return (Instant.now().toEpochMilli() - asyncRedisChannel.getLastSeen()) > idleTimeout;
    }

    private void initBootstrap(int commandTimeout) {
        bootstrap.group(workerGroup).channel(getSocketChannel()).handler(new NettyChannelInitializer(commandTimeout, this));
    }

    private static EventLoopGroup getLoopGroup(int consumers) {
        return Epoll.isAvailable() ? new EpollEventLoopGroup(consumers) : new NioEventLoopGroup(consumers);
    }

    private static Class<? extends SocketChannel> getSocketChannel() {
        return Epoll.isAvailable() ? EpollSocketChannel.class : NioSocketChannel.class;
    }

}
