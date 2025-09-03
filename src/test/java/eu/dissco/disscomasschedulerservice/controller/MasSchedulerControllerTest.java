package eu.dissco.disscomasschedulerservice.controller;

import static eu.dissco.disscomasschedulerservice.TestUtils.givenMasJobRequest;
import static org.mockito.BDDMockito.then;

import eu.dissco.disscomasschedulerservice.service.MasSchedulerService;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MasSchedulerControllerTest {

  private MasSchedulerController controller;
  @Mock
  private MasSchedulerService service;

  @BeforeEach
  void init(){
    this.controller = new MasSchedulerController(service);
  }

  @Test
  void testScheduleMas() throws Exception {
    // When
    controller.scheduleMas(Set.of(givenMasJobRequest()));

    // Then
    then(service).should().scheduleMass(Set.of(givenMasJobRequest()));
  }


}
