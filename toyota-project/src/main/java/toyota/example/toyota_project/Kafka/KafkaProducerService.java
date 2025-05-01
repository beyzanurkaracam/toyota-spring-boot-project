package toyota.example.toyota_project.Kafka;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import toyota.example.toyota_project.Entities.Rate;

@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private static final Logger logger = LogManager.getLogger(KafkaProducerService.class);
    private static final String TOPIC = "toyota-topic";

    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendRateMessage(String platformName, String rateName, Rate rate) {
        String message = formatRateMessage(platformName, rateName, rate);
        sendMessage(TOPIC, message);
    }

    private String formatRateMessage(String platformName, String rateName, Rate rate) {
        return String.format("%s_%s|%.5f|%.5f|%s", 
                platformName, rateName, rate.getBid(), rate.getAsk(), rate.getTimestamp());
    }

    public void sendMessage(String topic, String message) {
        try {
            kafkaTemplate.send(topic, message);
            logger.info("Message sent to topic {}: {}", topic, message);
        } catch (Exception e) {
            logger.error("Error sending message to Kafka: {}", e.getMessage());
        }
    }
}
