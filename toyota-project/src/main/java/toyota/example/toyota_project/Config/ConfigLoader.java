package toyota.example.toyota_project.Config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import toyota.example.toyota_project.Helpers.*;
import toyota.example.toyota_project.Helpers.Logging.LoggingHelper;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

public class ConfigLoader {
    private final String configFilePath;
    private static final Logger logger = LogManager.getLogger(ConfigLoader.class);

    public ConfigLoader(String configFilePath) {
        this.configFilePath = configFilePath;
    }

    public Map<String, Double> loadRates() {
        Map<String, Double> rates = new HashMap<>();
        Properties properties = new Properties();

        try (FileInputStream input = new FileInputStream(configFilePath)) {
            properties.load(input);

            // Sembol ve fiyatları yükle
            for (String key : properties.stringPropertyNames()) {
                if (key.startsWith("rates.")) { // Sadece rates ile başlayan anahtarları yükle
                    String symbol = key.substring(6); // "rates." kısmını çıkar
                    double rate = Double.parseDouble(properties.getProperty(key));
                    rates.put(symbol, rate);
                    LoggingHelper.logInfo("Loaded rate for {}: {}", symbol, rate);
                }
            }
        } catch (IOException e) {
            LoggingHelper.logError("Error loading rates from config file", e);
        }

        return rates;
    }

    public int loadPort() {
        Properties properties = new Properties();

        try (FileInputStream input = new FileInputStream(configFilePath)) {
            properties.load(input);
            int port = Integer.parseInt(properties.getProperty("server.port", "8081")); // Varsayılan port: 8081
            LoggingHelper.logInfo("Loaded server port: {}", port);
            return port;
        } catch (IOException e) {
            LoggingHelper.logError("Error loading port from config file", e);
            return 8081; // Hata durumunda varsayılan port
        }
    }
    public static List<String> getSubscriptionsForPlatform(String platformName) {
        try {
           
            Properties props = loadProperties("config/" + platformName + ".properties");
            
            
            return Optional.ofNullable(props.getProperty("subscriptions"))
                    .map((String s) -> Arrays.stream(s.split(","))
                                             .map(String::trim) // her ihtimale karşı trim
                                             .filter(rate -> !rate.isEmpty())
                                             .collect(Collectors.toList()))
                    .orElse(Collections.emptyList());

            
        } catch (RuntimeException e) {
            logger.error("Failed to load subscriptions for platform: {}", platformName, e);
            return Collections.emptyList(); // Safe fallback
        }
    }

    // Var olan loadProperties() metodunuz (örnek)
    public static Properties loadProperties(String configFile) {
        Properties props = new Properties();
        try (InputStream input = ConfigLoader.class.getClassLoader().getResourceAsStream(configFile)) {
            if (input == null) {
                throw new FileNotFoundException("Config file not found: " + configFile);
            }
            props.load(input);
            logger.debug("Loaded {} properties from {}", props.size(), configFile);
            return props;
        } catch (IOException e) {
            logger.error("Config load failed for {}", configFile, e);
            throw new RuntimeException("Configuration error", e);
        }
    }


    public static List<CollectorConfig> loadCollectorConfigs(String configFilePath) {
        List<CollectorConfig> configs = new ArrayList<>();
        Properties props = new Properties();
        
        try (FileInputStream input = new FileInputStream(configFilePath)) {
            props.load(input);
            
            // Toplam platform sayısını oku
            int collectorCount = Integer.parseInt(props.getProperty("collectors.count", "0"));
            LoggingHelper.logInfo("Loading {} collector configurations", collectorCount);
            
            // Her bir platform için konfigürasyonları yükle
            for (int i = 1; i <= collectorCount; i++) {
                String prefix = "collector." + i + ".";
                
                CollectorConfig config = new CollectorConfig();
                config.setName(getRequiredProperty(props, prefix + "name", configFilePath));
                config.setClassName(getRequiredProperty(props, prefix + "className", configFilePath));
                config.setConfigFile(props.getProperty(prefix + "configFile"));
                
                // Opsiyonel alanlar
                config.setUserName(props.getProperty(prefix + "userName"));
                config.setPassword(props.getProperty(prefix + "password"));
                
                // Rate'leri listeye çevir
                String rateNames = props.getProperty(prefix + "rateNames", "");
                config.setRateNames(Arrays.asList(rateNames.split(",")));
                
                configs.add(config);
                LoggingHelper.logInfo("Loaded collector config: {}", config.getName());
            }
            
        } catch (IOException e) {
            LoggingHelper.logError("Failed to load collector configs from {}", configFilePath, e);
            throw new RuntimeException("Config loading failed", e);
        } catch (NumberFormatException e) {
            LoggingHelper.logError("Invalid collectors.count format in {}", configFilePath, e);
            throw new RuntimeException("Invalid config format", e);
        }
        
        return configs;
    }

    private static String getRequiredProperty(Properties props, String key, String configPath) {
        String value = props.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            String errorMsg = "Missing required property: " + key + " in " + configPath;
            LoggingHelper.logError(errorMsg);
            throw new RuntimeException(errorMsg);
        }
        return value;
    }
}