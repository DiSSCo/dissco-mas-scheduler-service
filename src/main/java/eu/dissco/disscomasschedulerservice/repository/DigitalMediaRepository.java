package eu.dissco.disscomasschedulerservice.repository;

import static eu.dissco.disscomasschedulerservice.database.jooq.Tables.DIGITAL_MEDIA_OBJECT;
import static eu.dissco.disscomasschedulerservice.database.jooq.Tables.DIGITAL_SPECIMEN;
import static eu.dissco.disscomasschedulerservice.repository.RepositoryUtils.DOI_STRING;

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
public class DigitalMediaRepository {

  private final DSLContext context;
  private final ObjectMapper mapper;
  private final DateTimeFormatter formatter;

  public Map<String, JsonNode> getMedia(Set<String> targetIds) {
    return context.select(DIGITAL_MEDIA_OBJECT.asterisk())
        .from(DIGITAL_MEDIA_OBJECT)
        .where(DIGITAL_MEDIA_OBJECT.ID.in(targetIds))
        .and(DIGITAL_MEDIA_OBJECT.DELETED.isNull())
        .fetchMap(DIGITAL_MEDIA_OBJECT.ID, this::mapToDigitalMedia);
  }

  private JsonNode mapToDigitalMedia(Record dbRecord) {
    try {
      var media = (ObjectNode) mapper.readTree(dbRecord.get(DIGITAL_MEDIA_OBJECT.DATA).data());
      media
          .put("@id", DOI_STRING + dbRecord.get(DIGITAL_MEDIA_OBJECT.ID))
          .put("dcterms:identifier", DOI_STRING + dbRecord.get(DIGITAL_SPECIMEN.ID))
          .put("dcterms:created", formatter.format(dbRecord.get(DIGITAL_MEDIA_OBJECT.CREATED)))
          .put("ods:version", dbRecord.get(DIGITAL_MEDIA_OBJECT.VERSION));
      return media;
    } catch (JsonProcessingException e) {
      log.error("Unable to read media {}", dbRecord.get(DIGITAL_MEDIA_OBJECT.ID));
      return null;
    }
  }


}
