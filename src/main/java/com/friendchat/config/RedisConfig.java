package com.friendchat.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * Named "jsonRedisTemplate" — avoids conflict with Spring Boot's
     * auto-configured "redisTemplate" and "stringRedisTemplate" beans.
     */
    @Bean("jsonRedisTemplate")
    public RedisTemplate<String, Object> jsonRedisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        StringRedisSerializer str = new StringRedisSerializer();
        template.setKeySerializer(str);
        template.setHashKeySerializer(str);

        // Use a fresh ObjectMapper here — NOT the application one — to
        // avoid polluting global serialization with type metadata.
        ObjectMapper redisMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .activateDefaultTyping(
                        BasicPolymorphicTypeValidator.builder()
                                .allowIfSubType(Object.class).build(),
                        ObjectMapper.DefaultTyping.NON_FINAL,
                        JsonTypeInfo.As.PROPERTY);

        Jackson2JsonRedisSerializer<Object> json =
                new Jackson2JsonRedisSerializer<>(redisMapper, Object.class);
        template.setValueSerializer(json);
        template.setHashValueSerializer(json);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Named "presenceRedisTemplate" for simple string presence keys.
     * Avoids conflict with Spring Boot's auto-configured stringRedisTemplate.
     */
    @Bean("presenceRedisTemplate")
    public RedisTemplate<String, String> presenceRedisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        StringRedisSerializer ser = new StringRedisSerializer();
        template.setKeySerializer(ser);
        template.setValueSerializer(ser);
        template.afterPropertiesSet();
        return template;
    }
}
