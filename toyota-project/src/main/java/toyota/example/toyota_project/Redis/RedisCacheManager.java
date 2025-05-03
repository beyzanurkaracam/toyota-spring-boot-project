package toyota.example.toyota_project.Redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import toyota.example.toyota_project.Entities.RateFields;

//@Component
public class RedisCacheManager {
	private final RedisTemplate<String, RateFields> redisTemplate;
    private static final String CACHE_PREFIX = "rates:";

    @Autowired
    public RedisCacheManager(RedisTemplate<String, RateFields> rateFieldsRedisTemplate) {
        this.redisTemplate = rateFieldsRedisTemplate;
    }

	    public void put(String rateName, RateFields rateFields) {
	        redisTemplate.opsForValue().set(CACHE_PREFIX + rateName, rateFields);
	    }

	    public RateFields get(String rateName) {
	        return redisTemplate.opsForValue().get(CACHE_PREFIX + rateName);
	    }

	    public void delete(String rateName) {
	        redisTemplate.delete(CACHE_PREFIX + rateName);
	    }

}
