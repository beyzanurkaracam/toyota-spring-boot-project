package toyota.example.toyota_project.Redis;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import toyota.example.toyota_project.Entities.RateFields;
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, RateFields> rateFieldsRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, RateFields> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Key ve Value Serializer'ları
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(RateFields.class));
        
        template.afterPropertiesSet();
        return template;
    }
}