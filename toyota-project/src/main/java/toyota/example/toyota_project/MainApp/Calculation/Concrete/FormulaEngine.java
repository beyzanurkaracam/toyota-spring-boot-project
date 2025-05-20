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

import javax.script.Invocable;
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
    private static final Logger logger = LogManager.getLogger(FormulaEngine.class);
    private final Map<String, CalculationRule> calculationRules = new HashMap<>();
    private final ScriptEngineManager scriptManager = new ScriptEngineManager();

    @PostConstruct
    public void initialize() {
        try {
            logger.info("Loading calculation rules from /calculation_rules.json...");
            InputStream inputStream = getClass().getResourceAsStream("/calculation_rules.json");
            if (inputStream == null) {
                logger.error("Calculation rules file not found in classpath!");
                throw new RuntimeException("calculation_rules.json not found");
            }
            
            String jsonContent = new String(inputStream.readAllBytes());
            ObjectMapper mapper = new ObjectMapper();
            calculationRules.putAll(mapper.readValue(jsonContent, new TypeReference<>() {}));
            
            logger.info("Successfully loaded {} calculation rules", calculationRules.size());
            logger.debug("Loaded rules: {}", calculationRules.keySet());
        } catch (IOException e) {
            logger.error("Failed to load calculation rules: {}", e.getMessage(), e);
            throw new RuntimeException("Calculation rules initialization failed", e);
        }
    }

    @PostConstruct
    public void checkEngines() {
        logger.debug("Checking available script engines...");
        ScriptEngine groovyEngine = scriptManager.getEngineByName("groovy");
        
        if (groovyEngine == null) {
            logger.error("Groovy engine NOT FOUND!");
            scriptManager.getEngineFactories().forEach(factory -> 
                logger.warn("Available engine: {}", factory.getEngineName())
            );
        } else {
            logger.info("Groovy engine loaded successfully");
        }
    }

    public Map<String, Double> calculate(String rateName, Map<String, RateFields> dependencies) {
        logger.info("Starting calculation for {}...", rateName);
        logger.debug("Dependencies for {}: {}", rateName, dependencies.keySet());

        CalculationRule rule = calculationRules.get(rateName);
        if (rule == null) {
            logger.error("No calculation rule defined for: {}", rateName);
            logger.debug("Available rules: {}", calculationRules.keySet());
            throw new IllegalArgumentException("Calculation rule not found for " + rateName);
        }

        logger.info("Using {} formula for {}", rule.getFormulaType(), rateName);
        
        try {
            switch (rule.getFormulaType().toUpperCase()) {
                case "JAVA":
                    return executeJavaClass(rule.getFormula(), dependencies);
                case "GROOVY":
                    return executeScript("groovy", rule.getFormula(), dependencies);
                case "JAVASCRIPT":
                    return executeScript("javascript", rule.getFormula(), dependencies);
                default:
                    logger.error("Unsupported formula type: {}", rule.getFormulaType());
                    throw new UnsupportedOperationException("Unsupported formula type: " + rule.getFormulaType());
            }
        } finally {
            logger.info("Completed calculation for {}", rateName);
        }
    }

    private Map<String, Double> executeJavaClass(String className, Map<String, RateFields> dependencies) {
        logger.debug("Executing Java formula: {}", className);
        try {
            Class<?> clazz = Class.forName(className);
            CalculationFormula formula = (CalculationFormula) clazz.getDeclaredConstructor().newInstance();
            
            logger.debug("Invoking calculate() method...");
            Map<String, Double> results = formula.calculate(dependencies);
            
            logger.info("Java formula executed successfully. Results: {}", results);
            return results;
        } catch (Exception e) {
            logger.error("Java formula execution failed for {} | Error: {}", className, e.getMessage(), e);
            throw new RuntimeException("Java formula execution failed", e);
        }
    }

    private Map<String, Double> executeScript(String engineName, String script, Map<String, RateFields> dependencies) {
        ScriptEngine engine = scriptManager.getEngineByName(engineName);
        
        // Bağımlılıkları script'e aktar
        dependencies.forEach((key, value) -> {
            engine.put(key + "_bid", value.getBid());
            engine.put(key + "_ask", value.getAsk());
           
        });
        engine.put("usdmid", calculateUsdMid(dependencies));
        try {
            // Groovy için Map dönüşü
            if (engineName.equals("groovy")) {
                return (Map<String, Double>) engine.eval(script);
            }
            // JavaScript için JSON benzeri obje
            else if (engineName.equals("javascript")) {
                Invocable invocable = (Invocable) engine;
                engine.eval("function calculate() { " + script + " }");
                return (Map<String, Double>) invocable.invokeFunction("calculate");
            }
            throw new UnsupportedOperationException("Unsupported engine: " + engineName);
        } catch (Exception e) {
            throw new RuntimeException("Script error: " + e.getMessage(), e);
        }
    }
    private double calculateUsdMid(Map<String, RateFields> dependencies) {
        RateFields pf1UsdTry = dependencies.get("PF1_USDTRY");
        RateFields pf2UsdTry = dependencies.get("PF2_USDTRY");
        return (
            (pf1UsdTry.getBid() + pf2UsdTry.getBid()) / 2 +
            (pf1UsdTry.getAsk() + pf2UsdTry.getAsk()) / 2
        ) / 2;
    }
    private static class CalculationRule {
        private String formulaType;
        private String formula;

        public String getFormulaType() { return formulaType; }
        public String getFormula() { return formula; }

        @Override
        public String toString() {
            return "CalculationRule{" +
                "formulaType='" + formulaType + '\'' +
                ", formula='" + formula + '\'' +
                '}';
        }
    }
}