package toyota.example.toyota_project.MainApp.Abstract;

import toyota.example.toyota_project.MainApp.Concrete.Coordinator;

public interface DataCollector {

  
	void connect(String platformName, String userid, String password);

	String getPlatformName();
	String getUserId();
	String getPassword();
	void disConnect(String platformName, String userid, String password);

	
	void subscribe(String platformName, String rateName);

	
	void unSubscribe(String platformName, String rateName);


	void loadConfig(String configFile);


	void setCallBack(Coordinator coordinator);


}
