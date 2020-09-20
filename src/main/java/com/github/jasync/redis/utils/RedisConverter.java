package com.github.jasync.redis.utils;

import com.github.jasync.redis.JavaRedisDeserializer;
import com.github.jasync.redis.JavaRedisSerializer;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.CodecException;
import io.netty.handler.codec.redis.*;
import io.netty.util.CharsetUtil;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RedisConverter {

    public static RedisMessage createRequest(String command, String key, JavaRedisSerializer serializer, Object... args) {
        int commands = key == null ? 1 : 2;
        if (args != null) {
            commands = commands + args.length;
        }
        List<RedisMessage> children = new ArrayList<>(commands);
        children.add(getStringMessage(command));
        if (key != null) {
            children.add(getStringMessage(key));
        }
        if (args == null) {
            return new ArrayRedisMessage(children);
        }
        if (serializer == null) {
            fillArrayStrings(children, args);
        } else {
            fillArrayObjects(children, args, serializer);
        }
        return new ArrayRedisMessage(children);
    }

    public static Object getResponse(JavaRedisDeserializer deserializer, RedisMessage redisMessage) {
        if (redisMessage instanceof SimpleStringRedisMessage) {
            return ((SimpleStringRedisMessage) redisMessage).content();
        }
        if (redisMessage instanceof IntegerRedisMessage) {
            return ((IntegerRedisMessage) redisMessage).value();
        }
        if (redisMessage instanceof FullBulkStringRedisMessage) {
            FullBulkStringRedisMessage fullBulkRedisMessage = (FullBulkStringRedisMessage) redisMessage;
            if (deserializer != null) {
                return getObject(deserializer, fullBulkRedisMessage);
            } else {
                return getString(fullBulkRedisMessage);
            }
        }
        if (redisMessage instanceof ArrayRedisMessage) {
            return getAggregatedRedisResponse(deserializer, (ArrayRedisMessage) redisMessage);
        }
        if (redisMessage instanceof ErrorRedisMessage) {
            throw new RuntimeException(((ErrorRedisMessage) redisMessage).toString());
        }
        throw new CodecException("Unknown message type: " + redisMessage.getClass().getName());
    }

    private static List<Object> getAggregatedRedisResponse(JavaRedisDeserializer deserializer, ArrayRedisMessage msg) {
        List<Object> list = new ArrayList<>(msg.children().size());
        for (RedisMessage child : msg.children()) {
            list.add(getResponse(deserializer, child));
        }
        return list;
    }

    private static Object getObject(JavaRedisDeserializer deserializer, FullBulkStringRedisMessage msg) {
        byte[] bytes = new byte[msg.content().readableBytes()];
        int readerIndex = msg.content().readerIndex();
        msg.content().getBytes(readerIndex, bytes);
        return deserializer.deserialize(bytes);
    }

    private static String getString(FullBulkStringRedisMessage msg) {
        if (msg.isNull()) {
            return null;
        }
        return msg.content().toString(CharsetUtil.UTF_8);
    }

    private static void fillArrayStrings(List<RedisMessage> children, Object[] args) {
        for (Object arg : args) {
            children.add(getStringMessage(objectString(arg)));
        }
    }

    private static void fillArrayObjects(List<RedisMessage> children, Object[] args, JavaRedisSerializer serializer) {
        for (Object arg : args) {
            children.add(getObjectMessage(arg, serializer));
        }
    }

    private static RedisMessage getStringMessage(String string) {
        return new FullBulkStringRedisMessage(Unpooled.wrappedBuffer(string.getBytes(StandardCharsets.UTF_8)));
    }

    private static RedisMessage getObjectMessage(Object arg, JavaRedisSerializer serializer) {
        return new FullBulkStringRedisMessage(Unpooled.wrappedBuffer(serializer.serialize(arg)));
    }

    private static String objectString(Object arg) {
        if (arg == null) {
            return "(null)";
        }
        String string = arg.toString();
        if (string == null) {
            return "(null)";
        }
        return string;
    }

}
