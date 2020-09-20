package com.github.jasync.redis.engine;

import com.github.jasync.redis.RedisReport;
import com.github.jasync.redis.AsyncRedisClient;
import com.github.jasync.redis.RedisQuery;
import com.github.jasync.redis.utils.RedisConverter;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncRedisClientImpl implements AsyncRedisClient {

    private final AtomicBoolean started;
    private final ExecutorService executorService;
    private final AsyncRedisChannelPool connectionPool;
    private final BlockingQueue<AsyncRedisCommands> messageQueue;

    public CompletableFuture<RedisReport> runCommand(String command, String key, Object... args) {
        Queue<AsyncRedisCommand> commands = new LinkedList<>();
        commands.add(new AsyncRedisCommand(RedisConverter.createRequest(command, key, null, args), null));
        return runQuery(commands, true);
    }

    public CompletableFuture<RedisReport> runScript(String script, Object... args) {
        Queue<AsyncRedisCommand> commands = new LinkedList<>();
        commands.add(new AsyncRedisCommand(RedisConverter.createRequest(script, null, null, args), null));
        return runQuery(commands, true);
    }

    public RedisQuery buildQuery() {
        return new AsyncRedisRequest(this);
    }

    public void shutdown() {
        if (!started.compareAndSet(true, false)) {
            return;
        }
        executorService.shutdownNow();
        messageQueue.clear();
        connectionPool.stop();
    }

    CompletableFuture<RedisReport> runQuery(Queue<AsyncRedisCommand> commands, boolean isSingle) {
        if (!started.get()) {
            throw new IllegalStateException("The client has been stopped");
        }
        CompletableFuture<RedisReport> completableFuture = new CompletableFuture<>();
        putCommand(new AsyncRedisCommands(commands, isSingle, completableFuture));
        return completableFuture;
    }

    AsyncRedisClientImpl(AsyncRedisChannelPool connectionPool, BlockingQueue<AsyncRedisCommands> messageQueue, int consumers) {
        this.started = new AtomicBoolean(false);
        this.executorService = Executors.newFixedThreadPool(consumers);
        this.connectionPool = connectionPool;
        this.messageQueue = messageQueue;
    }

    void start(int consumers) {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        for (int i = 0; i < consumers; i++) {
            executorService.submit(this::consume);
        }
    }

    private void putCommand(AsyncRedisCommands command) {
        boolean offered = messageQueue.offer(command);
        if (!offered) {
            throw new IllegalStateException("The queue is full");
        }
    }

    private void consume() {
        while (!Thread.interrupted()) {
            AsyncRedisChannel channel;
            AsyncRedisCommands commands;
            try {
                commands = messageQueue.take();
                channel = connectionPool.nextChannel();
            } catch (InterruptedException iex) {
                return;
            } catch (Exception ex) {
                ex.printStackTrace();
                continue;
            }
            try {
                channel.send(commands);
            } catch (RuntimeException ex) {
                ex.printStackTrace();
            }
        }
    }

}
