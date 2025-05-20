package toyota.example.toyota_project.MainApp.Concrete;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.Instant;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import toyota.example.toyota_project.Config.ConfigLoader;
import toyota.example.toyota_project.Entities.Rate;
import toyota.example.toyota_project.Entities.RateFields;
import toyota.example.toyota_project.Helpers.Logging.TimestampFormatter;
import toyota.example.toyota_project.MainApp.Abstract.CoordinatorCallBack;
import toyota.example.toyota_project.MainApp.Abstract.DataCollector;

public class TcpDataCollector implements DataCollector {
	private static final Logger logger = LogManager.getLogger(TcpDataCollector.class);
	private final Properties config = new Properties();
	private Socket socket;
	private CoordinatorCallBack callback;
	private String platformName;
	private final AtomicBoolean isConnected = new AtomicBoolean(false);
	private BufferedReader reader;
	private AtomicBoolean isRunning = new AtomicBoolean(false);
	//private ExecutorService executor;
	private PrintWriter writer;
	private final Set<String>receivedRates=ConcurrentHashMap.newKeySet();
	private final Set<String>subscribedRates=ConcurrentHashMap.newKeySet();
	@Autowired
	@Qualifier("taskExecutor")
	private ExecutorService executor;
	
	private String host;
	private int port;
	private int maxAttempts;
	private long reconnectInterval;
	private int connectionTimeout;
	
	private String userId;
	private String password;



  
	public TcpDataCollector() {
		// Default constructor
	}
	
	@Override
	public void connect(String platformName, String userid, String password) {
		this.platformName = platformName;
		this.userId = userid;
		this.password = password;
		
		if (this.executor == null) {
			this.executor = Executors.newCachedThreadPool();
			logger.info("Executor initialized for {}", platformName);
		}
		
		executor.submit(()->{
			int attempt = 0;
			boolean success = false;
			while(attempt < maxAttempts && !isConnected.get()) {
				attempt++;
				logger.info("Connecting to {}:{} (Attempt {}/{})", 
					host, port, attempt, maxAttempts);
				
				try {
					socket = new Socket();
					socket.setSoTimeout(connectionTimeout);
					socket.connect(new java.net.InetSocketAddress(host, port), connectionTimeout);
					
					isConnected.set(true);
					success = true;
					logger.info("Successfully connected to {}:{}", host, port);
					
					
					this.writer = new PrintWriter(socket.getOutputStream(), true);
					this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					isRunning.set(true);
				  
					startDataReadingThread();
					break;
					
				} catch (IOException e) {
					logger.error("Connection attempt {} failed: {}", attempt, e.getMessage());
					try {
						Thread.sleep(reconnectInterval);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						break;
					}
				}
			}
			
			if (callback != null) {
				callback.onConnect(platformName, success);
			}
			
			if (!success) {
				logger.error("Failed to connect after {} attempts", maxAttempts);
			}
		});
	}
	
	
	
   private void startDataReadingThread() throws IOException {
		executor.submit(()->{
			logger.info("Data reading thread started for {}", platformName);
			String line;
			try {
				while(isRunning.get()) {
					try {
						if ((line = reader.readLine()) != null) {
							logger.info("Received data: {}", line);
							processIncomingData(line);
						} else {
							logger.warn("Received null line, connection might be closed");
							break;
						}
					} catch (Exception e) {
						logger.error("Error reading data: {}", e.getMessage());
						break;
					}
				}
			} finally {
				logger.info("Data reading thread stopped for {}", platformName);
				isRunning.set(false);
				isConnected.set(false);
				connect(platformName, userId, password); // User ID ve şifre ile yeniden bağlan
			}
		});
	}
	
