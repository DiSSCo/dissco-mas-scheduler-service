package eu.dissco.disscomasschedulerservice.repository;

import static eu.dissco.disscomasschedulerservice.TestUtils.MAPPER;
import static eu.dissco.disscomasschedulerservice.TestUtils.givenMas;
import static eu.dissco.disscomasschedulerservice.database.jooq.Tables.MACHINE_ANNOTATION_SERVICE;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.dissco.backend.schema.MachineAnnotationService;
import java.util.Set;
import org.jooq.JSONB;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MasRepositoryIT extends BaseRepositoryIT {

  private MasRepository repository;

  @BeforeEach
  void setup() {
    repository = new MasRepository(context, MAPPER);
  }

  @AfterEach
  void destroy() {
    context.truncate(MACHINE_ANNOTATION_SERVICE).execute();
  }

  @Test
  void testGetMasRecords() throws JsonProcessingException {
    // Given
    populateDatabase();

    // When
    var result = repository.getMasRecords(Set.of("1", "2"));

    // Then
    assertThat(result).hasSize(2);
  }

  private void populateDatabase() throws JsonProcessingException {
    for (int i =0; i < 4; i++){
      createRecord(givenMas(String.valueOf(i)));
    }

  }

  private void createRecord(MachineAnnotationService mas) throws JsonProcessingException {
    context.insertInto(MACHINE_ANNOTATION_SERVICE)
        .set(MACHINE_ANNOTATION_SERVICE.ID, mas.getId())
        .set(MACHINE_ANNOTATION_SERVICE.VERSION, mas.getSchemaVersion())
        .set(MACHINE_ANNOTATION_SERVICE.NAME, mas.getSchemaName())
        .set(MACHINE_ANNOTATION_SERVICE.CREATED, mas.getSchemaDateCreated().toInstant())
        .set(MACHINE_ANNOTATION_SERVICE.MODIFIED, mas.getSchemaDateModified().toInstant())
        .set(MACHINE_ANNOTATION_SERVICE.CREATOR, mas.getSchemaCreator().getId())
        .set(MACHINE_ANNOTATION_SERVICE.CONTAINER_IMAGE, mas.getOdsContainerImage())
        .set(MACHINE_ANNOTATION_SERVICE.CONTAINER_IMAGE_TAG, mas.getOdsContainerTag())
        .set(MACHINE_ANNOTATION_SERVICE.CREATIVE_WORK_STATE,
            mas.getSchemaCreativeWorkStatus())
        .set(MACHINE_ANNOTATION_SERVICE.SERVICE_AVAILABILITY,
            mas.getOdsServiceAvailability())
        .set(MACHINE_ANNOTATION_SERVICE.SOURCE_CODE_REPOSITORY,
            mas.getSchemaCodeRepository())
        .set(MACHINE_ANNOTATION_SERVICE.CODE_MAINTAINER, mas.getSchemaMaintainer().getId())
        .set(MACHINE_ANNOTATION_SERVICE.CODE_LICENSE, mas.getSchemaLicense())
        .set(MACHINE_ANNOTATION_SERVICE.BATCHING_PERMITTED, mas.getOdsBatchingPermitted())
        .set(MACHINE_ANNOTATION_SERVICE.TIME_TO_LIVE, mas.getOdsTimeToLive())
        .set(MACHINE_ANNOTATION_SERVICE.TOMBSTONED,
            mas.getOdsHasTombstoneMetadata() != null ? mas.getOdsHasTombstoneMetadata()
                .getOdsTombstoneDate().toInstant() : null)
        .set(MACHINE_ANNOTATION_SERVICE.DATA, mapToJSONB(mas))
        .execute();
  }

  private JSONB mapToJSONB(MachineAnnotationService mas) throws JsonProcessingException {
    return JSONB.valueOf(MAPPER.writeValueAsString(mas));
  }


}
