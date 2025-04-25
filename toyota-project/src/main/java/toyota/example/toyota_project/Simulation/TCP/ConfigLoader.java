package toyota.example.toyota_project.Simulation.TCP;

import toyota.example.toyota_project.Helpers.*;
import toyota.example.toyota_project.Helpers.Logging.LoggingHelper;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ConfigLoader {
    private final String configFilePath;

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
}