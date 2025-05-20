package toyota.example.toyota_project.MainApp.Concrete;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
	
	 @Bean(name = "taskExecutor", destroyMethod = "shutdown")
	    public ExecutorService taskExecutor() {
	        return Executors.newCachedThreadPool();
	    }

	  @Bean(name = "scheduledExecutorService", destroyMethod = "shutdown")
	    public ScheduledExecutorService scheduledExecutorService() {
	        
	        return Executors.newScheduledThreadPool(10);
	    }

}
