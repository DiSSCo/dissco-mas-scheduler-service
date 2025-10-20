package eu.dissco.disscomasschedulerservice.repository;

import static eu.dissco.disscomasschedulerservice.TestUtils.AGENT_ID;
import static eu.dissco.disscomasschedulerservice.TestUtils.CREATED;
import static eu.dissco.disscomasschedulerservice.TestUtils.MAPPER;
import static eu.dissco.disscomasschedulerservice.TestUtils.TARGET_ID;
import static eu.dissco.disscomasschedulerservice.TestUtils.TARGET_ID_WITH_PROXY;
import static eu.dissco.disscomasschedulerservice.TestUtils.givenDateTimeFormatter;
import static eu.dissco.disscomasschedulerservice.TestUtils.givenDigitalSpecimen;
import static eu.dissco.disscomasschedulerservice.database.jooq.Tables.DIGITAL_SPECIMEN;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import org.jooq.JSONB;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DigitalSpecimenRepositoryIT extends BaseRepositoryIT {

  private final DateTimeFormatter formatter = givenDateTimeFormatter();
  private DigitalSpecimenRepository specimenRepository;

  @BeforeEach
  void setup() {
    specimenRepository = new DigitalSpecimenRepository(context, MAPPER, formatter);
  }

  @AfterEach
  void destroy() {
    context.truncate(DIGITAL_SPECIMEN).execute();
  }

  @Test
  void getDigitalSpecimen() throws JsonProcessingException {
    // Given
    insertIntoDatabase();
    var specimen = (ObjectNode) givenDigitalSpecimen(TARGET_ID);
    specimen
        .put("@id", TARGET_ID_WITH_PROXY)
        .put("dcterms:identifier", TARGET_ID_WITH_PROXY)
        .put("ods:midsLevel", (short) 1)
        .put("dcterms:created", formatter.format(CREATED))
        .put("ods:version", 1);
    var expected = Map.of(TARGET_ID_WITH_PROXY, specimen);

    // When
    var result = specimenRepository.getSpecimens(Set.of(TARGET_ID));

    // Then
    assertThat(result).isEqualTo(expected);
  }

  private void insertIntoDatabase() throws JsonProcessingException {
    context.insertInto(DIGITAL_SPECIMEN)
        .set(DIGITAL_SPECIMEN.ID, TARGET_ID)
        .set(DIGITAL_SPECIMEN.VERSION, 1)
        .set(DIGITAL_SPECIMEN.TYPE, "ods:DigitalSpecimen")
        .set(DIGITAL_SPECIMEN.MIDSLEVEL, (short) 1)
        .set(DIGITAL_SPECIMEN.PHYSICAL_SPECIMEN_ID, "Some_phys_id")
        .set(DIGITAL_SPECIMEN.PHYSICAL_SPECIMEN_TYPE, "ods:DigitalSpecimen")
        .set(DIGITAL_SPECIMEN.SPECIMEN_NAME, "Some_phys_id")
        .set(DIGITAL_SPECIMEN.ORGANIZATION_ID, AGENT_ID)
        .set(DIGITAL_SPECIMEN.SOURCE_SYSTEM_ID, AGENT_ID)
        .set(DIGITAL_SPECIMEN.CREATED, CREATED)
        .set(DIGITAL_SPECIMEN.LAST_CHECKED, CREATED)
        .set(DIGITAL_SPECIMEN.DATA, JSONB.jsonb(
            MAPPER.writeValueAsString(givenDigitalSpecimen(TARGET_ID))))
        .execute();
  }


}
