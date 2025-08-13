package eu.dissco.disscomasschedulerservice.service;

import static eu.dissco.disscomasschedulerservice.TestUtils.MAPPER;
import static eu.dissco.disscomasschedulerservice.TestUtils.givenMasJobRequest;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RabbitMqConsumerServiceTest {

  private RabbitMqConsumerService consumerService;
  @Mock
  private RabbitMqPublisherService publisherService;
  @Mock
  private MasSchedulerService masSchedulerService;

  @BeforeEach
  void setup(){
    consumerService = new RabbitMqConsumerService(MAPPER, publisherService, masSchedulerService);
  }

  @Test
  void testGetMessages() throws Exception {
    // Given
    var messages = List.of(MAPPER.writeValueAsString(givenMasJobRequest()));

    // When
    consumerService.getMessages(messages);

    // Then
    then(masSchedulerService).should().scheduleMass(Set.of(givenMasJobRequest()));
  }

  @Test
  void testGetMessagesFailed() throws Exception {
    // Given
    var messages = List.of("bad message");

    // When
    consumerService.getMessages(messages);

    // Then
    then(publisherService).should().deadLetterRaw("bad message");
  }



}
