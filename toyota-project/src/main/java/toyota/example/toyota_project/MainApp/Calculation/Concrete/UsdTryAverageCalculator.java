package toyota.example.toyota_project.MainApp.Calculation.Concrete;

import java.util.HashMap;
import java.util.Map;
import toyota.example.toyota_project.Entities.RateFields;
import toyota.example.toyota_project.MainApp.Calculation.Abstract.CalculationFormula;

public class UsdTryAverageCalculator implements CalculationFormula {
    @Override
    public Map<String, Double> calculate(Map<String, RateFields> dependencies) {
        RateFields pf1 = dependencies.get("PF1_USDTRY");
        RateFields pf2 = dependencies.get("PF2_USDTRY");
        
        Map<String, Double> results = new HashMap<>();
        results.put("bid", (pf1.getBid() + pf2.getBid()) / 2);
        results.put("ask", (pf1.getAsk() + pf2.getAsk()) / 2);
        return results;
    }
}
