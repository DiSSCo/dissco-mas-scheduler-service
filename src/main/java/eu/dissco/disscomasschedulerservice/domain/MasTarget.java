package eu.dissco.disscomasschedulerservice.domain;

import com.fasterxml.jackson.databind.JsonNode;

public record MasTarget(
    JsonNode object,
    String jobId,
    Boolean batchingRequested
) {

}
