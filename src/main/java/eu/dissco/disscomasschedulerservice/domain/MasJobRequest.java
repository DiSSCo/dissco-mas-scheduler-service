package eu.dissco.disscomasschedulerservice.domain;

import com.fasterxml.jackson.databind.JsonNode;
import eu.dissco.disscomasschedulerservice.database.jooq.enums.MjrTargetType;

public record MasJobRequest(
    String masId,
    JsonNode targetObject,
    boolean batching,
    String agentId
) {

}
