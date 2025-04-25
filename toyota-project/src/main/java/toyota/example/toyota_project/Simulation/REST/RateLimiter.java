package toyota.example.toyota_project.Simulation.REST;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RateLimiter {
	 private static final Logger logger = LogManager.getLogger(RateLimiter.class);

	private static final Map<String, Queue<Long>> requestTimestamps = new ConcurrentHashMap<String, Queue<Long>>();
 private static final int MAX_REQUESTS = 10; // 10 istek
    private static final int TIME_WINDOW = 1000; // 1 saniye
    
    public static boolean checkLimit(String symbol) {
    	 logger.trace("Rate limit kontrolü başladı: {}", symbol);

         long now = System.currentTimeMillis();
         Queue<Long> timestamps = requestTimestamps.computeIfAbsent(symbol, 
             k -> new ConcurrentLinkedQueue<>());
         
         // Eski timestampları temizle
         while (!timestamps.isEmpty() && timestamps.peek() < now - TIME_WINDOW) {
             timestamps.poll();
         }
         
         // Limit kontrolü
         if (timestamps.size() >= MAX_REQUESTS) {
             logger.warn("Rate limit aşıldı: {}", symbol);
             return false;
         }
         
         timestamps.offer(now);
         logger.debug("Rate limit kontrolü başarılı: {}", symbol);
         return true;
     }
}