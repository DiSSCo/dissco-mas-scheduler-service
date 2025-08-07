package eu.dissco.disscomasschedulerservice.domain;

import com.fasterxml.jackson.databind.JsonNode;

public record MasJobRequest(
    String masId,
    JsonNode targetObject,
    boolean batching,
    String agentId
) {

}
