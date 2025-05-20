package toyota.example.toyota_project.MainApp.Concrete;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import toyota.example.toyota_project.Config.ConfigLoader;
import toyota.example.toyota_project.Entities.Rate;
import toyota.example.toyota_project.Entities.RateFields;
import toyota.example.toyota_project.Helpers.Logging.TimestampFormatter;
import toyota.example.toyota_project.MainApp.Abstract.CoordinatorCallBack;
import toyota.example.toyota_project.MainApp.Abstract.DataCollector;
import toyota.example.toyota_project.Simulation.REST.Responses.RateResponse;


import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

public class RestDataCollector implements DataCollector {
    private static final Logger logger = LogManager.getLogger(RestDataCollector.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final Properties config = new Properties();
    private CoordinatorCallBack callback;
    private String platformName;
    //private ScheduledExecutorService scheduler;
    private volatile boolean isConnected = false;
    private String baseUrl;
    private long pollingInterval;
    private Set<String> subscribedRates = ConcurrentHashMap.newKeySet();
    private Set<String> receivedRates = ConcurrentHashMap.newKeySet();
    @Autowired
    @Qualifier("scheduledExecutorService") 
    private ScheduledExecutorService scheduler;
    private String userId;
    private String password;
    // Zaman damgası formatı
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = 
        DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public void connect(String platformName, String userid, String password) {
        this.platformName = platformName;
        this.userId = userid;
        this.password = password;

        int maxRetries = 3;
        int retryCount = 0;
        boolean connectionSuccess = false;

        while (retryCount < maxRetries && !connectionSuccess) {
            try {
                logger.info("Bağlantı denemesi başlatılıyor ({} / {})", retryCount + 1, maxRetries);

                // Mevcut bağlantı mantığı
                logger.debug("Konfigürasyon dosyası yükleniyor: collectors.properties");
                loadConfig("collectors.properties");
                baseUrl = config.getProperty("rest.baseUrl");
                pollingInterval = Long.parseLong(config.getProperty("rest.pollingInterval", "3000"));

                // Health check
                String healthUrl = baseUrl + "/actuator/health";
                logger.debug("Health check için URL: {}", healthUrl);
                ResponseEntity<String> healthResponse = restTemplate.getForEntity(healthUrl, String.class);

                if (!healthResponse.getStatusCode().is2xxSuccessful()) {
                    logger.error("Health check başarısız. HTTP Status: {}", healthResponse.getStatusCode());
                    throw new ResourceAccessException("Health check failed");
                }

                // Scheduler'ı başlat
                scheduler = Executors.newSingleThreadScheduledExecutor();
                scheduler.scheduleAtFixedRate(
                    this::fetchRates,
                    0,
                    pollingInterval,
                    TimeUnit.MILLISECONDS
                );

                // Başarılı bağlantı
                isConnected = true;
                callback.onConnect(platformName, true);
                connectionSuccess = true;
                logger.info("Bağlantı başarılı: {}", platformName);

            } catch (Exception e) {
                retryCount++;
                logger.error("Bağlantı hatası ({} / {}): {}", retryCount, maxRetries, e.getMessage());

                // Son denemede bile başarısızsa logla ve çık
                if (retryCount >= maxRetries) {
                    logger.error("Maksimum yeniden deneme sayısına ulaşıldı. Bağlantı sağlanamadı.");
                    callback.onConnect(platformName, false);
                    break;
                }

                // 5 saniye bekle (InterruptedException kontrolü ile)
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    logger.error("Bekleme sırasında kesinti: {}", ex.getMessage());
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
    private void fetchRates() {
        if (!isConnected || subscribedRates.isEmpty()) return;

        subscribedRates.forEach(rateName -> {
        	  try {
                  String endpoint = baseUrl + "/api/rates/" + rateName;
                  ResponseEntity<RateResponse> response = restTemplate.getForEntity(
                      endpoint, 
                      RateResponse.class
                  );

                  if (response.getStatusCode().is2xxSuccessful() && response.hasBody()) {
                      RateResponse rateResponse = response.getBody();
                      processRateResponse(rateResponse);
                  } else {
                      logger.warn("Geçersiz yanıt: {}", response.getStatusCode());
                  }

              } catch (Exception e) {
                  logger.error("Veri çekme hatası ({}): {}", rateName, e.getMessage());
              }
        });
     
    }

    private void processRateResponse(RateResponse response) {
        try {
            // Timestamp'i standartlaştır
            String standardizedTimestamp = formatTimestamp(response.getTimestamp());
            
            // RateResponse → RateFields dönüşümü
            RateFields rateFields = new RateFields(
                response.getRateName(),
                response.getBid(),
                response.getAsk(),
                standardizedTimestamp
            );

            boolean isFirstTime = receivedRates.add(response.getRateName());
            
            if (callback != null) {
                if (isFirstTime) {
                    Rate rate = new Rate(
                        response.getBid(),
                        response.getAsk(),
                        standardizedTimestamp
                    );
                    callback.onRateAvailable(platformName, response.getRateName(), rate);
                } else {
                    callback.onRateUpdate(platformName, response.getRateName(), rateFields);
                }
            }
            
            logger.debug("Veri işlendi: {}", response.getRateName());

        } catch (Exception e) {
            logger.error("Veri işleme hatası: {}", e.getMessage());
        }
    }

    private String formatTimestamp(Instant timestamp) {
        // TimestampFormatter kullanarak standartlaştırma
        String rawTimestamp = timestamp != null ? timestamp.toString() : Instant.now().toString();
        return TimestampFormatter.standardizeTimestamp(rawTimestamp);
    }

    @Override
    public void subscribe(String platformName, String rateName) {
        if (!isConnected) {
            logger.error("Bağlantı yok: {}", platformName);
            return;
        }
        subscribedRates.add(rateName);
        logger.info("Abone olundu: {}", rateName);
    }

    @Override
    public void unSubscribe(String platformName, String rateName) {
        subscribedRates.remove(rateName);
        logger.info("Abonelik sonlandırıldı: {}", rateName);
    }

    @Override
    public void disConnect(String platformName, String userid, String password) {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        isConnected = false;
        callback.onDisConnect(platformName, true);
    }

    @Override
    public void loadConfig(String configFile) {
        ConfigLoader loader = new ConfigLoader(configFile);
        Properties loadedProps = loader.loadProperties(configFile);
        this.config.putAll(loadedProps);
        this.platformName = config.getProperty("platform.name");
        
        logger.info("REST Collector konfigürasyonu yüklendi: {}", platformName);
    }

    @Override
    public void setCallBack(Coordinator callback) {
        this.callback = callback;
    }

    @Override
    public String getPlatformName() {
        return platformName;
    }


    @Override
    public String getUserId() {
        return this.userId;
    }

    @Override
    public String getPassword() {
        return this.password;
    }
}