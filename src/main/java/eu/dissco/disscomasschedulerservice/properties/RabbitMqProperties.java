package eu.dissco.disscomasschedulerservice.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Positive;

@Data
@Validated
@ConfigurationProperties(prefix = "rabbitmq")
public class RabbitMqProperties {

  @Positive
  private int batchSize = 500;
  @NotBlank
  private String exchangeName = "mas-scheduler-exchange";
  @NotBlank
  private String routingKeyName = "mas-scheduler";
  @NotBlank
  private String queueName = "mas-scheduler-queue";
  @NotBlank
  private String dlqExchangeName = "mas-scheduler-exchange-dlq";
  @NotBlank
  private String dlqRoutingKeyName = "mas-scheduler-dlq";
}
