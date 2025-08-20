package eu.dissco.disscomasschedulerservice.controller;

import eu.dissco.disscomasschedulerservice.Profiles;
import eu.dissco.disscomasschedulerservice.domain.MasJobRequest;
import eu.dissco.disscomasschedulerservice.exception.InvalidRequestException;
import eu.dissco.disscomasschedulerservice.exception.NotFoundException;
import eu.dissco.disscomasschedulerservice.exception.PidCreationException;
import eu.dissco.disscomasschedulerservice.service.MasSchedulerService;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestController
@RestControllerAdvice
@RequestMapping("/")
@RequiredArgsConstructor
@Profile(Profiles.WEB)
public class MasSchedulerController {

  private final MasSchedulerService masSchedulerService;

  @PostMapping("")
  public ResponseEntity<Void> scheduleMas(@RequestBody MasJobRequest masJobRequest)
      throws PidCreationException, NotFoundException, InvalidRequestException {
    log.info("Scheduling mas {} on digital object {}, requested by agent {}",
        masJobRequest.masId(),
        masJobRequest.targetId(),
        masJobRequest.agentId());
    masSchedulerService.scheduleMass(Set.of(masJobRequest));
    log.info("MAS {} Scheduled", masJobRequest.masId());
    return ResponseEntity.status(HttpStatus.ACCEPTED).build();
  }

}
