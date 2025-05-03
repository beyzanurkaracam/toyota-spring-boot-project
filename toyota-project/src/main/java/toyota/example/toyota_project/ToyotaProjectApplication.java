package toyota.example.toyota_project;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import jakarta.annotation.PostConstruct;
import toyota.example.toyota_project.MainApp.Concrete.Coordinator;

@ComponentScan(basePackages = {"toyota.example.toyota_project"})
@SpringBootApplication
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
}