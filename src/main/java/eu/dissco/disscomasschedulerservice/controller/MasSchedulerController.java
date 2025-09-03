package eu.dissco.disscomasschedulerservice.controller;

import eu.dissco.disscomasschedulerservice.Profiles;
import eu.dissco.disscomasschedulerservice.domain.MasJobRecord;
import eu.dissco.disscomasschedulerservice.domain.MasJobRequest;
import eu.dissco.disscomasschedulerservice.exception.InvalidRequestException;
import eu.dissco.disscomasschedulerservice.exception.NotFoundException;
import eu.dissco.disscomasschedulerservice.exception.UnprocessableEntityException;
import eu.dissco.disscomasschedulerservice.service.MasSchedulerService;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
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
  public ResponseEntity<List<MasJobRecord>> scheduleMas(
      @RequestBody Set<MasJobRequest> masJobRequests)
      throws UnprocessableEntityException, NotFoundException, InvalidRequestException {
    log.info("Received {} requests to schedule MASs", masJobRequests.size());
    return ResponseEntity.accepted().body(masSchedulerService.scheduleMass(masJobRequests));
  }

}
