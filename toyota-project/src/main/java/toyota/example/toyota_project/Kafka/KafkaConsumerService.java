package toyota.example.toyota_project.Kafka;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

//@Service
public class KafkaConsumerService {
	
	 private static final Logger logger = LogManager.getLogger(KafkaConsumerService.class);

	    @KafkaListener(topics = "rates", groupId = "toyota-grup")
	    public void listen(String message) {
	        logger.info("Kafka'dan mesaj alındı: {}", message);
	        // Mesaj işleme kodları buraya
	    }

}
