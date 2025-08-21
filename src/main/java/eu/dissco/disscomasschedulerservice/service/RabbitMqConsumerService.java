package eu.dissco.disscomasschedulerservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.disscomasschedulerservice.Profiles;
import eu.dissco.disscomasschedulerservice.domain.MasJobRequest;
import eu.dissco.disscomasschedulerservice.exception.InvalidRequestException;
import eu.dissco.disscomasschedulerservice.exception.NotFoundException;
import eu.dissco.disscomasschedulerservice.exception.UnprocessableEntityException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
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

  private final ObjectMapper mapper;
  private final RabbitMqPublisherService rabbitMqPublisherService;
  private final MasSchedulerService masSchedulerService;

  @RabbitListener(queues = {
      "${rabbitmq.queue-name}"}, containerFactory = "consumerBatchContainerFactory")
  public void getMessages(@Payload List<String> messages)
      throws UnprocessableEntityException, NotFoundException, InvalidRequestException {
    var events = messages.stream().map(message -> {
          try {
            return mapper.readValue(message, MasJobRequest.class);
          } catch (JsonProcessingException e) {
            log.error("Failed to parse event message", e);
            rabbitMqPublisherService.deadLetterRaw(message);
            return null;
          }
        }).filter(Objects::nonNull).collect(Collectors.toSet());
    masSchedulerService.scheduleMass(events);
  }

}
