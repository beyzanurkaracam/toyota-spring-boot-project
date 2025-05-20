package toyota.example.toyota_project;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.PropertySource;

import jakarta.annotation.PostConstruct;
import toyota.example.toyota_project.MainApp.Concrete.Coordinator;

@ComponentScan(basePackages = {"toyota.example.toyota_project"})
@SpringBootApplication
@PropertySource("classpath:rest-config.properties")
@DependsOn("forexRateSimulator")
public class ToyotaProjectApplication {

    @Autowired
    private Coordinator coordinator;

    public static void main(String[] args) {
        SpringApplication.run(ToyotaProjectApplication.class, args);
    }

    @PostConstruct
    public void init() {
        coordinator.initialize();
    }
    @Bean
    public ApplicationRunner delayRestDataCollector() {
        return args -> {
            // Tomcat'in başlaması için 3 saniye bekle
            Thread.sleep(3000);
        };
    }
}