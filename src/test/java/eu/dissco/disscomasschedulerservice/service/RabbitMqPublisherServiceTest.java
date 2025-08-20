package eu.dissco.disscomasschedulerservice.service;

import static eu.dissco.disscomasschedulerservice.TestUtils.MAPPER;
import static eu.dissco.disscomasschedulerservice.TestUtils.givenMasJobRequest;
import static eu.dissco.disscomasschedulerservice.TestUtils.givenMasTarget;
import static org.mockito.BDDMockito.then;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.dissco.disscomasschedulerservice.properties.RabbitMqProperties;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class RabbitMqPublisherServiceTest {

  private RabbitMqPublisherService publisherService;
  @Mock
  RabbitTemplate rabbitTemplate;
  private final RabbitMqProperties rabbitMqProperties = new RabbitMqProperties();

  @BeforeEach
  void init() {
    publisherService = new RabbitMqPublisherService(rabbitMqProperties, MAPPER, rabbitTemplate);
  }

  @Test
  void testPublishMasJob() throws JsonProcessingException {
    // Given
    var message = new Message(
        MAPPER.writeValueAsString(givenMasTarget()).getBytes(StandardCharsets.UTF_8));

    // When
    publisherService.publishMasJob("mas-routing-key", givenMasTarget());

    // Then
    then(rabbitTemplate).should()
        .send(rabbitMqProperties.getMasExchangeName(), "mas-routing-key", message);
  }

  @Test
  void testDeadLetterRequest() throws JsonProcessingException {
    // Given
    var message = MAPPER.writeValueAsString(givenMasJobRequest());

    // When
    publisherService.deadLetterMasJobRequest(givenMasJobRequest());

    // Then
    then(rabbitTemplate).should()
        .convertAndSend(rabbitMqProperties.getDlqExchangeName(),
            rabbitMqProperties.getDlqRoutingKeyName(),
            message);
  }

  @Test
  void testDeadLetterRaw() {
    // Given

    // When
    publisherService.deadLetterRaw("bad event");

    // Then
    then(rabbitTemplate).should().convertAndSend(rabbitMqProperties.getDlqExchangeName(),
        rabbitMqProperties.getDlqRoutingKeyName(), "bad event");
  }
}
