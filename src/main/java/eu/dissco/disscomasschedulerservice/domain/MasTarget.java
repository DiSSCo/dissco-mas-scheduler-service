package eu.dissco.disscomasschedulerservice.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public record MasTarget(
    @JsonProperty("object")
    JsonNode targetObject,
    String jobId,
    Boolean batchingRequested
) {

}
