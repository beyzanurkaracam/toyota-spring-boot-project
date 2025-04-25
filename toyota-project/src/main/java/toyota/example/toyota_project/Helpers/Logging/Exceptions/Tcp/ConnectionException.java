package toyota.example.toyota_project.Helpers.Logging.Exceptions.Tcp;

public class ConnectionException extends TCPServerException {
    public ConnectionException(String message) {
        super(message);
    }

    public ConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}