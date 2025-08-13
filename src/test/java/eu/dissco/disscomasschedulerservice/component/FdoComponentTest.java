package eu.dissco.disscomasschedulerservice.component;


import static eu.dissco.disscomasschedulerservice.TestUtils.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

class FdoComponentTest {

  @Test
  void testGetRequest() throws Exception {
    // Given
    var fdoComponent = new FdoRecordComponent(MAPPER);

    // When
    var result = fdoComponent.getPostRequest();

    // Then
    assertThat(result).isEqualTo(givenHandleRequest());
  }

  private static JsonNode givenHandleRequest() throws Exception {
    return MAPPER.readTree("""
        {
          "data" : {
            "type" : "https://doi.org/21.T11148/532ce6796e2828dd2be6",
            "attributes" : {
            }
          }
        }
        """);
  }

}
