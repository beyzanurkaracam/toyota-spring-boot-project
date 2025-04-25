package toyota.example.toyota_project.Helpers.Logging.Exceptions.Rest;

public class RateProcessingException extends RuntimeException {
    public RateProcessingException(String message) {
        super(message);
    }
    
    public RateProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}