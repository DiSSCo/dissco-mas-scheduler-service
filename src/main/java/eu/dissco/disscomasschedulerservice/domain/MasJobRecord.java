package eu.dissco.disscomasschedulerservice.domain;

import eu.dissco.disscomasschedulerservice.database.jooq.enums.JobState;
import eu.dissco.disscomasschedulerservice.database.jooq.enums.MjrTargetType;

public record MasJobRecord(
    String jobId,
    JobState state,
    String masId,
    String targetId,
    MjrTargetType targetType,
    String agentId,
    boolean batchingRequested,
    Integer timeToLive) {

}
