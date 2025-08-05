package eu.dissco.disscomasschedulerservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class DisscoMasSchedulerServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(DisscoMasSchedulerServiceApplication.class, args);
  }

}
