package toyota.example.toyota_project.Redis;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;

import toyota.example.toyota_project.Entities.RateFields;

@Configuration
public class RedisConfig {
	@Bean
	public RedisTemplate<String, RateFields> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, RateFields> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        
       
        Jackson2JsonRedisSerializer<RateFields> serializer = 
            new Jackson2JsonRedisSerializer<>(RateFields.class);
        
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.afterPropertiesSet();
        
        return template;
    }

}
