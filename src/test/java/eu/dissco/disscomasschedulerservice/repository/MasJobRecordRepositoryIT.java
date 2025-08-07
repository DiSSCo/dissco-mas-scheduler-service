package eu.dissco.disscomasschedulerservice.repository;

import static eu.dissco.disscomasschedulerservice.TestUtils.AGENT_ID;
import static eu.dissco.disscomasschedulerservice.TestUtils.JOB_ID;
import static eu.dissco.disscomasschedulerservice.TestUtils.MAS_ID;
import static eu.dissco.disscomasschedulerservice.TestUtils.TARGET_ID;
import static eu.dissco.disscomasschedulerservice.TestUtils.TTL_DEFAULT;
import static eu.dissco.disscomasschedulerservice.database.jooq.Tables.MAS_JOB_RECORD;
import static org.assertj.core.api.Assertions.assertThat;

import eu.dissco.disscomasschedulerservice.database.jooq.enums.JobState;
import eu.dissco.disscomasschedulerservice.database.jooq.enums.MjrTargetType;
import eu.dissco.disscomasschedulerservice.domain.MasJobRecord;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MasJobRecordRepositoryIT extends BaseRepositoryIT{

  private MasJobRecordRepository repository;

  @BeforeEach
  void setup() {
    repository = new MasJobRecordRepository(context);
  }

  @AfterEach
  void destroy() {
    context.truncate(MAS_JOB_RECORD).execute();
  }


  @Test
  void testCreateNewMasJobRecord() {
    // Given
    var mjr = new MasJobRecord(JOB_ID, JobState.SCHEDULED, MAS_ID, TARGET_ID,
        MjrTargetType.DIGITAL_SPECIMEN, AGENT_ID, false, TTL_DEFAULT);

    // When
    repository.createNewMasJobRecord(List.of(mjr));
    var result = context.select(MAS_JOB_RECORD.JOB_ID, MAS_JOB_RECORD.JOB_STATE)
        .from(MAS_JOB_RECORD).where(MAS_JOB_RECORD.JOB_ID.eq(JOB_ID)).fetchOne();

    // Then
    assertThat(result.get(MAS_JOB_RECORD.JOB_ID)).isEqualTo(JOB_ID);
    assertThat(result.get(MAS_JOB_RECORD.JOB_STATE)).isEqualTo(JobState.SCHEDULED);
  }

  @Test
  void testMarkMasJobRecordsAsFailed() {
    // Given
    var mjr = new MasJobRecord(JOB_ID, JobState.SCHEDULED, MAS_ID, TARGET_ID,
        MjrTargetType.DIGITAL_SPECIMEN, AGENT_ID, false, TTL_DEFAULT);
    repository.createNewMasJobRecord(List.of(mjr));

    // When
    repository.markMasJobRecordsAsFailed(List.of(JOB_ID));
    var result = context.select(MAS_JOB_RECORD.JOB_ID, MAS_JOB_RECORD.JOB_STATE,
            MAS_JOB_RECORD.TIME_COMPLETED)
        .from(MAS_JOB_RECORD)
        .where(MAS_JOB_RECORD.JOB_ID.eq(JOB_ID))
        .fetchSingle();
    var timestamp = result.get(MAS_JOB_RECORD.TIME_COMPLETED);
    var state = result.get(MAS_JOB_RECORD.JOB_STATE);

    // Then
    assertThat(timestamp).isNotNull();
    assertThat(state).isEqualTo(JobState.FAILED);
  }


}
