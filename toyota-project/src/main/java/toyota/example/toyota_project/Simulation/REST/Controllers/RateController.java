package toyota.example.toyota_project.Simulation.REST.Controllers;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import toyota.example.toyota_project.Helpers.Logging.Exceptions.Rest.RateProcessingException;
import toyota.example.toyota_project.Simulation.REST.RateLimiter;
import toyota.example.toyota_project.Simulation.REST.Responses.RateResponse;
import toyota.example.toyota_project.Simulation.REST.Services.Concrete.RateService;
import toyota.example.toyota_project.Simulation.TCP.TCPServerSimulation;

@RestController
@RequestMapping("/api/rates")
public class RateController {
    private static final Logger logger = LogManager.getLogger(RateController.class);
    
    @Autowired
    private RateService rateService;
    
    @GetMapping("/{symbol}")
    public ResponseEntity<?> getRate(@PathVariable String symbol) {
        try {
            logger.debug("Rate isteği alındı: {}", symbol);

            // Rate limiter kontrolü
            if (!RateLimiter.checkLimit(symbol)) {
                logger.warn("Rate limit aşıldı: {}", symbol);
                return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Too many requests for symbol: " + symbol);
            }
            
            RateResponse rate = rateService.getRateForSymbol(symbol);
            if (rate == null) {
                logger.warn("Rate bulunamadı: {}", symbol);
                return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Rate not found for symbol: " + symbol);
            }
            
            logger.info("Rate başarıyla döndürüldü: {}", symbol);
            return ResponseEntity.ok(rate);
            
        } catch (RateProcessingException e) {
            logger.error("Rate işleme hatası: {}", symbol, e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing rate: " + e.getMessage());
        } catch (Exception e) {
            logger.fatal("Beklenmeyen hata: {}", symbol, e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An unexpected error occurred");
        }
    }
}