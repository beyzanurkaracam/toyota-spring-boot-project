package toyota.example.toyota_project.Helpers.Logging;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LoggingHelper {
    private static final Logger logger = LogManager.getLogger(LoggingHelper.class);

    // Info seviyesinde loglama (statik metod)
    public static void logInfo(String message, Object... args) {
        logger.info(message, args);
    }

    // Debug seviyesinde loglama (statik metod)
    public static void logDebug(String message, Object... args) {
        logger.debug(message, args);
    }

    // Warn seviyesinde loglama (statik metod)
    public static void logWarn(String message, Object... args) {
        logger.warn(message, args);
    }

    public static void logError(String message, Object... args) {
        logger.error(message, args);
    }
    // Error seviyesinde loglama (statik metod)
    public static void logError(String message, Throwable throwable, Object... args) {
        logger.error(message, args, throwable);
    }
    
}