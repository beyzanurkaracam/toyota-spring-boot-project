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
	    
	    List<CollectorConfig> configs = ConfigLoader.loadCollectorConfigs("src/main/resources/collectors.properties");
	    
	    for (CollectorConfig config : configs) {
	        DataCollector collector = createCollector(config);
	        collectors.add(collector);
	        executor.submit(() -> collector.connect(config.getName(), config.getUserName(), config.getPassword()));
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
	public void onRateAvailable(String platformName, String rateName,Rate rate) {
		logger.info("New rate available - Platform: {}, Rate: {}, Bid: {}, Ask: {}, Timestamp: {}",
	            platformName, rateName, rate.getBid(), rate.getAsk(), rate.getTimestamp());
		RateFields rateFields=new RateFields(rateName,rate.getBid(),rate.getAsk(),rate.getTimestamp());
		try {
	        redisCacheManager.put(rateName, rateFields);
	        logger.debug("Cached in Redis: {}", rateName);
	    } catch (Exception e) {
	        logger.error("Redis cache failed for {}: {}", rateName, e.getMessage());
	    }
		//kafkaya göndericem
		 try {
	            kafkaProducerService.sendRateMessage(platformName, rateName, rate);
	            logger.info("Sent to Kafka: {}", rateName);
	        } catch (Exception e) {
	            logger.error("Failed to send to Kafka: {}", e.getMessage());
	        }
		//hesaplamaları yapıcam
		  if (rateName.startsWith("PF")) {
	            String baseSymbol = rateName.split("_")[1]; // PF1_USDTRY -> USDTRY
	            calculateDerivedRate(baseSymbol);
	        }
		
	}
	private void calculateDerivedRate(String baseSymbol) {
	    try {
	       
	        Map<String, RateFields> dependencies = new HashMap<>();
	        if (baseSymbol.equals("USDTRY")) {
	            dependencies.put("PF1_USDTRY", redisCacheManager.get("PF1_USDTRY"));
	            dependencies.put("PF2_USDTRY", redisCacheManager.get("PF2_USDTRY"));
	        } else if (baseSymbol.equals("EURTRY")) {
	            dependencies.put("PF1_USDTRY", redisCacheManager.get("PF1_USDTRY"));
	            dependencies.put("PF2_USDTRY", redisCacheManager.get("PF2_USDTRY"));
	            dependencies.put("PF1_EURUSD", redisCacheManager.get("PF1_EURUSD"));
	            dependencies.put("PF2_EURUSD", redisCacheManager.get("PF2_EURUSD"));
	        }else if (baseSymbol.equals("GBPTRY")) {
	            dependencies.put("PF1_USDTRY", redisCacheManager.get("PF1_USDTRY"));
	            dependencies.put("PF2_USDTRY", redisCacheManager.get("PF2_USDTRY"));
	            dependencies.put("PF1_GBPUSD", redisCacheManager.get("PF1_GBPUSD"));
	            dependencies.put("PF2_GBPUSD", redisCacheManager.get("PF2_GBPUSD"));
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
	public void onRateUpdate(String platformName, String rateName,RateFields rateFields) {
		 try {
		        
		        logger.info("Veri güncellendi - Platform: {}, Sembol: {}, Bid: {}, Ask: {}, Zaman: {}",
		                platformName, rateName, rateFields.getBid(), rateFields.getAsk(), rateFields.getTimestamp());

		        
		        redisCacheManager.put(rateName, rateFields);
		        logger.debug("Redis'te güncellendi: {}", rateName);

		       
		        Rate updatedRate = new Rate(
		                rateFields.getBid(),
		                rateFields.getAsk(),
		                rateFields.getTimestamp()
		        );
		        kafkaProducerService.sendRateMessage(platformName, rateName, updatedRate);
		        logger.info("Kafka'ya güncel veri gönderildi: {}", rateName);

		         if (rateName.startsWith("PF")) {
		            String baseSymbol = rateName.split("_")[1]; // Örn: PF1_USDTRY -> USDTRY
		            
		            
		            calculateDerivedRate(baseSymbol);
		            
		            findAndCalculateDependentRates(baseSymbol);
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
