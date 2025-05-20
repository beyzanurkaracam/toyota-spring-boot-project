package toyota.example.toyota_project.Config;
import toyota.example.toyota_project.Helpers.Logging.LoggingHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import toyota.example.toyota_project.Helpers.Logging.LoggingHelper;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private static final Map<String, List<String>> dependencyMap = new HashMap<>();

    static {
        try (InputStream inputStream = ConfigLoader.class.getResourceAsStream("/calculation_dependencies.json")) {
            if (inputStream == null) {
                throw new RuntimeException("calculation_dependencies.json not found in classpath!");
            }
            ObjectMapper mapper = new ObjectMapper();
            dependencyMap.putAll(mapper.readValue(inputStream, new TypeReference<>() {}));
        } catch (IOException e) {
            throw new RuntimeException("Bağımlılık konfigürasyonu yüklenemedi", e);
        }
    }

    public static List<String> getDependentSymbols(String baseSymbol) {
        return dependencyMap.getOrDefault(baseSymbol, new ArrayList<>());
    }
    public Map<String, Double> loadRates() {
        Map<String, Double> rates = new HashMap<>();
        Properties properties = new Properties();

        try (FileInputStream input = new FileInputStream(configFilePath)) {
            properties.load(input);

           
            for (String key : properties.stringPropertyNames()) {
                if (key.startsWith("rates.")) { 
                    String symbol = key.substring(6); 
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
            int port = Integer.parseInt(properties.getProperty("server.port", "8081")); 
            LoggingHelper.logInfo("Loaded server port: {}", port);
            return port;
        } catch (IOException e) {
            LoggingHelper.logError("Error loading port from config file", e);
            return 8081; 
        }
    }
    public static List<String> getSubscriptionsForPlatform(String platformName) {
        try {
           
            Properties props = loadProperties( platformName + ".properties");
            
            
            return Optional.ofNullable(props.getProperty("subscriptions"))
                    .map((String s) -> Arrays.stream(s.split(","))
                                             .map(String::trim) 
                                             .filter(rate -> !rate.isEmpty())
                                             .collect(Collectors.toList()))
                    .orElse(Collections.emptyList());

            
        } catch (RuntimeException e) {
            logger.error("Failed to load subscriptions for platform: {}", platformName, e);
            return Collections.emptyList(); 
        }
    }

    
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
            
            
            int collectorCount = Integer.parseInt(props.getProperty("collectors.count", "0"));
            LoggingHelper.logInfo("Loading {} collector configurations", collectorCount);
            
            
            for (int i = 1; i <= collectorCount; i++) {
                String prefix = "collector." + i + ".";
                
                CollectorConfig config = new CollectorConfig();
                config.setName(getRequiredProperty(props, prefix + "name", configFilePath));
                config.setClassName(getRequiredProperty(props, prefix + "className", configFilePath));
                config.setConfigFile(props.getProperty(prefix + "configFile"));
                
                
                config.setUserName(props.getProperty(prefix + "userName"));
                config.setPassword(props.getProperty(prefix + "password"));
                
                
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