package toyota.example.toyota_project;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import jakarta.annotation.PostConstruct;
import toyota.example.toyota_project.MainApp.Concrete.Coordinator;

@SpringBootApplication
@Configuration
@PropertySource({"classpath:collectors.properties",
		"classpath:rest-config.properties"}
	)
public class ToyotaProjectApplication {
	 @Autowired
	    private Coordinator coordinator;

	    @PostConstruct
	    public void init() {
	        try {
	            coordinator.initialize();
	            
	         
	            
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }

	    public static void main(String[] args) {
	        SpringApplication.run(ToyotaProjectApplication.class, args);
	    }

}
