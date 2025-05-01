package toyota.example.toyota_project.Kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {
	
	@KafkaListener(topics="rates",groupId="toyota-grup")
	
	public void listen(String message) {
		System.out.println("Received message: "+message);
	}
	

}
