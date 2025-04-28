package toyota.example.toyota_project.Config;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CollectorConfig {

	private String name;
	private String userName;
	private String password;
	private String className;
	private String configFile;
	private List<String> rateNames;
	

}
