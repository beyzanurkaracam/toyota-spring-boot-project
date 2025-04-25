package toyota.example.toyota_project.Helpers.Logging.Exceptions.Tcp;
public class TCPServerException extends RuntimeException {
    public TCPServerException(String message) {
        super(message);
    }

    public TCPServerException(String message, Throwable cause) {
        super(message, cause);
    }
}

