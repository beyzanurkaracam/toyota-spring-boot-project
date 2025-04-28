package toyota.example.toyota_project.Simulation.REST.Services.Concrete;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import toyota.example.toyota_project.Helpers.Logging.LoggingHelper;
import toyota.example.toyota_project.Helpers.Logging.Exceptions.Rest.RateProcessingException;
import toyota.example.toyota_project.Simulation.REST.ForexRateSimulator;
import toyota.example.toyota_project.Simulation.REST.Responses.RateResponse;
import toyota.example.toyota_project.Simulation.REST.Services.Abstract.AbstractRateService;
@Profile("rest")
@Service
public class RateService extends AbstractRateService {

    @Autowired
    private ForexRateSimulator simulator;

    @Value("${initial.rates}")
    private String[] initialRates;

    @Value("${rate.refresh.interval:1000}")
    private long refreshInterval;

    @Value("${max.broadcast.count:1000}")
    private int maxBroadcastCount;

    private int currentBroadcastCount = 0;

    @PostConstruct
    @Override
    public void initialize() {
        LoggingHelper.logInfo("RateService başlatılıyor...");
        validateAndInitializeRates();
        startRateUpdateScheduler();
    }

    private void validateAndInitializeRates() {
        LoggingHelper.logDebug("Başlangıç oranları yükleniyor...");
        
        for (String rateConfig : initialRates) {
            String[] parts = rateConfig.split(":");
            if (parts.length != 3) {
                LoggingHelper.logError("Geçersiz oran konfigürasyonu: {}", null, rateConfig);
                continue;
            }

            try {
                String symbol = validateSymbol(parts[0]);
                double initialRate = Double.parseDouble(parts[1]);
                double spread = Double.parseDouble(parts[2]);

                validateRateValues(initialRate, spread);

                rates.put(symbol, initialRate);
                spreads.put(symbol, spread);
                simulator.initializeRate(symbol, initialRate);

                LoggingHelper.logInfo("Oran başlatıldı: {}, Başlangıç Değeri: {}, Spread: {}", 
                    symbol, initialRate, spread);

            } catch (IllegalArgumentException e) {
                LoggingHelper.logError("Oran başlatma hatası: {}", e, e.getMessage());
            }
        }
    }

    private void startRateUpdateScheduler() {
        LoggingHelper.logInfo("Oran güncelleme zamanlayıcısı başlatılıyor...");

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            if (currentBroadcastCount < maxBroadcastCount) {
                updateAllRates();
                currentBroadcastCount++;
            } else {
                scheduler.shutdown();
                LoggingHelper.logInfo("Maksimum yayın sayısına ulaşıldı. Oran güncellemeleri durduruluyor.");
            }
        }, 0, refreshInterval, TimeUnit.MILLISECONDS);
    }

    @Override
    public RateResponse getRateForSymbol(String symbol) {
        try {
            LoggingHelper.logDebug("Oran hesaplama başladı: {}", symbol);

            if (!isValidSymbol(symbol)) {
                LoggingHelper.logWarn("Geçersiz sembol: {}", symbol);
                return null;
            }

            Double currentRate = rates.get(symbol);
            if (currentRate == null) {
                LoggingHelper.logWarn("Sembol için oran bulunamadı: {}", symbol);
                return null;
            }

            if (!simulator.isWithinMarketHours()) {
                LoggingHelper.logInfo("Piyasa saatleri dışında istek: {}", symbol);
                return createResponse(symbol, currentRate);
            }

            double newRate = simulator.calculateNextRate(symbol, currentRate);
            ForexRateSimulator.MarketProperties props = simulator.getMarketProperties(symbol);
            double spread = spreads.getOrDefault(symbol, props.getSpread());
            double newAsk = newRate + (newRate * spread);

            rates.put(symbol, newRate);

            LoggingHelper.logInfo("Oran güncellendi: {} -> {}", currentRate, newRate);
            return new RateResponse(symbol, newRate, newAsk, LocalDateTime.now());

        } catch (Exception e) {
            LoggingHelper.logError("Oran hesaplama hatası: {}", e, symbol);
            throw new RateProcessingException("Oran hesaplama hatası", e);
        }
    }

    private String validateSymbol(String symbol) {
        if (!symbol.startsWith("PF2_")) {
            LoggingHelper.logError("Geçersiz sembol öneki: {}", null, symbol);
            throw new IllegalArgumentException("Geçersiz sembol öneki: " + symbol);
        }
        return symbol;
    }

    private void validateRateValues(double rate, double spread) {
        if (rate <= 0 || spread < 0 || spread > 1) {
            LoggingHelper.logError("Geçersiz oran veya spread değerleri: Rate={}, Spread={}", null, rate, spread);
            throw new IllegalArgumentException("Geçersiz oran veya spread değerleri");
        }
    }

    private RateResponse createResponse(String symbol, double currentRate) {
        double spread = spreads.getOrDefault(symbol, 0.01);
        double ask = currentRate + (currentRate * spread);
        return new RateResponse(symbol, currentRate, ask, LocalDateTime.now());
    }

    private void updateAllRates() {
        LoggingHelper.logDebug("Tüm oranlar güncelleniyor...");
        rates.keySet().forEach(symbol -> getRateForSymbol(symbol));
    }

    @Override
    protected double calculateRealisticFluctuation(double currentRate, String symbol) {
        // TODO: Simülatör entegrasyonu sonrası bu metod güncellenebilir
        return 0;
    }
}