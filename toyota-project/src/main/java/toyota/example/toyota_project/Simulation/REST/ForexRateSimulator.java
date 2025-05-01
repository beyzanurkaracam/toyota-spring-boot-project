package toyota.example.toyota_project.Simulation.REST;


import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import toyota.example.toyota_project.Simulation.REST.Controllers.RateController;


@Component
public class ForexRateSimulator {

 private static final Logger logger = LogManager.getLogger(ForexRateSimulator.class);
 
 private final Map<String, RateHistory> rateHistories = new ConcurrentHashMap<>();
 private final Map<String, MarketProperties> marketProperties = new ConcurrentHashMap<>();

 @Value("${market.hours.start:09:00}")
 private String marketHoursStart;

 @Value("${market.hours.end:17:30}")
 private String marketHoursEnd;

 private LocalTime marketStart;
 private LocalTime marketEnd;

 @Data
 @AllArgsConstructor
 public static class RateHistory {
     private double previousRate;
     private double currentRate;
     private LocalDateTime lastUpdate;
     private List<Double> recentRates;
     private double volatilityIndex;
 }

 @Data
 @AllArgsConstructor
 public static class MarketProperties {
     private double baseVolatility;
     private double trendStrength;
     private double momentumFactor;
     private double spread;
     private ZoneId timeZone;
 }

 @PostConstruct
 public void initialize() {
     marketStart = LocalTime.parse(marketHoursStart);
     marketEnd = LocalTime.parse(marketHoursEnd);
     initializeMarketProperties();
 }

 private void initializeMarketProperties() {
     marketProperties.put("PF2_USDTRY", new MarketProperties(
         0.002,  // Base volatility
         0.3,    // Trend strength
         0.2,    // Momentum factor
         0.01,   // Default spread
         ZoneId.of("Europe/Istanbul")
     ));
     
     marketProperties.put("PF2_EURUSD", new MarketProperties(
         0.0015, // Lower base volatility for major pair
         0.25,   // Trend strength
         0.15,   // Momentum factor
         0.0008, // Tighter spread for major pair
         ZoneId.of("Europe/London")
     ));
 }

 public void initializeRate(String symbol, double initialRate) {
     rateHistories.put(symbol, new RateHistory(
         initialRate,
         initialRate,
         LocalDateTime.now(),
         new ArrayList<>(),
         0.0
     ));
 }

 public double calculateNextRate(String symbol, double currentRate) {
     RateHistory history = rateHistories.get(symbol);
     MarketProperties props = marketProperties.get(symbol);

     if (history == null || props == null) {
         logger.warn("History or properties not found for symbol: {}", symbol);
         return currentRate;
     }

     // Market conditions factor
     double marketFactor = calculateMarketConditionsFactor(props);
     
     // Trend factor
     double trendFactor = calculateTrendFactor(history, props);
     
     // Volatility
     double volatility = calculateVolatilityFactor(history, props);
     
     // Momentum
     double momentum = calculateMomentumFactor(history, props);
     
     // Combine all factors
     double totalChange = (trendFactor + volatility + momentum) * marketFactor;
     
     // Calculate new rate
     double newRate = currentRate * (1 + totalChange);
     
     // Update history
     updateRateHistory(history, newRate);
     
     return newRate;
 }

 private void updateRateHistory(RateHistory history, double newRate) {
     history.setPreviousRate(history.getCurrentRate());
     history.setCurrentRate(newRate);
     history.setLastUpdate(LocalDateTime.now());
     
     List<Double> recentRates = history.getRecentRates();
     recentRates.add(newRate);
     if (recentRates.size() > 50) {
         recentRates.remove(0);
     }
     
     history.setVolatilityIndex(calculateVolatilityIndex(recentRates));
 }

 private double calculateMarketConditionsFactor(MarketProperties props) {
     LocalTime now = LocalTime.now(props.getTimeZone());
     
     if (now.isBefore(marketStart) || now.isAfter(marketEnd)) {
         return 0.5; 
     }

   
     if (now.isBefore(marketStart.plusHours(1)) || 
         now.isAfter(marketEnd.minusHours(1))) {
         return 1.5;
     }

     // Öğle saatlerinde düşük aktivite
     if (now.isAfter(LocalTime.of(12, 0)) && 
         now.isBefore(LocalTime.of(13, 30))) {
         return 0.7;
     }

     return 1.0;
 }

 private double calculateTrendFactor(RateHistory history, MarketProperties props) {
     List<Double> rates = history.getRecentRates();
     if (rates.size() < 2) return 0.0;

     double shortTermTrend = calculateTrend(
         rates.subList(Math.max(0, rates.size() - 10), rates.size())
     );
     double longTermTrend = calculateTrend(rates);

     return (shortTermTrend * 0.7 + longTermTrend * 0.3) * props.getTrendStrength();
 }

 private double calculateTrend(List<Double> rates) {
     if (rates.size() < 2) return 0.0;
     return (rates.get(rates.size() - 1) - rates.get(0)) / rates.get(0);
 }

 private double calculateVolatilityFactor(RateHistory history, MarketProperties props) {
     double baseVolatility = props.getBaseVolatility();
     double currentVolatility = history.getVolatilityIndex();
     double adjustedVolatility = baseVolatility * (1 + currentVolatility);
     return (Math.random() - 0.5) * 2 * adjustedVolatility;
 }

 private double calculateMomentumFactor(RateHistory history, MarketProperties props) {
     if (history.getRecentRates().size() < 2) return 0.0;
     
     double recentChange = (history.getCurrentRate() - history.getPreviousRate()) / 
                          history.getPreviousRate();
     
     return recentChange * props.getMomentumFactor();
 }

 private double calculateVolatilityIndex(List<Double> rates) {
     if (rates.size() < 2) return 0.0;
     
     double mean = rates.stream()
         .mapToDouble(Double::doubleValue)
         .average()
         .orElse(0.0);
     
     double sumSquaredDiff = rates.stream()
         .mapToDouble(rate -> Math.pow(rate - mean, 2))
         .sum();
     
     return Math.sqrt(sumSquaredDiff / (rates.size() - 1));
 }

 public boolean isWithinMarketHours() {
     LocalTime now = LocalTime.now();
     return !now.isBefore(marketStart) && !now.isAfter(marketEnd);
 }

 public MarketProperties getMarketProperties(String symbol) {
     return marketProperties.get(symbol);
 }
}