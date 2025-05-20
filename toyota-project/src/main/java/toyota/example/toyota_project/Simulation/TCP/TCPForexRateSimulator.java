package toyota.example.toyota_project.Simulation.TCP;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.AllArgsConstructor;
import lombok.Data;
import toyota.example.toyota_project.Simulation.REST.ForexRateSimulator;
import toyota.example.toyota_project.Simulation.REST.ForexRateSimulator.MarketProperties;
import toyota.example.toyota_project.Simulation.REST.ForexRateSimulator.RateHistory;

public class TCPForexRateSimulator {

	 private static final Logger logger = LogManager.getLogger(TCPForexRateSimulator.class);
	    private final Map<String, RateHistory> rateHistories = new ConcurrentHashMap<>();
	    private final Map<String, MarketProperties> marketProperties = new ConcurrentHashMap<>();
	    private LocalTime marketStart;
	    private LocalTime marketEnd;
	    private final Properties config;

	    public TCPForexRateSimulator(Properties config) {
	        this.config = config;
	        initialize();
	    }
	    public static class RateHistory {
	        private double previousRate;
	        private double currentRate;
	        private LocalDateTime lastUpdate;
	        private List<Double> recentRates;
	        private double volatilityIndex;
	        
	        public RateHistory(double previousRate,double currentRate,LocalDateTime lastUpdate,List<Double> recentRates,double volatilityIndex) {
	        	this.previousRate=previousRate;
	        	this.currentRate=currentRate;
	        	this.lastUpdate=lastUpdate;
	        	
	        	this.recentRates=recentRates;
	        	this.volatilityIndex=volatilityIndex;
	        	
	        }
	        public double getPreviousRate() {
	            return previousRate;
	        }

	        public void setPreviousRate(double previousRate) {
	            this.previousRate = previousRate;
	        }

	        public double getCurrentRate() {
	            return currentRate;
	        }

	        public void setCurrentRate(double currentRate) {
	            this.currentRate = currentRate;
	        }

	        public LocalDateTime getLastUpdate() {
	            return lastUpdate;
	        }

	        public void setLastUpdate(LocalDateTime lastUpdate) {
	            this.lastUpdate = lastUpdate;
	        }

	        public List<Double> getRecentRates() {
	            return recentRates;
	        }

	        public void setRecentRates(List<Double> recentRates) {
	            this.recentRates = recentRates;
	        }

	        public double getVolatilityIndex() {
	            return volatilityIndex;
	        }

	        public void setVolatilityIndex(double volatilityIndex) {
	            this.volatilityIndex = volatilityIndex;
	        }
	    }

	   
	    public static class MarketProperties {
	        private double baseVolatility;
	        private double trendStrength;
	        private double momentumFactor;
	        private double spread;
	        private ZoneId timeZone;
	        
	        public MarketProperties(double baseVolatility,double trendStrength,double momentumFactor,double spread,ZoneId timeZone) {
	        	this.baseVolatility=baseVolatility;
	        	this.trendStrength=trendStrength;
	        	this.momentumFactor=momentumFactor;
	        	this.spread=spread;
	        	this.timeZone=timeZone;
	        	
	        }
	        public double getBaseVolatility() {
	            return baseVolatility;
	        }

	        public double getTrendStrength() {
	            return trendStrength;
	        }

	        public double getMomentumFactor() {
	            return momentumFactor;
	        }

	        public double getSpread() {
	            return spread;
	        }

	        public ZoneId getTimeZone() {
	            return timeZone;
	        }

	        
	        public void setBaseVolatility(double baseVolatility) {
	            this.baseVolatility = baseVolatility;
	        }

	        public void setTrendStrength(double trendStrength) {
	            this.trendStrength = trendStrength;
	        }

	        public void setMomentumFactor(double momentumFactor) {
	            this.momentumFactor = momentumFactor;
	        }

	        public void setSpread(double spread) {
	            this.spread = spread;
	        }

