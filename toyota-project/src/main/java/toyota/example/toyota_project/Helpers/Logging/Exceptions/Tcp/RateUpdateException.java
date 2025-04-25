package toyota.example.toyota_project.Helpers.Logging.Exceptions.Tcp;

public class RateUpdateException extends TCPServerException {
    public RateUpdateException(String message) {
        super(message);
    }

    public RateUpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}