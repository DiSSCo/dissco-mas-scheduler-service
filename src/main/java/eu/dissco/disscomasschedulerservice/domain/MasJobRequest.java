package eu.dissco.disscomasschedulerservice.domain;


import eu.dissco.disscomasschedulerservice.database.jooq.enums.MjrTargetType;
import jakarta.validation.constraints.NotNull;

public record MasJobRequest(
    @NotNull
    String masId,
    @NotNull
    String targetId,
    boolean batching,
    String agentId,
    @NotNull
    MjrTargetType targetType
) {

}
