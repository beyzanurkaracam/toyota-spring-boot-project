package toyota.example.toyota_project.Simulation.TCP;

import java.io.PrintWriter;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import toyota.example.toyota_project.Helpers.Logging.LoggingHelper;
import toyota.example.toyota_project.Helpers.Logging.Exceptions.Tcp.ConnectionException;
import toyota.example.toyota_project.Helpers.Logging.Exceptions.Tcp.RateUpdateException;
import toyota.example.toyota_project.Helpers.Logging.Exceptions.Tcp.SubscriptionException;

public class TCPExceptionHandler {
    
    public static void handleServerException(Exception e, PrintWriter out) {
        if (e instanceof ConnectionException) {
            handleConnectionException((ConnectionException) e, out);
        } else if (e instanceof SubscriptionException) {
            handleSubscriptionException((SubscriptionException) e, out);
        } else if (e instanceof RateUpdateException) {
            handleRateUpdateException((RateUpdateException) e, out);
        } else if (e instanceof SocketTimeoutException) {
            handleSocketTimeoutException((SocketTimeoutException) e, out);
        } else if (e instanceof SocketException) {
            handleSocketException((SocketException) e, out);
        } else {
            handleGenericException(e, out);
        }
    }

    private static void handleConnectionException(ConnectionException e, PrintWriter out) {
        LoggingHelper.logError("Connection error: {}", e.getMessage());
        sendErrorResponse(out, "CONNECTION_ERROR", e.getMessage());
    }

    private static void handleSubscriptionException(SubscriptionException e, PrintWriter out) {
        LoggingHelper.logError("Subscription error: {}", e.getMessage());
        sendErrorResponse(out, "SUBSCRIPTION_ERROR", e.getMessage());
    }

    private static void handleRateUpdateException(RateUpdateException e, PrintWriter out) {
        LoggingHelper.logError("Rate update error: {}", e.getMessage());
        sendErrorResponse(out, "RATE_UPDATE_ERROR", e.getMessage());
    }

    private static void handleSocketTimeoutException(SocketTimeoutException e, PrintWriter out) {
        LoggingHelper.logError("Socket timeout: {}", e.getMessage());
        sendErrorResponse(out, "TIMEOUT_ERROR", "Connection timed out");
    }

    private static void handleSocketException(SocketException e, PrintWriter out) {
        LoggingHelper.logError("Socket error: {}", e.getMessage());
        sendErrorResponse(out, "SOCKET_ERROR", "Connection error occurred");
    }

    private static void handleGenericException(Exception e, PrintWriter out) {
        LoggingHelper.logError("Unexpected error: {}", e.getMessage());
        sendErrorResponse(out, "INTERNAL_ERROR", "An internal error occurred");
    }

    private static void sendErrorResponse(PrintWriter out, String errorCode, String message) {
        if (out != null && !out.checkError()) {
            out.println(String.format("ERROR|%s|%s", errorCode, message));
        }
    }
}