	        public void setTimeZone(ZoneId timeZone) {
	            this.timeZone = timeZone;
	        }
	    }

	    private void initialize() {
	        String marketStartStr = config.getProperty("market.hours.start", "09:00");
	        String marketEndStr = config.getProperty("market.hours.end", "17:30");
	        this.marketStart = LocalTime.parse(marketStartStr);
	        this.marketEnd = LocalTime.parse(marketEndStr);
	        initializeMarketProperties();
	        logger.info("ForexRateSimulator initialized with market hours: {} - {}", marketStart, marketEnd);
	    }

	    private void initializeMarketProperties() {
	        // Tüm sembolleri config'den al (PF1_USDTRY, PF1_EURUSD vb.)
	        Set<String> symbols = new HashSet<>();
	        for (String key : config.stringPropertyNames()) {
	            if (key.contains(".")) {
	                String symbol = key.split("\\.")[0];
	                symbols.add(symbol);
	            }
	        }

	        for (String symbol : symbols) {
	            try {
	                double baseVolatility = Double.parseDouble(config.getProperty(symbol + ".baseVolatility", "0.002"));
	                double trendStrength = Double.parseDouble(config.getProperty(symbol + ".trendStrength", "0.25"));
	                double momentumFactor = Double.parseDouble(config.getProperty(symbol + ".momentumFactor", "0.15"));
	                double spread = Double.parseDouble(config.getProperty(symbol + ".spread", "0.01"));
	                ZoneId timeZone = ZoneId.of(config.getProperty(symbol + ".timeZone", "UTC"));

	                marketProperties.put(symbol, new MarketProperties(
	                    baseVolatility,
	                    trendStrength,
	                    momentumFactor,
	                    spread,
	                    timeZone
	                ));
	                logger.debug("Loaded market properties for {}: {}", symbol, marketProperties.get(symbol));
	            } catch (Exception e) {
	                logger.error("Error loading properties for symbol {}: {}", symbol, e.getMessage());
	            }
	        }
	    }
	    
	    public void initializeRate(String symbol, double initialRate) {
	        if (initialRate <= 0) {
	            logger.warn("Invalid initial rate for {}: {}. Setting to default value 1.0", symbol, initialRate);
	            initialRate = 1.0;
	        }
	        
	        List<Double> initialRates = new ArrayList<>();
	        initialRates.add(initialRate);
	        
	        rateHistories.put(symbol, new RateHistory(
	            initialRate,
	            initialRate,
	            LocalDateTime.now(),
	            initialRates,
	            0.0
	        ));
	        
	        logger.info("Rate initialized for {} with value {}", symbol, initialRate);
	    }

	   /* public void setMarketHours(String start, String end) {
	        this.marketHoursStart = start;
	        this.marketHoursEnd = end;
	        logger.info("Market hours set to {} - {}", start, end);
	    }*/

