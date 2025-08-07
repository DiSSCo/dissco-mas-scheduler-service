package eu.dissco.disscomasschedulerservice.domain;

import com.fasterxml.jackson.databind.JsonNode;

public record MasTarget(
    JsonNode targetObject,
    String jobId,
    Boolean batchingRequested
) {

}
