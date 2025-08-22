package eu.dissco.disscomasschedulerservice.repository;

import static eu.dissco.disscomasschedulerservice.database.jooq.Tables.MAS_JOB_RECORD;
import static eu.dissco.disscomasschedulerservice.repository.RepositoryUtils.DOI_STRING;

import eu.dissco.disscomasschedulerservice.database.jooq.enums.JobState;
import eu.dissco.disscomasschedulerservice.domain.MasJobRecord;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MasJobRecordRepository {

  private final DSLContext context;

  public void markMasJobRecordsAsFailed(List<String> ids) {
    context.update(MAS_JOB_RECORD)
        .set(MAS_JOB_RECORD.JOB_STATE, JobState.FAILED)
        .set(MAS_JOB_RECORD.TIME_COMPLETED, Instant.now())
        .where(MAS_JOB_RECORD.JOB_ID.in(ids))
        .execute();
  }

  public void createNewMasJobRecord(List<MasJobRecord> masJobRecord) {
    var queries = masJobRecord.stream().map(this::mjrToQuery).toList();
    context.batch(queries).execute();
  }

  private Query mjrToQuery(MasJobRecord masJobRecord) {
    var now = Instant.now();
    var ttl = now.plusSeconds(masJobRecord.timeToLive());
    return context.insertInto(MAS_JOB_RECORD)
        .set(MAS_JOB_RECORD.JOB_ID, masJobRecord.jobId())
        .set(MAS_JOB_RECORD.JOB_STATE, masJobRecord.state())
        .set(MAS_JOB_RECORD.MAS_ID, masJobRecord.masId())
        .set(MAS_JOB_RECORD.CREATOR, masJobRecord.agentId())
        .set(MAS_JOB_RECORD.TARGET_ID, masJobRecord.targetId())
        .set(MAS_JOB_RECORD.TARGET_TYPE, masJobRecord.targetType())
        .set(MAS_JOB_RECORD.TIME_STARTED, now)
        .set(MAS_JOB_RECORD.BATCHING_REQUESTED, masJobRecord.batchingRequested())
        .set(MAS_JOB_RECORD.EXPIRES_ON, ttl);
  }



}
