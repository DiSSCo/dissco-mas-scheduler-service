package eu.dissco.disscomasschedulerservice.domain;

import com.fasterxml.jackson.databind.JsonNode;
import eu.dissco.disscomasschedulerservice.database.jooq.enums.MjrTargetType;

public record MasJobRequestFull(
    String masId,
    String targetId,
    JsonNode targetObject,
    boolean batching,
    String agentId,
    MjrTargetType targetType
) {

}
