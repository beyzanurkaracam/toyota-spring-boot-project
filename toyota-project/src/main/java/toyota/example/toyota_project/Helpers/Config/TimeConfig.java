package toyota.example.toyota_project.Helpers.Config;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import toyota.example.toyota_project.Helpers.Logging.TimestampFormatter;

import org.springframework.beans.factory.annotation.Autowired;


/**
 * Zaman damgası ve diğer global yapılandırma ayarlarını yönetir.
 */
@Configuration
public class TimeConfig {
    private static final Logger logger = LogManager.getLogger(TimeConfig.class);
    
    @Autowired
    private Environment env;
    
    @Value("${app.timestamp.useLocal:true}")
    private boolean useLocalTimestamp;
    
    /**
     * Uygulama başlangıcında çalışır ve yapılandırma değerlerini ayarlar.
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        // Eğer ortamda (environment) değer varsa, onu kullan
        String envUseLocal = env.getProperty("APP_TIMESTAMP_USE_LOCAL");
        if (envUseLocal != null) {
            useLocalTimestamp = Boolean.parseBoolean(envUseLocal);
        }
        
        // TimestampFormatter'ı yapılandır
        TimestampFormatter.setUseLocalTimestamp(useLocalTimestamp);
        logger.info("Zaman damgası yapılandırması: useLocalTimestamp={}", useLocalTimestamp);
    }
}