	    public double calculateNextRate(String symbol, double currentRate) {
	        // Safety check - avoid invalid input
	        if (currentRate <= 0) {
	            logger.error("Invalid current rate for {}: {}. Using default rate 1.0", symbol, currentRate);
	            return 1.0;
	        }
	        
	        RateHistory history = rateHistories.get(symbol);
	        MarketProperties props = marketProperties.get(symbol);

	        if (history == null) {
	            logger.warn("History not found for symbol: {}. Initializing with current rate.", symbol);
	            initializeRate(symbol, currentRate);
	            history = rateHistories.get(symbol);
	        }
	        
	        if (props == null) {
	            logger.warn("Properties not found for symbol: {}. Using default values.", symbol);
	            props = new MarketProperties(
	                0.002,  // baseVolatility
	                0.25,   // trendStrength
	                0.15,   // momentumFactor
	                0.01,   // spread
	                ZoneId.systemDefault()
	            );
	            marketProperties.put(symbol, props);
	        }

	        try {
	            // Market conditions factor - bounded between 0.5 and 1.5
	            double marketFactor = calculateMarketConditionsFactor(props);
	            
	            // Trend factor - capped to prevent extreme values
	            double trendFactor = calculateTrendFactor(history, props);
	            trendFactor = capValue(trendFactor, -0.01, 0.01);
	            
	            // Volatility - random but controlled
	            double volatility = calculateVolatilityFactor(history, props);
	            volatility = capValue(volatility, -0.005, 0.005);
	            
	            // Momentum - based on recent movement direction
	            double momentum = calculateMomentumFactor(history, props);
	            momentum = capValue(momentum, -0.008, 0.008);
	            
	            // Combine all factors with safety limits
	            double totalChange = (trendFactor + volatility + momentum) * marketFactor;
	            totalChange = capValue(totalChange, -0.01, 0.01); // Cap total change to prevent extreme movements
	            
	            // Calculate new rate with validation
	            double newRate = currentRate * (1 + totalChange);
	            
	            // Final safety check
	            if (Double.isNaN(newRate) || Double.isInfinite(newRate) || newRate <= 0) {
	                logger.error("Invalid calculated rate for {}: {}. Fallback to current rate.", symbol, newRate);
	                newRate = currentRate > 0 ? currentRate : 1.0;
	            }
	            
	            // Update history
	            updateRateHistory(history, newRate);
	            
	            logger.debug("Rate calculated for {}: {} -> {}", symbol, currentRate, newRate);
	            return newRate;
	            
	        } catch (Exception e) {
	            logger.error("Error calculating rate for {}: {}", symbol, e.getMessage(), e);
	            // In case of any exception, return a safe value
	            return currentRate > 0 ? currentRate : 1.0;
	        }
	    }

	    // Helper to cap values and prevent extreme changes
	    private double capValue(double value, double min, double max) {
	        return Math.max(min, Math.min(max, value));
	    }

	    private void updateRateHistory(RateHistory history, double newRate) {
	        history.setPreviousRate(history.getCurrentRate());
	        history.setCurrentRate(newRate);
	        history.setLastUpdate(LocalDateTime.now());
	        
	        List<Double> recentRates = history.getRecentRates();
	        recentRates.add(newRate);
	        // Keep list size manageable
	        if (recentRates.size() > 30) {
	            recentRates.remove(0);
	        }
	        
	        // Update volatility index safely
	        try {
	            history.setVolatilityIndex(calculateVolatilityIndex(recentRates));
	        } catch (Exception e) {
	            logger.warn("Error calculating volatility index: {}", e.getMessage());
	            history.setVolatilityIndex(0.001); // Safe default
	        }
	    }

	    private double calculateMarketConditionsFactor(MarketProperties props) {
	        try {
	            LocalTime now = LocalTime.now(props.getTimeZone());
	            
	            if (now.isBefore(marketStart) || now.isAfter(marketEnd)) {
	                return 0.5; // Lower volatility outside market hours
	            }

	            // Higher volatility near market open/close
	            if (now.isBefore(marketStart.plusHours(1)) || 
	                now.isAfter(marketEnd.minusHours(1))) {
	                return 1.5;
	            }

	            // Lower activity during lunch hours
	            if (now.isAfter(LocalTime.of(12, 0)) && 
	                now.isBefore(LocalTime.of(13, 30))) {
	                return 0.7;
	            }

	            return 1.0;
	        } catch (Exception e) {
	            logger.warn("Error in market conditions calculation: {}", e.getMessage());
	            return 1.0; // Safe default
	        }
	    }

