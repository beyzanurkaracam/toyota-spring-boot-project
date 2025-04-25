package toyota.example.toyota_project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@Configuration
@PropertySource("classpath:rest-config.properties")
public class ToyotaProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(ToyotaProjectApplication.class, args);
	}

}
