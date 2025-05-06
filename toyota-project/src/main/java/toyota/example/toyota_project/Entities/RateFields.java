package toyota.example.toyota_project.Entities;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import toyota.example.toyota_project.Config.CollectorConfig;
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public class RateFields {
    private final String rateName;
    private final double bid;
    private final double ask;
    private final String timestamp;

    @JsonCreator
    public RateFields(
        @JsonProperty("rateName") String rateName,
        @JsonProperty("bid") double bid,
        @JsonProperty("ask") double ask,
        @JsonProperty("timestamp") String timestamp) {
        this.rateName = rateName;
        this.bid = bid;
        this.ask = ask;
        this.timestamp = timestamp;
    }
}