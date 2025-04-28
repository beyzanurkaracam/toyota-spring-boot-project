package toyota.example.toyota_project.Entities;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import toyota.example.toyota_project.Config.CollectorConfig;
@Data
public class RateFields  {
	  private final String rateName;
	    private final double bid;
	    private final double ask;
	    private final String timestamp;

	    public RateFields(String rateName, double bid, double ask, String timestamp) {
	        this.rateName = rateName;
	        this.bid = bid;
	        this.ask = ask;
	        this.timestamp = timestamp;
	    }
}