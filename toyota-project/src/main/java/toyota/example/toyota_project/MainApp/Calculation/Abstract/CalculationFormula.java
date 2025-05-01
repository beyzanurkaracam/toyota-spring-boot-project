package toyota.example.toyota_project.MainApp.Calculation.Abstract;

import java.util.Map;

import toyota.example.toyota_project.Entities.RateFields;

public interface CalculationFormula {
	Map<String,Double>calculate(Map<String,RateFields>dependencies);

}
