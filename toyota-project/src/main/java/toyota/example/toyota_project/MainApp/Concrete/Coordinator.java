package toyota.example.toyota_project.MainApp.Concrete;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import toyota.example.toyota_project.Config.CollectorConfig;
import toyota.example.toyota_project.Config.ConfigLoader;
import toyota.example.toyota_project.MainApp.Abstract.CoordinatorCallBack;
import toyota.example.toyota_project.MainApp.Abstract.DataCollector;
@Component
public class Coordinator implements CoordinatorCallBack {
	private List<DataCollector>collectors=new ArrayList<>();
	private ExecutorService executor= Executors.newCachedThreadPool();
	private static final Logger logger = LogManager.getLogger(Coordinator.class);
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
	

}
