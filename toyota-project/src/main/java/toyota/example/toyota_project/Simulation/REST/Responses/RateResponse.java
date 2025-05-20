package toyota.example.toyota_project.Simulation.REST.Responses;

import java.time.Instant;
import java.time.LocalDateTime;

public class RateResponse {
    private String rateName;
    private double bid;
    private double ask;
    private Instant timestamp;

    public RateResponse(String rateName, double bid, double ask, Instant timestamp) {
        this.rateName = rateName;
        this.bid = bid;
        this.ask = ask;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getRateName() { return rateName; }
    public void setRateName(String rateName) { this.rateName = rateName; }
    public double getBid() { return bid; }
    public void setBid(double bid) { this.bid = bid; }
    public double getAsk() { return ask; }
    public void setAsk(double ask) { this.ask = ask; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
