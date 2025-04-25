package toyota.example.toyota_project.Helpers.Logging.Exceptions.Tcp;
public class SubscriptionException extends TCPServerException {
    public SubscriptionException(String message) {
        super(message);
    }

    public SubscriptionException(String message, Throwable cause) {
        super(message, cause);
    }
}