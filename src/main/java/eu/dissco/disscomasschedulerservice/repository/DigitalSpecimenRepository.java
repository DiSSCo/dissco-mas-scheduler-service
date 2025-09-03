package eu.dissco.disscomasschedulerservice.repository;

import static eu.dissco.disscomasschedulerservice.database.jooq.Tables.DIGITAL_SPECIMEN;
import static eu.dissco.disscomasschedulerservice.service.ProxyUtils.DOI_STRING;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
@RequiredArgsConstructor
public class DigitalSpecimenRepository {

  private final DSLContext context;
  private final ObjectMapper mapper;
  private final DateTimeFormatter formatter;

  public Map<String, JsonNode> getSpecimens(Set<String> targetIds) {
    return context.select(DIGITAL_SPECIMEN.asterisk())
        .from(DIGITAL_SPECIMEN)
        .where(DIGITAL_SPECIMEN.ID.in(targetIds))
        .and(DIGITAL_SPECIMEN.DELETED.isNull())
        .fetchMap(this::getKey, this::mapToDigitalSpecimen);
  }

  private String getKey(Record dbRecord) {
    return DOI_STRING + dbRecord.get(DIGITAL_SPECIMEN.ID);
  }

  private JsonNode mapToDigitalSpecimen(Record dbRecord) {
    try {
      var specimen = (ObjectNode) mapper.readTree(dbRecord.get(DIGITAL_SPECIMEN.DATA).data());
      specimen
          .put("@id", DOI_STRING + dbRecord.get(DIGITAL_SPECIMEN.ID))
          .put("dcterms:identifier", DOI_STRING + dbRecord.get(DIGITAL_SPECIMEN.ID))
          .put("ods:midsLevel", dbRecord.get(DIGITAL_SPECIMEN.MIDSLEVEL))
          .put("dcterms:created", formatter.format(dbRecord.get(DIGITAL_SPECIMEN.CREATED)))
          .put("ods:version", dbRecord.get(DIGITAL_SPECIMEN.VERSION));
      return specimen;
    } catch (JsonProcessingException e) {
      log.error("Unable to read specimen {}", dbRecord.get(DIGITAL_SPECIMEN.ID));
      return null;
    }
  }

}
