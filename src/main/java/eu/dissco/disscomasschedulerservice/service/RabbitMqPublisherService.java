package eu.dissco.disscomasschedulerservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.disscomasschedulerservice.domain.MasTarget;
import eu.dissco.disscomasschedulerservice.properties.RabbitMqProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitMqPublisherService {

  private final RabbitMqProperties rabbitMqProperties;  
  private final ObjectMapper mapper;
  private final RabbitTemplate rabbitTemplate;

  public void publishMasJob(String routingKey, MasTarget masTarget) throws JsonProcessingException {
    log.debug("Publishing to MAS exchange with routing key: {} and with object: {}", routingKey, masTarget);
    rabbitTemplate.convertAndSend(rabbitMqProperties.getMasExchangeName(), routingKey, mapper.writeValueAsString(masTarget));
  }
  
  public void deadLetterRaw(String event){
    rabbitTemplate.convertAndSend(rabbitMqProperties.getDlqExchangeName(), rabbitMqProperties.getDlqRoutingKeyName(), event);
  }
  
}
