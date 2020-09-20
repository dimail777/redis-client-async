package com.github.jasync.redis.engine;

import com.github.jasync.redis.JavaRedisDeserializer;
import com.github.jasync.redis.utils.RedisConverter;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.redis.RedisMessage;
import io.netty.util.ReferenceCountUtil;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class NettyRedisHandler extends ChannelDuplexHandler {

    private final AsyncRedisChannelPool channelPool;
    private Throwable cause;
    private boolean dbSelected;
    private AsyncRedisChannel redisChannel;
    private Queue<Object> pocket;
    private JavaRedisDeserializer deserializer;

    public NettyRedisHandler(AsyncRedisChannelPool channelPool) {
        this.channelPool = channelPool;
        this.redisChannel = null;
        this.dbSelected = false;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        int db = channelPool.getDb();
        if (db > -1) {
            ctx.writeAndFlush(RedisConverter.createRequest(SELECT, String.valueOf(db), null));
        } else {
            initChannel(ctx);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!dbSelected) {
            initChannel(ctx);
            ReferenceCountUtil.release(msg);
            return;
        }
        AsyncRedisChannel redisChannel = this.redisChannel;
        if (redisChannel == null || redisChannel.isNotEstablished()) {
            return;
        }
        Boolean single = redisChannel.isOnlyOneCommand();
        if (single == null) {
            return;
        }
        RedisMessage redisResponse = (RedisMessage) msg;
        try {
            Object objectResponse = RedisConverter.getResponse(this.deserializer, redisResponse);
            if (single) {
                deserializer = null;
                pocket = null;
                channelPool.releaseChannel(redisChannel, new AsyncRedisResponse(objectResponse), null);
            } else {
                pocket.add(objectResponse);
                AsyncRedisCommand nextAsyncRedisCommand = redisChannel.nextCommand();
                if (nextAsyncRedisCommand == null) {
                    deserializer = null;
                    channelPool.releaseChannel(redisChannel, new AsyncRedisResponse(new ArrayList<>(pocket)), null);
                    pocket = null;
                } else {
                    this.deserializer = nextAsyncRedisCommand.getDeserializer();
                    ctx.writeAndFlush(nextAsyncRedisCommand.getRequest());
                }
            }
        } catch (Throwable ex) {
            deserializer = null;
            pocket = null;
            channelPool.releaseChannel(redisChannel, null, ex);
        } finally {
            ReferenceCountUtil.release(redisResponse);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        AsyncRedisChannel channel = this.redisChannel;
        if (channel == null || channel.isNotEstablished()) {
            return;
        }
        Boolean yes = channel.isOnlyOneCommand();
        if (yes == null) {
            return;
        }
        if (yes) {
            pocket = null;
        } else {
            pocket = new LinkedList<>();
        }
        AsyncRedisCommand asyncRedisCommand = channel.nextCommand();
        this.deserializer = asyncRedisCommand.getDeserializer();
        ctx.write(asyncRedisCommand.getRequest(), promise);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        this.cause = cause;
        ctx.close();
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) {
        AsyncRedisChannel channel = this.redisChannel;
        if (channel == null) {
            this.channelPool.updateChannelState(new AsyncRedisChannel(ctx.channel(), false));
            return;
        }
        this.pocket = null;
        this.deserializer = null;
        this.redisChannel = null;
        this.channelPool.terminateChannel(channel, cause);
        this.cause = null;
    }

    private void initChannel(ChannelHandlerContext ctx) {
        dbSelected = true;
        AsyncRedisChannel newChannel = new AsyncRedisChannel(ctx.channel(), true);
        redisChannel = newChannel;
        channelPool.updateChannelState(newChannel);
    }

    private static final String SELECT = "select";

}