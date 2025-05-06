package toyota.example.toyota_project.MainApp.Calculation.Concrete;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.logging.log4j.LogManager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import toyota.example.toyota_project.Entities.RateFields;
import toyota.example.toyota_project.MainApp.Calculation.Abstract.CalculationFormula;
@Component
public class FormulaEngine {
	private static final Logger logger=LogManager.getLogger(FormulaEngine.class);
	private final Map<String,CalculationRule> calculationRules=new HashMap<>();
	private final ScriptEngineManager scriptManager=new ScriptEngineManager();
	@PostConstruct
	public void initialize() {
	    try {
	        InputStream inputStream = getClass().getResourceAsStream("/calculation_rules.json");
	        String jsonContent = new String(inputStream.readAllBytes());
	        ObjectMapper mapper = new ObjectMapper();
	        calculationRules.putAll(mapper.readValue(jsonContent, new TypeReference<>() {}));
	        logger.info("Successfully loaded {} calculation rules", calculationRules.size());
	    } catch (IOException e) {
	        logger.error("Failed to load calculation rules: {}", e.getMessage());
	        throw new RuntimeException("Calculation rules initialization failed", e);
	    }
	}
	 @PostConstruct
	    public void checkEngines() {
	        ScriptEngine groovyEngine = scriptManager.getEngineByName("groovy");
	        if (groovyEngine == null) {
	            logger.error("Groovy engine NOT FOUND!");
	        } else {
	            logger.info("Groovy engine loaded successfully.");
	        }
	    }
	 public Map<String,Double> calculate(String rateName,Map<String,RateFields>dependencies){
		 CalculationRule rule=calculationRules.get(rateName);
		 
		 if(rule==null) {
			 logger.error("No calculation rule defined for: {}", rateName);
			 throw new IllegalArgumentException("Calculation rule not found");
		 }
		 switch(rule.getFormulaType().toUpperCase()) {
		 case "JAVA":
			 return executeJavaClass(rule.getFormula(),dependencies);
		 case "GROOVY":
			 return executeScript("groovy",rule.getFormula(),dependencies);
		 case "JAVASCRİPT":
			 return executeScript("javascript",rule.getFormula(),dependencies);
			 default:
				 throw new UnsupportedOperationException("Unsupported formula type: " + rule.getFormulaType());
		 }
	 }
	 private Map<String,Double> executeJavaClass(String className,Map<String,RateFields>dependencies){
		 try {
	            Class<?> clazz = Class.forName(className);
	            CalculationFormula formula = (CalculationFormula) clazz.getDeclaredConstructor().newInstance();
	            return formula.calculate(dependencies);
	        } catch (Exception e) {
	            throw new RuntimeException("Java formula execution failed", e);
	        }
	 }
	 
	 private Map<String,Double>executeScript(String engineName,String script,Map<String,RateFields>dependencies){
		 ScriptEngine engine = scriptManager.getEngineByName(engineName);
		    if (engine == null) {
		        logger.error("{} engine not found. Available engines: {}", engineName, scriptManager.getEngineFactories());
		        throw new RuntimeException("Script engine not found: " + engineName);
		    }
		  try {
	            // Bağımlılıkları script'e aktar
	            dependencies.forEach((key, value) -> {
	                engine.put(key + "_bid", value.getBid());
	                engine.put(key + "_ask", value.getAsk());
	            });

	            // Script'i çalıştır ve sonuçları al
	            Map<String, Double> results = new HashMap<>();
	            results.put("bid", (Double) engine.eval(script));
	            results.put("ask", (Double) engine.eval(script.replace("bid", "ask")));
	            return results;
	        } catch (ScriptException e) {
	            throw new RuntimeException("Script execution error", e);
	        }
		  
		  
	 }
	 private static class CalculationRule{
		 private String formulaType;
		 private String formula;
		 
		 public String getFormulaType() {return formulaType;}
		 public String getFormula() {return formula;}
		 
		 
	 }

}
