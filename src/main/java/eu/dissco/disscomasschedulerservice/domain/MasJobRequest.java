package eu.dissco.disscomasschedulerservice.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record MasJobRequest(
    List<String> masIds,
    JsonNode target,
    boolean batching
) {

}
