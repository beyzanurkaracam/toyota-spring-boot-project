package toyota.example.toyota_project.Simulation.TCP;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import toyota.example.toyota_project.Simulation.REST.ForexRateSimulator;
import toyota.example.toyota_project.Helpers.*;
import toyota.example.toyota_project.Helpers.Logging.LoggingHelper;
import toyota.example.toyota_project.Helpers.Logging.Exceptions.Tcp.ConnectionException;
import toyota.example.toyota_project.Helpers.Logging.Exceptions.Tcp.SubscriptionException;
import toyota.example.toyota_project.Helpers.Logging.Exceptions.Tcp.TCPServerException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TCPServerSimulation {
    private static final Logger logger = LogManager.getLogger(TCPServerSimulation.class);
    private static Map<String, Double> rates;
    private static Map<String, Double> spreads;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
    private static final Properties config = new Properties();
    private static long publishInterval;
    private static int maxPublications;
    private static int currentPublications = 0;

    private static ForexRateSimulator forexRateSimulator;

    public static void main(String[] args) {
        try {
            initialize();
            int port = Integer.parseInt(config.getProperty("port", "8081"));
            startServer(port);
        } catch (Exception e) {
            LoggingHelper.logError("Server initialization failed", e);
            System.exit(1);
        }
    }

    private static void initialize() throws IOException {
        LoggingHelper.logInfo("Initializing TCP Server Simulation");
        config.load(TCPServerSimulation.class.getResourceAsStream("/config.properties"));

        publishInterval = Long.parseLong(config.getProperty("publish.interval", "1000"));
        maxPublications = Integer.parseInt(config.getProperty("max.publications", "1000"));

        rates = new ConcurrentHashMap<>();
        spreads = new ConcurrentHashMap<>();

        forexRateSimulator = new ForexRateSimulator();

        String[] symbols = config.getProperty("symbols", "").split(",");

        for (String symbol : symbols) {
            String rateKey = "initial.rates." + symbol;
            String spreadKey = "spreads." + symbol;

            double initialRate = Double.parseDouble(config.getProperty(rateKey, "0.0"));
            double spread = Double.parseDouble(config.getProperty(spreadKey, "0.01"));

            rates.put(symbol, initialRate);
            spreads.put(symbol, spread);

            forexRateSimulator.initializeRate(symbol, initialRate);

            LoggingHelper.logInfo("Initialized rate for {}: {}, spread: {}", symbol, initialRate, spread);
        }
    }

    private static void startServer(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            LoggingHelper.logInfo("TCP Server started on port {}", port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                LoggingHelper.logInfo("New client connected from {}", clientSocket.getInetAddress());
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            LoggingHelper.logError("Server error occurred", e);
        }
    }

 
    private static void handleClient(Socket clientSocket) {
        Set<String> subscribedSymbols = ConcurrentHashMap.newKeySet();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            clientSocket.setKeepAlive(true);
            
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    sendUpdatedRates(subscribedSymbols, out);
                } catch (Exception e) {
                    TCPExceptionHandler.handleServerException(e, out);
                }
            }, 0, publishInterval, TimeUnit.MILLISECONDS);

            String clientMessage;
            while ((clientMessage = in.readLine()) != null) {
                try {
                    processClientMessage(clientMessage, subscribedSymbols, out);
                } catch (Exception e) {
                    TCPExceptionHandler.handleServerException(e, out);
                }
            }
        } catch (IOException e) {
            TCPExceptionHandler.handleServerException(
                new ConnectionException("Connection lost", e), 
                null
            );
        } finally {
            cleanup(clientSocket, scheduler);
        }
    }

   
    private static void processClientMessage(String message, Set<String> subscribedSymbols, PrintWriter out) {
        if (message == null || message.trim().isEmpty()) {
            throw new SubscriptionException("Empty or invalid message received");
        }

        String[] parts = message.split("\\|");
        if (parts.length < 1) {
            throw new SubscriptionException("Invalid request format");
        }

        try {
            switch (parts[0].toLowerCase()) {
                case "subscribe":
                    if (parts.length != 2) {
                        throw new SubscriptionException("Invalid subscribe format");
                    }
                    handleSubscribe(parts[1], subscribedSymbols, out);
                    break;

                case "unsubscribe":
                    if (parts.length != 2) {
                        throw new SubscriptionException("Invalid unsubscribe format");
                    }
                    handleUnsubscribe(parts[1], subscribedSymbols, out);
                    break;

                default:
                    throw new SubscriptionException("Invalid request type: " + parts[0]);
            }
        } catch (Exception e) {
            throw new TCPServerException("Error processing message: " + message, e);
        }
    }
    private static void handleSubscribe(String symbol, Set<String> subscribedSymbols, PrintWriter out) {
        if (!rates.containsKey(symbol)) {
            LoggingHelper.logWarn("Subscription attempt for unknown symbol: {}", symbol);
            out.println("ERROR|Rate data not found for " + symbol);
            return;
        }

        subscribedSymbols.add(symbol);
        LoggingHelper.logInfo("Client subscribed to symbol: {}", symbol);
        out.println("Subscribed to " + symbol);
    }

    private static void handleUnsubscribe(String symbol, Set<String> subscribedSymbols, PrintWriter out) {
        subscribedSymbols.remove(symbol);
        LoggingHelper.logInfo("Client unsubscribed from symbol: {}", symbol);
        out.println("Unsubscribed from " + symbol);
    }

    private static void sendUpdatedRates(Set<String> subscribedSymbols, PrintWriter out) {
        if (currentPublications >= maxPublications) {
            return;
        }

        for (String symbol : subscribedSymbols) {
            Double currentBid = rates.get(symbol);
            if (currentBid == null) {
                LoggingHelper.logError("Rate not found for symbol: {}", symbol);
                out.println("ERROR|Rate data not found for " + symbol);
                continue;
            }

            double newBid = forexRateSimulator.calculateNextRate(symbol, currentBid);
            double spread = spreads.getOrDefault(symbol, 0.01);
            double newAsk = newBid + (newBid * spread);

            rates.put(symbol, newBid);

            String timestamp = LocalDateTime.now().format(formatter);
            String data = String.format(Locale.US, 
                "%s|22:number:%.6f|25:number:%.6f|5:timestamp:%s",
                symbol, newBid, newAsk, timestamp);

            out.println(data);
            LoggingHelper.logDebug("Sent rate update: {}", data);
            currentPublications++;
        }
    }

    private static void cleanup(Socket clientSocket, ScheduledExecutorService scheduler) {
        try {
            scheduler.shutdown();
            if (!clientSocket.isClosed()) {
                clientSocket.close();
            }
            LoggingHelper.logInfo("Client connection cleaned up");
        } catch (IOException e) {
            LoggingHelper.logError("Error during cleanup", e);
        }
    }
}