	    private double calculateTrendFactor(RateHistory history, MarketProperties props) {
	        try {
	            List<Double> rates = history.getRecentRates();
	            if (rates.size() < 2) return 0.0;
	            
	            double shortTermTrend = calculateSimpleTrend(rates.subList(Math.max(0, rates.size() - 5), rates.size()));
	            double longTermTrend = calculateSimpleTrend(rates);
	            
	            // Mean Reversion: Ortalama fiyata göre düzeltme
	            double mean = rates.stream().mapToDouble(Double::doubleValue).average().orElse(history.getCurrentRate());
	            double meanReversion = (mean - history.getCurrentRate()) / mean * 0.2; // %20 mean reversion
	            
	            return ((shortTermTrend * 0.4 + longTermTrend * 0.3 + meanReversion * 0.3) * props.getTrendStrength());
	        } catch (Exception e) {
	            logger.warn("Error calculating trend factor: {}", e.getMessage());
	            return 0.0;
	        }
	    }

	    private double calculateVolatilityFactor(RateHistory history, MarketProperties props) {
	        try {
	            double marketFactor = calculateMarketConditionsFactor(props);
	            double baseVolatility = props.getBaseVolatility() * marketFactor;
	            
	            // Rastgele yön ve şiddet
	            double randomDirection = (Math.random() > 0.5 ? 1 : -1);
	            double volatility = baseVolatility * (1 + Math.random()) * randomDirection;
	            
	            return volatility;
	        } catch (Exception e) {
	            logger.warn("Error calculating volatility factor: {}", e.getMessage());
	            return 0.0;
	        }
	    }
	   

	    private double calculateSimpleTrend(List<Double> rates) {
	        if (rates.size() < 2 || rates.get(0) <= 0) return 0.0;
	        
	        double first = rates.get(0);
	        double last = rates.get(rates.size() - 1);
	        
	        // Avoid division by zero or negative values
	        if (first <= 0) first = 0.01;
	        if (last <= 0) last = 0.01;
	        
	        return (last - first) / first;
	    }

	    

	    private double calculateMomentumFactor(RateHistory history, MarketProperties props) {
	        try {
	            if (history.getRecentRates().size() < 3) return 0.0;
	            
	            // Son 3 değişikliğin ortalaması
	            double momentum = history.getRecentRates().stream()
	                .skip(history.getRecentRates().size() - 3)
	                .mapToDouble(d -> d)
	                .average()
	                .orElse(0.0);
	            
	            // Momentumun yavaşça azalması
	            return (momentum - history.getCurrentRate()) * props.getMomentumFactor() * 0.7;
	        } catch (Exception e) {
	            logger.warn("Error calculating momentum factor: {}", e.getMessage());
	            return 0.0;
	        }
	    }

	    private double calculateVolatilityIndex(List<Double> rates) {
	        if (rates == null || rates.size() < 2) return 0.001;
	        
	        try {
	            double sum = 0;
	            double count = 0;
	            
	            // Calculate mean with validation
	            for (Double rate : rates) {
	                if (rate != null && rate > 0) {
	                    sum += rate;
	                    count++;
	                }
	            }
	            
	            if (count < 2) return 0.001;
	            
	            double mean = sum / count;
	            
	            // Calculate variance with validation
	            double sumSquaredDiff = 0;
	            for (Double rate : rates) {
	                if (rate != null && rate > 0) {
	                    sumSquaredDiff += Math.pow(rate - mean, 2);
	                }
	            }
	            
	            // Standard deviation
	            double stdDev = Math.sqrt(sumSquaredDiff / (count - 1));
	            
	            // Return coefficient of variation
	            return stdDev / mean;
	        } catch (Exception e) {
	            logger.warn("Error in volatility calculation: {}", e.getMessage());
	            return 0.001; // Safe default
	        }
	    }

	    public boolean isWithinMarketHours() {
	        try {
	            LocalTime now = LocalTime.now();
	            return !now.isBefore(marketStart) && !now.isAfter(marketEnd);
	        } catch (Exception e) {
	            logger.warn("Error checking market hours: {}", e.getMessage());
	            return true; // Default to open
	        }
	    }

	    public MarketProperties getMarketProperties(String symbol) {
	        return marketProperties.get(symbol);
	    }
	    
	    // For debugging purposes
	    public RateHistory getRateHistory(String symbol) {
	        return rateHistories.get(symbol);
	    }
}
