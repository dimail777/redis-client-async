# Async Redis client
redis-async is a Simple, Netty based, asynchronous library for Redis

## Getting started

        AsyncRedisClient client = AsyncRedisBuilder.start(AsyncRedisConfiguration.withDefault());
        RedisQuery query = client.buildQuery();
        CompletableFuture<RedisReport> future = query.withQuery("get", "my-java-object", new MyJavaRedisDeserializer()).proceed();
        future.whenCompleteAsync((report, error) -> {
            MyObject object = report.getType(MyObject.class);
            // To do something
        });
        
## Download

        <repository>
            <id>redis-client-async-mvn-repo</id>
            <url>https://raw.github.com/dimail777/redis-client-async/mvn-repo/</url>
            <snapshots>
                <enabled>true</enabled>
                <updatePolicy>always</updatePolicy>
            </snapshots>
        </repository>

        <dependency>
            <groupId>com.github.redis-client-async</groupId>
            <artifactId>redis-async</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
        
## Reference Documentation
   For further reference, please consider the following sections:
   
   * [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
   
## Support
   If you have any question to contact with me dimail777@gmail.com
