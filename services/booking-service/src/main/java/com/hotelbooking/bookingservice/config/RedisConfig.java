// package com.hotelbooking.bookingservice.config;


// import com.fasterxml.jackson.annotation.JsonTypeInfo;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.fasterxml.jackson.databind.SerializationFeature;
// import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
// import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
// import org.springframework.cache.annotation.EnableCaching;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.data.redis.cache.RedisCacheConfiguration;
// import org.springframework.data.redis.cache.RedisCacheManager;
// import org.springframework.data.redis.connection.RedisConnectionFactory;
// import org.springframework.data.redis.core.RedisTemplate;
// import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
// import org.springframework.data.redis.serializer.RedisSerializationContext;
// import org.springframework.data.redis.serializer.StringRedisSerializer;

// import java.time.Duration;

// @Configuration
// @EnableCaching
// public class RedisConfig {
//     private ObjectMapper buildRedisObjectMapper() {
//         ObjectMapper mapper = new ObjectMapper();
//         mapper.registerModule(new JavaTimeModule());
//         mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//         mapper.activateDefaultTyping(
//                 LaissezFaireSubTypeValidator.instance,
//                 ObjectMapper.DefaultTyping.EVERYTHING,
//                 JsonTypeInfo.As.PROPERTY
//         );
//         return mapper;
//     }

//     @Bean
//     public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
//         RedisTemplate<String, Object> template = new RedisTemplate<>();
//         template.setConnectionFactory(connectionFactory);

//         GenericJackson2JsonRedisSerializer jsonSerializer =
//                 new GenericJackson2JsonRedisSerializer(buildRedisObjectMapper());

//         template.setKeySerializer(new StringRedisSerializer());
//         template.setHashKeySerializer(new StringRedisSerializer());
//         template.setValueSerializer(jsonSerializer);
//         template.setHashValueSerializer(jsonSerializer);
//         template.afterPropertiesSet();
//         return template;
//     }

//     @Bean
//     public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
//         GenericJackson2JsonRedisSerializer jsonSerializer =
//                 new GenericJackson2JsonRedisSerializer(buildRedisObjectMapper());

//         RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
//                 .serializeKeysWith(
//                         RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
//                 .serializeValuesWith(
//                         RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
//                 .disableCachingNullValues();

//         return RedisCacheManager.builder(connectionFactory)
//                 .cacheDefaults(defaultConfig.entryTtl(Duration.ofMinutes(10)))
//                 .build();
//     }
// }