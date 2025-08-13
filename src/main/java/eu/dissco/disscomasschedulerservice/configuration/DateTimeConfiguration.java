package eu.dissco.disscomasschedulerservice.configuration;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DateTimeConfiguration {

  public static final String DATE_STRING = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

  @Bean
  public DateTimeFormatter formatter() {
    return DateTimeFormatter.ofPattern(DATE_STRING).withZone(ZoneOffset.UTC);
  }

}
