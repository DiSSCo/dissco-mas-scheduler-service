package eu.dissco.disscomasschedulerservice.domain;


import eu.dissco.disscomasschedulerservice.database.jooq.enums.MjrTargetType;

public record MasJobRequest(
    String masId,
    String targetId,
    boolean batching,
    String agentId,
    MjrTargetType targetType
) {

}
