package toyota.example.toyota_project.MainApp.Concrete;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import toyota.example.toyota_project.Entities.Rate;
import toyota.example.toyota_project.Entities.RateFields;
import toyota.example.toyota_project.Kafka.KafkaProducerService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import toyota.example.toyota_project.Config.CollectorConfig;
import toyota.example.toyota_project.Config.ConfigLoader;
import toyota.example.toyota_project.MainApp.Abstract.CoordinatorCallBack;
import toyota.example.toyota_project.MainApp.Abstract.DataCollector;
import toyota.example.toyota_project.MainApp.Calculation.Concrete.FormulaEngine;
import toyota.example.toyota_project.Redis.RedisCacheManager;
@Component
@Qualifier
public class Coordinator implements CoordinatorCallBack {
	private List<DataCollector>collectors=new ArrayList<>();
	private ExecutorService executor= Executors.newCachedThreadPool();
	private static final Logger logger = LogManager.getLogger(Coordinator.class);
	 @Autowired
	    private RedisCacheManager redisCacheManager;
	 @Autowired
	 private KafkaProducerService kafkaProducerService;
	 @Autowired
	    private FormulaEngine formulaEngine;
	 
	@Override
	public void onConnect(String platformName, Boolean status) {
		if(status) {
			logger.info("Connected to platform:{}",platformName);
			
			List<String> ratesToSubscribe=ConfigLoader.getSubscriptionsForPlatform(platformName);
			
			DataCollector collector=findCollectorByPlatform(platformName);
			
			if(collector!=null && !ratesToSubscribe.isEmpty()) {
				ratesToSubscribe.forEach(rate->{
					collector.subscribe(platformName, rate);
					logger.debug("Subscribed to {} on {}",rate,platformName);
				});
			}else {
				logger.error("Connection failed for platform: {}",platformName);
			}
		}
	}
	
	private DataCollector findCollectorByPlatform(String platformName) {
		return collectors.stream()
				.filter(c->c.getPlatformName().equals(platformName))
				.findFirst()
				.orElse(null);
	}

	@Override
	public void onDisConnect(String platformName, Boolean status) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void initialize() {
	    logger.info("Initializing collectors...");
	    List<CollectorConfig> configs = ConfigLoader.loadCollectorConfigs("src/main/resources/collectors.properties");
	    
	    for (CollectorConfig config : configs) {
	        logger.info("Creating collector for {}", config.getName());
	        DataCollector collector = createCollector(config);
	        if (collector != null) {
	            collectors.add(collector);
	            executor.submit(() -> {
	                logger.info("Connecting to {}", config.getName());
	                collector.connect(config.getName(), config.getUserName(), config.getPassword());
	            });
	        }
	    }
	}

	private DataCollector createCollector(CollectorConfig config) {
		try {
			Class <?> clazz=Class.forName(config.getClassName());
			if(DataCollector.class.isAssignableFrom(clazz)) {
				DataCollector collector=(DataCollector)clazz.getDeclaredConstructor().newInstance();
				collector.loadConfig(config.getConfigFile());
				collector.setCallBack(this);
				return collector;
			}
		}
			catch(Exception e) {
				
				throw new RuntimeException("Failed to load collector: " + config.getName(), e);
			}
	return null;
			
	}

