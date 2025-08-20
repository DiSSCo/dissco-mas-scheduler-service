package eu.dissco.disscomasschedulerservice.service;

import static eu.dissco.disscomasschedulerservice.repository.RepositoryUtils.DOI_STRING;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import eu.dissco.backend.schema.MachineAnnotationService;
import eu.dissco.disscomasschedulerservice.Profiles;
import eu.dissco.disscomasschedulerservice.database.jooq.enums.JobState;
import eu.dissco.disscomasschedulerservice.database.jooq.enums.MjrTargetType;
import eu.dissco.disscomasschedulerservice.domain.MasJobRecord;
import eu.dissco.disscomasschedulerservice.domain.MasJobRequest;
import eu.dissco.disscomasschedulerservice.domain.MasJobRequestFull;
import eu.dissco.disscomasschedulerservice.domain.MasTarget;
import eu.dissco.disscomasschedulerservice.exception.InvalidRequestException;
import eu.dissco.disscomasschedulerservice.exception.NotFoundException;
import eu.dissco.disscomasschedulerservice.exception.PidCreationException;
import eu.dissco.disscomasschedulerservice.repository.DigitalMediaRepository;
import eu.dissco.disscomasschedulerservice.repository.DigitalSpecimenRepository;
import eu.dissco.disscomasschedulerservice.repository.MasJobRecordRepository;
import eu.dissco.disscomasschedulerservice.repository.MasRepository;
import eu.dissco.disscomasschedulerservice.web.HandleComponent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MasSchedulerService {

  private final MasRepository masRepository;
  private final MasJobRecordRepository masJobRecordRepository;
  private final RabbitMqPublisherService publisherService;
  private final HandleComponent handleComponent;
  private final DigitalSpecimenRepository specimenRepository;
  private final DigitalMediaRepository mediaRepository;
  private final Environment environment;

  public void scheduleMass(Set<MasJobRequest> masRequests)
      throws PidCreationException, InvalidRequestException, NotFoundException {
    Set<MasJobRequest> failedMasJobRequests = new HashSet<>();
    var uniqueMasIds = masRequests.stream().map(MasJobRequest::masId).collect(Collectors.toSet());
    var masMap = masRepository.getMasRecords(uniqueMasIds).stream()
        .collect(Collectors.toMap(MachineAnnotationService::getId, Function.identity()));
    validateMas(uniqueMasIds, masMap, masRequests, failedMasJobRequests);
    var targetObjectMap = getTargetObjects(masRequests, failedMasJobRequests);
    var filteredRequests = filterMasJobRequests(masRequests, failedMasJobRequests, targetObjectMap,
        masMap);
    log.info("Publishing {} MAS jobs", filteredRequests.size());
    var masJobRecordMap = createMasJobRecords(filteredRequests, masMap);
    var failedMasJobIds = new ArrayList<String>();
    for (var masRequest : filteredRequests) {
      var masJobRecord = masJobRecordMap.get(
          getMasJobRecordKey(masRequest.targetObject().get("@id").asText(), masRequest.masId()));
      var mas = masMap.get(masRequest.masId());
      try {
        var masTarget = new MasTarget(masRequest.targetObject(),
            masJobRecord.jobId(), masRequest.batching());
        publisherService.publishMasJob(mas.getOdsTopicName(), masTarget);
      } catch (JsonProcessingException e) {
        log.error("Failed to send masRecord: {}  to rabbitMQ", mas.getId());
        failedMasJobIds.add(masJobRecord.jobId());
        failedMasJobRequests.add(new MasJobRequest(
            masRequest.masId(),
            masRequest.targetId(),
            masRequest.batching(),
            masRequest.agentId(),
            masRequest.targetType()
        ));
      }
      processFailedJobs(failedMasJobIds, failedMasJobRequests);
    }
  }

  private void processFailedJobs(List<String> failedMasJobIds,
      Set<MasJobRequest> failedMasJobRequests) {
    if (!failedMasJobIds.isEmpty()) {
      masJobRecordRepository.markMasJobRecordsAsFailed(failedMasJobIds);
    }
    if (!failedMasJobIds.isEmpty()) {
      failedMasJobRequests.forEach(failedMasJobRequest -> {
        try {
          publisherService.deadLetterMasJobRequest(failedMasJobRequest);
        } catch (JsonProcessingException e) {
          log.error("Unable to dlq mas job request {}", failedMasJobRequest);
        }
      });
    }
  }

  private List<MasJobRequestFull> filterMasJobRequests(Set<MasJobRequest> masRequests,
      Set<MasJobRequest> failedMasJobRequests, Map<String, JsonNode> targetObjectMap,
      Map<String, MachineAnnotationService> masMap) throws InvalidRequestException {
    List<MasJobRequestFull> masJobRequests = new ArrayList<>();
    for (var masRequest : masRequests) {
      if (targetObjectMap.containsKey(masRequest.targetId())
          && masMap.containsKey(masRequest.masId())
          && checkIfBatchingComplies(masRequest, masMap.get(masRequest.masId()))
          && checkIfMasCompliesToTarget(targetObjectMap.get(masRequest.targetId()),
          masMap.get(masRequest.masId()))) {
        masJobRequests.add(
            new MasJobRequestFull(
                masRequest.masId(),
                masRequest.targetId(),
                targetObjectMap.get(masRequest.targetId()),
                masRequest.batching(),
                masRequest.agentId(),
                masRequest.targetType()
            ));
      } else {
        failedMasJobRequests.add(masRequest);
      }
    }
    return masJobRequests;
  }

  private Map<String, JsonNode> getTargetObjects(Set<MasJobRequest> masRequests,
      Set<MasJobRequest> failedMasJobRequests) throws NotFoundException {
    var specimenTargets = masRequests.stream()
        .filter(masRequest -> masRequest.targetType().equals(MjrTargetType.DIGITAL_SPECIMEN))
        .map(MasJobRequest::targetId)
        .map(id -> id.replace(DOI_STRING, ""))
        .collect(Collectors.toSet());
    var mediaTargets = masRequests.stream()
        .filter(masRequest -> masRequest.targetType().equals(MjrTargetType.MEDIA_OBJECT))
        .map(MasJobRequest::targetId)
        .map(id -> id.replace(DOI_STRING, ""))
        .collect(Collectors.toSet());
    var targetMapSpecimens = new HashMap<>(specimenRepository.getSpecimens(specimenTargets));
    var targetMapMedia = new HashMap<>(mediaRepository.getMedia(mediaTargets));
    verifyTargetExists(specimenTargets, targetMapSpecimens, masRequests, failedMasJobRequests);
    verifyTargetExists(mediaTargets, targetMapMedia, masRequests, failedMasJobRequests);
    return Stream.concat(targetMapSpecimens.entrySet().stream(), targetMapMedia.entrySet().stream())
        .filter(e -> e.getValue() != null)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private void verifyTargetExists(Set<String> targetIds, Map<String, JsonNode> targetMap,
      Set<MasJobRequest> masJobRequests, Set<MasJobRequest> failedMasJobRequests)
      throws NotFoundException {
    if (targetIds.size() > targetMap.size()) {
      var missingTargetIds = targetIds.stream().filter(targetId -> !targetMap.containsKey(targetId))
          .collect(
              Collectors.toSet());
      log.error("Targets not found: {}", missingTargetIds);
      if (environment.matchesProfiles(Profiles.WEB)) {
        throw new NotFoundException("Unable to retrieve all targets");
      } else {
        failedMasJobRequests.addAll(masJobRequests.stream().filter(
            masJobRequest -> missingTargetIds.contains(masJobRequest.targetId())
        ).collect(Collectors.toSet()));
      }
    }
  }

  private void validateMas(Set<String> uniqueMasIds,
      Map<String, MachineAnnotationService> masMap, Set<MasJobRequest> masJobRequests,
      Set<MasJobRequest> failedMasJobRequests) throws NotFoundException {
    if (uniqueMasIds.size() > masMap.size()) {
      var missingMas = uniqueMasIds.stream().filter(masId -> !masMap.containsKey(masId)).collect(
          Collectors.toSet());
      log.error("MASs not found: {}", missingMas);
      if (environment.matchesProfiles(Profiles.WEB)) {
        throw new NotFoundException("Unable to retrieve all MASs");
      }
      failedMasJobRequests.addAll(
          masJobRequests.stream()
              .filter(masJobRequest -> missingMas.contains(masJobRequest.masId()))
              .collect(Collectors.toSet()));
    }
  }

  private Map<String, MasJobRecord> createMasJobRecords(List<MasJobRequestFull> filteredRequests,
      Map<String, MachineAnnotationService> masMap)
      throws PidCreationException {
    if (filteredRequests.isEmpty()) {
      return Map.of();
    }
    log.info("Requesting {} handles from API", filteredRequests.size());
    var handles = handleComponent.postHandle(filteredRequests.size());
    var handleItr = handles.iterator();
    var masJobRecordList = filteredRequests.stream().map(
        masRequest -> new MasJobRecord(
            handleItr.next(),
            JobState.SCHEDULED,
            masRequest.masId(),
            masRequest.targetObject().get("@id").asText(),
            masRequest.targetType(),
            masRequest.agentId(),
            masRequest.batching(),
            masMap.get(masRequest.masId()).getOdsTimeToLive())).toList();
    masJobRecordRepository.createNewMasJobRecord(masJobRecordList);
    return masJobRecordList.stream()
        .collect(Collectors.toMap(
            masJobRecord -> getMasJobRecordKey(masJobRecord.targetId(), masJobRecord.masId()),
            Function.identity()));
  }

  // We need a temp key to link the job request to the mas job record we just created
  // Unfortunately we can't use the mas job record id because that's not in the request
  private static String getMasJobRecordKey(String targetId, String masId) {
    return targetId + "-" + masId;
  }

  private boolean checkIfMasCompliesToTarget(JsonNode jsonNode,
      MachineAnnotationService machineAnnotationService) throws InvalidRequestException {
    var filters = machineAnnotationService.getOdsHasTargetDigitalObjectFilter();
    if (filters == null) {
      return true;
    }
    var fields = filters.getAdditionalProperties();
    var complies = true;
    for (var stringObjectEntry : fields.entrySet()) {
      var allowedValues = (List<Object>) stringObjectEntry.getValue();
      var fieldKey = stringObjectEntry.getKey();
      try {
        var values = JsonPath.read(jsonNode.toString(), fieldKey);
        if (values instanceof List<?>) {
          var valueList = (List<Object>) values;
          if (valueList.isEmpty() || (!allowedValues.contains("*") && !allowedValues.contains(
              valueList))) {
            complies = false;
          }
        } else if (values instanceof Object && (!allowedValues.contains(values)
            && !allowedValues.contains("*"))) {
          complies = false;
        }
      } catch (PathNotFoundException e) {
        log.warn("Key: {} not found in json: {}", fieldKey, jsonNode);
        complies = false;
      }
    }
    validateComplies(jsonNode, machineAnnotationService, complies);
    return complies;
  }

  private void validateComplies(JsonNode jsonNode,
      MachineAnnotationService machineAnnotationService, boolean complies)
      throws InvalidRequestException {
    if (!complies && environment.matchesProfiles(Profiles.WEB)) {
      throw new InvalidRequestException(
          String.format("MAS %s does not comply with target %s", machineAnnotationService.getId(),
              jsonNode.get("@id").asText()));
    } else {
      log.debug("MAS {} does not comply with target {}", machineAnnotationService.getId(),
          jsonNode.get("@id").asText());
    }
  }

  private boolean checkIfBatchingComplies(MasJobRequest masJobRequest,
      MachineAnnotationService mas) throws InvalidRequestException {
    if (masJobRequest.batching() && Boolean.FALSE.equals(mas.getOdsBatchingPermitted())) {
      log.warn("MAS {} is not batchable, but it has been requested to run as a batch", mas.getId());
      if (environment.matchesProfiles(Profiles.WEB)) {
        throw new InvalidRequestException(
            "MAS is not batchable, but it has been requested to run as a batch");
      }
      return false;
    }
    return true;
  }
}
