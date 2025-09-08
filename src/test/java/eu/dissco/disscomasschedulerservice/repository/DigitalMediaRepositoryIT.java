package eu.dissco.disscomasschedulerservice.repository;

import static eu.dissco.disscomasschedulerservice.TestUtils.CREATED;
import static eu.dissco.disscomasschedulerservice.TestUtils.MAPPER;
import static eu.dissco.disscomasschedulerservice.TestUtils.TARGET_ID;
import static eu.dissco.disscomasschedulerservice.TestUtils.TARGET_ID_WITH_PROXY;
import static eu.dissco.disscomasschedulerservice.TestUtils.givenDateTimeFormatter;
import static eu.dissco.disscomasschedulerservice.TestUtils.givenDigitalMedia;
import static eu.dissco.disscomasschedulerservice.database.jooq.Tables.DIGITAL_MEDIA_OBJECT;
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

class DigitalMediaRepositoryIT extends BaseRepositoryIT {

  private final DateTimeFormatter formatter = givenDateTimeFormatter();
  private DigitalMediaRepository mediaRepository;

  @BeforeEach
  void setup() {
    mediaRepository = new DigitalMediaRepository(context, MAPPER, formatter);
  }

  @AfterEach
  void destroy() {
    context.truncate(DIGITAL_MEDIA_OBJECT).execute();
  }


  @Test
  void getDigitalMedia() throws JsonProcessingException {
    // Given
    insertIntoDatabase();
    var media = (ObjectNode) givenDigitalMedia(TARGET_ID);
    media
        .put("@id", TARGET_ID_WITH_PROXY)
        .put("dcterms:identifier", TARGET_ID_WITH_PROXY)
        .put("dcterms:created", formatter.format(CREATED))
        .put("ods:version", 1);
    var expected = Map.of(TARGET_ID_WITH_PROXY, media);

    // When
    var result = mediaRepository.getMedia(Set.of(TARGET_ID));

    // Then
    assertThat(result).isEqualTo(expected);
  }

  private void insertIntoDatabase() throws JsonProcessingException {
    context.insertInto(DIGITAL_MEDIA_OBJECT)
        .set(DIGITAL_MEDIA_OBJECT.ID, TARGET_ID)
        .set(DIGITAL_MEDIA_OBJECT.VERSION, 1)
        .set(DIGITAL_MEDIA_OBJECT.TYPE, "ods:DigitalMeda")
        .set(DIGITAL_MEDIA_OBJECT.CREATED, CREATED)
        .set(DIGITAL_MEDIA_OBJECT.LAST_CHECKED, CREATED)
        .set(DIGITAL_MEDIA_OBJECT.MEDIA_URL, "https://media.com")
        .set(DIGITAL_MEDIA_OBJECT.ORIGINAL_DATA, JSONB.jsonb(
            MAPPER.writeValueAsString(givenDigitalMedia(TARGET_ID))))
        .set(DIGITAL_MEDIA_OBJECT.DATA, JSONB.jsonb(
            MAPPER.writeValueAsString(givenDigitalMedia(TARGET_ID))))
        .execute();
  }
}