	@Override
	public void onRateAvailable(String platformName, String rateName, Rate rate) {
	    try {
	    	 RateFields rateFields = new RateFields(
	    	            rateName,
	    	            rate.getBid(),
	    	            rate.getAsk(),
	    	            rate.getTimestamp() != null && !rate.getTimestamp().isEmpty() 
	    	                ? rate.getTimestamp() 
	    	                : Instant.now().toString()
	    	        );
	    	 logger.info("ONRATEaVAİLABLE", rate.getBid(),
	    	            rate.getAsk(),
	    	            rate.getTimestamp());
	      redisCacheManager.put(rateName, rateFields);
	      kafkaProducerService.sendRateMessage(platformName, rateName, rate);
	        
	    } catch (Exception e) {
	        logger.error("Error processing rate {}: {}", rateName, e.getMessage());
	    }
	}
	private void calculateDerivedRate(String baseSymbol) {
	    try {
	       
	    	 Map<String, RateFields> dependencies = new HashMap<>();
	         boolean missingData = false;

	         // USDTRY için bağımlılıklar
	         if (baseSymbol.equals("USDTRY")) {
	             RateFields pf1Usdtry = redisCacheManager.get("PF1_USDTRY");
	             RateFields pf2Usdtry = redisCacheManager.get("PF2_USDTRY");
	             if (pf1Usdtry == null || pf2Usdtry == null) {
	                 missingData = true;
	             } else {
	                 dependencies.put("PF1_USDTRY", pf1Usdtry);
	                 dependencies.put("PF2_USDTRY", pf2Usdtry);
	             }

	         // EURTRY için bağımlılıklar
	         } else if (baseSymbol.equals("EURTRY")) {
	             RateFields pf1Usdtry = redisCacheManager.get("PF1_USDTRY");
	             RateFields pf2Usdtry = redisCacheManager.get("PF2_USDTRY");
	             RateFields pf1EurUsd = redisCacheManager.get("PF1_EURUSD");
	             RateFields pf2EurUsd = redisCacheManager.get("PF2_EURUSD");

	             if (pf1Usdtry == null || pf2Usdtry == null || pf1EurUsd == null || pf2EurUsd == null) {
	                 missingData = true;
	             } else {
	                 dependencies.put("PF1_USDTRY", pf1Usdtry);
	                 dependencies.put("PF2_USDTRY", pf2Usdtry);
	                 dependencies.put("PF1_EURUSD", pf1EurUsd);
	                 dependencies.put("PF2_EURUSD", pf2EurUsd);
	             }

	         // GBPTRY için bağımlılıklar
	         } else if (baseSymbol.equals("GBPTRY")) {
	             RateFields pf1Usdtry = redisCacheManager.get("PF1_USDTRY");
	            RateFields pf2Usdtry = redisCacheManager.get("PF2_USDTRY");
	             RateFields pf1GbpUsd = redisCacheManager.get("PF1_GBPUSD");
	             RateFields pf2GbpUsd = redisCacheManager.get("PF2_GBPUSD");

	             if (pf1Usdtry == null || pf2Usdtry == null || pf1GbpUsd == null || pf2GbpUsd == null) {
	                 missingData = true;
	             } else {
	                 dependencies.put("PF1_USDTRY", pf1Usdtry);
	                 dependencies.put("PF2_USDTRY", pf2Usdtry);
	                 dependencies.put("PF1_GBPUSD", pf1GbpUsd);
	                 dependencies.put("PF2_GBPUSD", pf2GbpUsd);
	             }
	         }

	         // Eksik veri varsa hesaplama yapma
	         if (missingData) {
	             logger.warn("Hesaplama için gereken veriler eksik: {}", baseSymbol);
	             return;
	         }

	       
	       Map<String,Double>results=formulaEngine.calculate(baseSymbol, dependencies);
	        double calculatedBid = results.get("bid");
	        double calculatedAsk = results.get("ask");
	        
	        Rate calculatedRate = new Rate(
	                calculatedBid,
	                calculatedAsk,
	                Instant.now().toString()
	        );

	       
	        kafkaProducerService.sendRateMessage("CALCULATED", baseSymbol, calculatedRate);
	        logger.info("Hesaplanan veri Kafka'ya gönderildi: {}", baseSymbol);

	        
	        redisCacheManager.put(
	                baseSymbol,
	                new RateFields(
	                        baseSymbol,
	                        calculatedBid,
	                        calculatedAsk,
	                        Instant.now().toString()
	                )
	        );
	        logger.info("Hesaplanan veri Redis'e kaydedildi: {}", baseSymbol);

	    } catch (Exception e) {
	        logger.error("Hesaplama hatası: {}", e.getMessage(), e);
	    }
	}
	@Override
	public void onRateUpdate(String platformName, String rateName, RateFields rateFields) {
		logger.info("Veri alındı: {}", rateName);
	    try {
	        logger.info("Veri güncellendi - Platform: {}, Sembol: {}, Bid: {}, Ask: {}, Zaman: {}",
	                platformName, rateName, rateFields.getBid(), rateFields.getAsk(), rateFields.getTimestamp());

	        // Redis'e veriyi kaydet
	        redisCacheManager.put(rateName, rateFields);
	        logger.debug("Redis'te güncellendi: {}", rateName);

	        // Kafka'ya güncel veriyi gönder
	        Rate updatedRate = new Rate(
	                rateFields.getBid(),
	                rateFields.getAsk(),
	                rateFields.getTimestamp()
	        );
	        kafkaProducerService.sendRateMessage(platformName, rateName, updatedRate);
	        logger.info("Kafka'ya güncel veri gönderildi: {}", rateName);

	        // PF ile başlayan semboller için türev hesapla
	        if (rateName.startsWith("PF")) {
	          /*  String currencyPair = rateName.split("_")[1]; // Örn: PF1_GBPUSD → GBPUSD
	            List<String> targetSymbols = ConfigLoader.getDependentSymbols(currencyPair); // GBPUSD → ["GBPTRY"]

	            if (targetSymbols.isEmpty()) {
	                logger.warn("{} için tanımlı bağımlı sembol bulunamadı.", currencyPair);
	                return;
	            }

	            // Tüm bağımlı sembolleri hesapla
	            targetSymbols.forEach(targetSymbol -> {
	                calculateDerivedRate(targetSymbol);
	                logger.info("{} için türev hesaplama tetiklendi", targetSymbol);
	            });*/
	        }

	    } catch (Exception e) {
	        logger.error("Güncelleme hatası: {}", e.getMessage(), e);
	    }
	}
	private void findAndCalculateDependentRates(String baseSymbol) {
		List<String> dependentSymbols=ConfigLoader.getDependentSymbols(baseSymbol);
		
		dependentSymbols.forEach(symbol->{
			 try {
		            calculateDerivedRate(symbol);
		            logger.info("{} için türev hesaplama tetiklendi", symbol);
		        } catch (Exception e) {
		            logger.error("{} hesaplama hatası: {}", symbol, e.getMessage());
		        }
		});
	}


	

	
	

}
