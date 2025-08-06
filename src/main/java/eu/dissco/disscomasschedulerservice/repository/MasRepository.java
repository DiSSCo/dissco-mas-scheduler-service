package eu.dissco.disscomasschedulerservice.repository;

import static eu.dissco.disscomasschedulerservice.database.jooq.Tables.MACHINE_ANNOTATION_SERVICE;
import static eu.dissco.disscomasschedulerservice.repository.RepositoryUtils.HANDLE_STRING;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.backend.schema.MachineAnnotationService;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record1;
import org.jooq.exception.DataAccessException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MasRepository {

  private final DSLContext context;
  private final ObjectMapper mapper;

  public List<MachineAnnotationService> getMasRecords(Set<String> mass) {
    var massIds = mass.stream().map(this::removeProxy).toList();
    return context.select(MACHINE_ANNOTATION_SERVICE.DATA)
        .from(MACHINE_ANNOTATION_SERVICE)
        .where(MACHINE_ANNOTATION_SERVICE.ID.in(massIds))
        .and(MACHINE_ANNOTATION_SERVICE.TOMBSTONED.isNull())
        .fetch(this::mapToMas);
  }

  private MachineAnnotationService mapToMas(Record1<JSONB> record1) {
    try {
      return mapper.readValue(record1.get(MACHINE_ANNOTATION_SERVICE.DATA).data(),
          MachineAnnotationService.class);
    } catch (JsonProcessingException e) {
      throw new DataAccessException("Unable to convert jsonb to machine annotation service",
          e);
    }
  }

  private String removeProxy(String id) {
    return id.replace("urn:uuid:", "")
        .replace(HANDLE_STRING, "");
  }

}
