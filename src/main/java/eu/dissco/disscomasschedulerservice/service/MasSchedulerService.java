package eu.dissco.disscomasschedulerservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import eu.dissco.backend.schema.MachineAnnotationService;
import eu.dissco.disscomasschedulerservice.database.jooq.enums.JobState;
import eu.dissco.disscomasschedulerservice.database.jooq.enums.MjrTargetType;
import eu.dissco.disscomasschedulerservice.domain.MasJobRecord;
import eu.dissco.disscomasschedulerservice.domain.MasJobRequest;
import eu.dissco.disscomasschedulerservice.domain.MasTarget;
import eu.dissco.disscomasschedulerservice.exception.InvalidRequestException;
import eu.dissco.disscomasschedulerservice.exception.PidCreationException;
import eu.dissco.disscomasschedulerservice.repository.MasJobRecordRepository;
import eu.dissco.disscomasschedulerservice.repository.MasRepository;
import eu.dissco.disscomasschedulerservice.web.HandleComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MasSchedulerService {

  private final MasRepository masRepository;
  private final MasJobRecordRepository masJobRecordRepository;
  private final RabbitMqPublisherService publisherService;
  private final HandleComponent handleComponent;


  public void scheduleMass(List<MasJobRequest> masRequests) throws PidCreationException {
    var uniqueMasIds = masRequests.stream().map(MasJobRequest::masId).collect(Collectors.toSet());
    var masMap = masRepository.getMasRecords(uniqueMasIds).stream()
        .collect(Collectors.toMap(MachineAnnotationService::getId, Function.identity()));
    var filteredRequests = masRequests.stream()
        .filter(masRequest -> checkIfMasCompliesToTarget(masRequest.targetObject(), masMap.get(masRequest.masId())))
        .filter(masRequest -> checkIfBatchingComplies(masRequest, masMap.get(masRequest.masId())))
        .toList();
    var masJobRecordMap = createMasJobRecord(filteredRequests, masMap);
    var failedMasJobs = new ArrayList<String>();
    for (var masRequest : filteredRequests) {
      var masJobRecord = masJobRecordMap.get(
          getMasJobRecordKey(masRequest.targetObject().get("@id").asText(), masRequest.masId()));
      var mas = masMap.get(masRequest.masId());
      try {
        var masTarget = new MasTarget(masRequest.targetObject(), masJobRecord.jobId(), masRequest.batching());
        publisherService.sendObjectToQueue(mas.getOdsTopicName(), masTarget);
      } catch (JsonProcessingException e) {
        log.error("Failed to send masRecord: {}  to rabbitMQ", mas.getId());
        failedMasJobs.add(masJobRecord.jobId());
      }
      if (!failedMasJobs.isEmpty()) {
        masJobRecordRepository.markMasJobRecordsAsFailed(failedMasJobs);
      }
    }
  }

  private Map<String, MasJobRecord> createMasJobRecord(List<MasJobRequest> requests,
      Map<String, MachineAnnotationService> masMap) throws PidCreationException, InvalidRequestException {
    log.info("Requesting {} handles from API", requests.size());
    var handles = handleComponent.postHandle(requests.size());
    var handleItr = handles.iterator();
    var masJobRecordList = requests.stream().map(
        masRequest -> new MasJobRecord(
            handleItr.next(),
            JobState.SCHEDULED,
            masRequest.masId(),
            masRequest.targetObject().get("@id").asText(),
            getTargetType(masRequest.targetObject().get("@type").asText()),
            masRequest.agentId(),
            masRequest.batching(),
            masMap.get(masRequest.masId()).getOdsTimeToLive())).toList();
    masJobRecordRepository.createNewMasJobRecord(masJobRecordList);
    return masJobRecordList.stream()
        .collect(Collectors.toMap(
            masJobRecord -> getMasJobRecordKey(masJobRecord.targetId(), masJobRecord.masId()),
            Function.identity()));
  }

  private static MjrTargetType getTargetType(String type) {
    if (type.equals("ods:DigitalSpecimen")){
      return MjrTargetType.DIGITAL_SPECIMEN;
    } else if (type.equals("ods:DigitalMedia")) {
      return MjrTargetType.MEDIA_OBJECT;
    }
    log.error("Unrecognized target @type: {}", type);
    throw new InvalidRequestException("Unrecognized target @type" + type);
  }

  private static String getMasJobRecordKey(String targetId, String masId) {
    return targetId + "-" + masId;
  }

  private static boolean checkIfMasCompliesToTarget(JsonNode jsonNode,
      MachineAnnotationService machineAnnotationService) {
    var filters = machineAnnotationService.getOdsHasTargetDigitalObjectFilter();
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
    return complies;
  }

  private boolean checkIfBatchingComplies(MasJobRequest masJobRequest, MachineAnnotationService mas) {
    if (masJobRequest.batching() && Boolean.FALSE.equals(mas.getOdsBatchingPermitted())) {
      log.warn("MAS {} is not batchable, but it has been requested to run as a batch", mas.getId());
      return false;
    }
    return true;
  }


}
