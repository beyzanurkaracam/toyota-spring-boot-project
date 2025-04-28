package toyota.example.toyota_project.Entities;

import lombok.Data;

@Data
public class Rate {
	private final double bid;
	private final double ask;
	private final String timestamp;
	
	public Rate(double bid,double ask,String timestamp) {
		this.bid=bid;
		this.ask=ask;
		this.timestamp=timestamp;
	}
}
