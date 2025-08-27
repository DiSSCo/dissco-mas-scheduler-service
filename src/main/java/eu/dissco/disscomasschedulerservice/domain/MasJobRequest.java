package eu.dissco.disscomasschedulerservice.domain;


import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import eu.dissco.disscomasschedulerservice.database.jooq.enums.MjrTargetType;
import jakarta.validation.constraints.NotNull;

public record MasJobRequest(
    @NotNull
    @JsonPropertyDescription("Identifier of MAS. May include proxy")
    String masId,
    @NotNull
    String targetId,
    boolean batching,
    String agentId,
    @NotNull
    MjrTargetType targetType
) {

}
