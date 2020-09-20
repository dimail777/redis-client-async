package com.github.jasync.redis.utils;

import com.github.jasync.redis.engine.AsyncRedisChannelPool;
import com.github.jasync.redis.engine.NettyRedisHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.redis.RedisArrayAggregator;
import io.netty.handler.codec.redis.RedisBulkStringAggregator;
import io.netty.handler.codec.redis.RedisDecoder;
import io.netty.handler.codec.redis.RedisEncoder;
import io.netty.handler.timeout.ReadTimeoutHandler;

public class NettyChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final int commandTimeout;
    private final AsyncRedisChannelPool pool;

    public NettyChannelInitializer(int commandTimeout, AsyncRedisChannelPool pool) {
        this.commandTimeout = commandTimeout;
        this.pool = pool;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new RedisDecoder());
        pipeline.addLast(new RedisBulkStringAggregator());
        pipeline.addLast(new RedisArrayAggregator());
        pipeline.addLast(new RedisEncoder());
        pipeline.addLast(new ReadTimeoutHandler(commandTimeout));
        pipeline.addLast(new NettyRedisHandler(pool));
    }

}
