package toyota.example.toyota_project.Kafka;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import toyota.example.toyota_project.Entities.Rate;

@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private static final Logger logger = LogManager.getLogger(KafkaProducerService.class);
    private static final String TOPIC = "toyota-topic";

    @Autowired
    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendRateMessage(String platformName, String rateName, Rate rate) {
        String message = formatRateMessage(platformName, rateName, rate);
        sendMessage(TOPIC, message);
    }

    private String formatRateMessage(String platformName, String rateName, Rate rate) {
        return String.format("%s|%.5f|%.5f|%s", 
                rateName, rate.getBid(), rate.getAsk(), rate.getTimestamp()); // "PF1_USDTRY" formatı
    }
    public void sendMessage(String topic, String message) {
        try {
            logger.info("Kafka'ya mesaj gönderiliyor: {}", message);
            kafkaTemplate.send(topic, message).get(); // Async işlemi senkron hale getirin
            logger.info("Mesaj başarıyla gönderildi: {}", message);
        } catch (Exception e) {
            logger.error("HATA: Mesaj gönderilemedi: {}", e.getMessage(), e);
        }
    }
}