	private void processIncomingData(String rawData) {
		try {
			logger.info("Received raw data: {}", rawData);
			
		   
			
			if (rawData.startsWith("ERROR|")) {
				logger.error("Received error from server: {}", rawData);
				return;
			}
	
		   
			String[] parts = rawData.split("\\|");
			if (parts.length < 4) {
				logger.error("Invalid data format: {}", rawData);
				return;
			}
	
			String rateName = parts[0];
			
			
			if (rawData.contains(":number:NaN")) {
				logger.warn("Received NaN value for symbol {}, skipping", rateName);
				return;
			}
	
			double bid = 0, ask = 0;
			String timestamp = Instant.now().toString();
	
			try {
			   
				String[] bidParts = parts[1].split(":");
				if (bidParts.length >= 3 && bidParts[1].equals("number")) {
					bid = Double.parseDouble(bidParts[2]);
				}
	
				// Parse ask (25:number:1.234568)
				String[] askParts = parts[2].split(":");
				if (askParts.length >= 3 && askParts[1].equals("number")) {
					ask = Double.parseDouble(askParts[2]);
				}
	
				// Parse timestamp (5:timestamp:2023-...)
				String[] timeParts = parts[3].split(":");
				if (timeParts.length >= 3 && timeParts[1].equals("timestamp")) {
					timestamp = timeParts[2];
				}
				
				// Timestamps'i standartlaştır
				String standardizedTimestamp = TimestampFormatter.standardizeTimestamp(timestamp);
				
				// Validate parsed values
				if (Double.isNaN(bid) || Double.isNaN(ask)) {
					logger.warn("Invalid numeric values in data: {}", rawData);
					return;
				}
	
				RateFields rateFields = new RateFields(rateName, bid, ask, standardizedTimestamp);
				boolean isFirstTime = receivedRates.add(rateName);
	
				if (callback != null) {
					if (isFirstTime) {
						Rate rate = new Rate(bid, ask, standardizedTimestamp);
						callback.onRateAvailable(platformName, rateName, rate);
					} else {
						callback.onRateUpdate(platformName, rateName, rateFields);
					}
				}
			} catch (Exception e) {
				logger.error("Parse error in data parts: {}", rawData, e);
			}
		} catch (Exception e) {
			logger.error("Data parse error: {}", rawData, e);
		}
	}

	@Override
	public void disConnect(String platformName, String userid, String password) {
		logger.info("Disconnecting from {}:{}...", host, port);
		isRunning.set(false);
		isConnected.set(false);
	
		try {
			if (writer != null) writer.close();
			if (reader != null) reader.close();
			if (socket != null && !socket.isClosed()) {
				socket.close();
				logger.info("Socket closed for {}:{}", host, port);
			}
			if (executor != null) {  // ← Thread pool'ları kapat
				executor.shutdownNow(); 
			}
		} catch (IOException e) {
			logger.error("Error while disconnecting: {}", e.getMessage());
		}
		callback.onDisConnect(platformName, true);
	}

	@Override
	public void subscribe(String platformName, String rateName) {
		if (!isConnected.get()) {
			logger.error("Bağlantı yok: {}", platformName);
			return;
		}
		
		String command="subscribe|"+rateName;
		writer.println(command);
		subscribedRates.add(rateName);
		logger.info("Sent subscribe command: {}", command);
	}

	@Override
	public void unSubscribe(String platformName, String rateName) {
		if(!subscribedRates.contains(rateName)) {
			logger.warn("Zaten unssubscribe: {}", rateName);
			return;
		}
		
		String command="unsubscribe|"+rateName;
		writer.println(command);
		subscribedRates.remove(rateName);
		logger.info("{} için unsubscribe olundu: {}", rateName, platformName);
	}

	@Override
	public void loadConfig(String configFile) {
		ConfigLoader loader = new ConfigLoader(configFile);
		Properties loadedProps = loader.loadProperties(configFile);
		this.config.putAll(loadedProps); 
		this.platformName = config.getProperty("platform.name");
		
		this.host = config.getProperty("tcp.host", "127.0.0.1");
		this.port = Integer.parseInt(config.getProperty("tcp.port", "8081"));
		this.connectionTimeout = Integer.parseInt(config.getProperty("tcp.connectionTimeout", "5000"));
		this.maxAttempts = Integer.parseInt(config.getProperty("tcp.maxReconnectAttempts", "3"));
		this.reconnectInterval = Long.parseLong(config.getProperty("tcp.reconnectInterval", "3000"));
	
		logger.info("TCP Collector configured - Host: {}, Port: {}, Timeout: {}ms, MaxAttempts: {}, ReconnectInterval: {}ms",
				host, port, connectionTimeout, maxAttempts, reconnectInterval);
	}

	@Override
	public void setCallBack(Coordinator coordinator) {
		this.callback = coordinator;
	}

	@Override
	public String getPlatformName() {
		return this.platformName;
	}

	@Override
	public String getUserId() {
		 return this.userId;
	}

	@Override
	public String getPassword() {
		return this.password;
	}
}