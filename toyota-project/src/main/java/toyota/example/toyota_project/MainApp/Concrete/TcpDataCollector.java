package toyota.example.toyota_project.MainApp.Concrete;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import toyota.example.toyota_project.Config.ConfigLoader;
import toyota.example.toyota_project.MainApp.Abstract.CoordinatorCallBack;
import toyota.example.toyota_project.MainApp.Abstract.DataCollector;

public class TcpDataCollector implements DataCollector {
	private static final Logger logger = LogManager.getLogger(TcpDataCollector.class);
	  private final Properties config = new Properties();
	    private Socket socket;
	    private CoordinatorCallBack callback;
	    private String platformName;
	    private final AtomicBoolean isConnected = new AtomicBoolean(false);
	    private ExecutorService executor;
	    private PrintWriter writer;

	  
	    private String host;
	    private int port;
	    private int maxAttempts;
	    private long reconnectInterval;
	    private int connectionTimeout;
	    public TcpDataCollector() {
	        // Default constructor
	    }
	    
	@Override
	public void connect(String platformName, String userid, String password) {
		this.platformName=platformName;
		this.executor=executor;
		
		executor.submit(()->{
			int attempt=0;
			boolean success=false;
			while(attempt<maxAttempts && !isConnected.get()) {
				attempt++;
				logger.info("Connection attempt {}/{} to {}:{}", 
                        attempt, maxAttempts, host, port);
				try {
					
					socket= new Socket();
					socket.connect(new java.net.InetSocketAddress(host,port),connectionTimeout);
					isConnected.set(true);
					success=true;
                    logger.info("Connected to {}:{}", host, port);
break;

					
					
				}catch(IOException e) {
					logger.warn("Connection attempt {} failed: {}", attempt, e.getMessage());

                    try {
                        Thread.sleep(reconnectInterval);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
				}
			}
			 if (callback != null) {
	                callback.onConnect(platformName, success);
	            }

	            if (!success) {
	                logger.error("All connection attempts failed for {}", platformName);
	            }
		});
	}

	@Override
	public void disConnect(String platformName, String userid, String password) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void subscribe(String platformName, String rateName) {
		if(isConnected.get()) {
			String command="subscribe|"+rateName;
			writer.println(command);
			logger.info("Sent subscribe command: {}", command);
		}
	}

	@Override
	public void unSubscribe(String platformName, String rateName) {
		// TODO Auto-generated method stub
		
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
	

}
