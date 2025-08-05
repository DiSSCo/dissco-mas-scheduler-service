package eu.dissco.disscomasschedulerservice.service;
import eu.dissco.disscomasschedulerservice.Profiles;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Profile(Profiles.RABBIT_MQ)
@AllArgsConstructor
public class RabbitMqConsumerService {

  @RabbitListener(queues = {"rabbitmq.queue-name"}, containerFactory = "consumerBatchContainerFactory")
  public void getMessages(@Payload List<String> messages){

  }

}
