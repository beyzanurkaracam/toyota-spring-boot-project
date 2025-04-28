package toyota.example.toyota_project.Simulation.REST.Services.Abstract;

import org.springframework.context.annotation.Profile;

import toyota.example.toyota_project.Simulation.REST.Responses.RateResponse;
@Profile("rest")
public interface IRateService {
    void initialize();
    RateResponse getRateForSymbol(String symbol);
     boolean isValidSymbol(String symbol);
}