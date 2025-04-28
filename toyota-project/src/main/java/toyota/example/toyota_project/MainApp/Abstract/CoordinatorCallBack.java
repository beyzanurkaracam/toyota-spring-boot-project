package toyota.example.toyota_project.MainApp.Abstract;

public interface CoordinatorCallBack {
	
	void onConnect(String platformName, Boolean status);

	
	void onDisConnect(String platformName, Boolean status);
	
	void initialize();

	
	//void onRateAvailable(String platformName, String rateName, Rate rate);

	
	//void onRateUpdate(String platformName, String rateName, RateFields rateFields);

	
	//void onRateStatus(String platformName, String rateName, RateStatus rateStatus);


}
