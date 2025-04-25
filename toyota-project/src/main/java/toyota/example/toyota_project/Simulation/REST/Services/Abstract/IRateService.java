package toyota.example.toyota_project.Simulation.REST.Services.Abstract;

import toyota.example.toyota_project.Simulation.REST.Responses.RateResponse;

public interface IRateService {
    void initialize();
    RateResponse getRateForSymbol(String symbol);
     boolean isValidSymbol(String symbol);
}