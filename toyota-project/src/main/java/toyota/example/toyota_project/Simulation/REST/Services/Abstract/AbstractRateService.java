package toyota.example.toyota_project.Simulation.REST.Services.Abstract;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Profile;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
@Profile("rest")
public abstract class AbstractRateService implements IRateService {
    protected static final Logger logger = LogManager.getLogger(AbstractRateService.class);
    
    protected Map<String, Double> rates = new ConcurrentHashMap<>();
    protected Map<String, Double> spreads = new ConcurrentHashMap<>();
    protected Map<String, Double> previousRates = new ConcurrentHashMap<>();
    
    protected double rateFluctuation;

    @Override
    public boolean isValidSymbol(String symbol) {
        return rates.containsKey(symbol);
    }

    protected abstract double calculateRealisticFluctuation(double currentRate, String symbol);
    
    protected void logRateUpdate(String symbol, double oldRate, double newRate) {
        logger.info("Rate updated for {}: {} -> {}", symbol, oldRate, newRate);
    }
}

