package eu.dissco.disscomasschedulerservice.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Getter
@Component
public class FdoRecordComponent {

  private final JsonNode postRequest;

  public FdoRecordComponent(ObjectMapper mapper) {
    this.postRequest = mapper.createObjectNode()
        .set("data", mapper.createObjectNode()
            .put("type", "https://doi.org/21.T11148/532ce6796e2828dd2be6")
            .set("attributes", mapper.createObjectNode()));
  }

}
