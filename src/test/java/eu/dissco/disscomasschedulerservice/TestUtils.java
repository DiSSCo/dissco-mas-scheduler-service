package eu.dissco.disscomasschedulerservice;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers.DateDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.DateSerializer;
import eu.dissco.backend.schema.Agent;
import eu.dissco.backend.schema.Agent.Type;
import eu.dissco.backend.schema.MachineAnnotationService;
import eu.dissco.backend.schema.MachineAnnotationService.OdsStatus;
import eu.dissco.backend.schema.OdsHasTargetDigitalObjectFilter;
import eu.dissco.backend.schema.SchemaContactPoint;
import eu.dissco.disscomasschedulerservice.configuration.InstantDeserializer;
import eu.dissco.disscomasschedulerservice.configuration.InstantSerializer;
import eu.dissco.disscomasschedulerservice.database.jooq.enums.MjrTargetType;
import eu.dissco.disscomasschedulerservice.domain.MasJobRequest;
import eu.dissco.disscomasschedulerservice.domain.MasTarget;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class TestUtils {

  public static final ObjectMapper MAPPER;
  public static final Instant CREATED = Instant.parse("2022-11-01T09:59:24.00Z");
  public static final String PREFIX = "20.5000.1025";
  public static final String HANDLE = "https://hdl.handle.net/" + PREFIX;
  public static final String DOI = "https://doi.org/";
  public static final String MAS_ID = HANDLE + "/AAA-BBB-CCC";
  public static final String MAS_ID_ALT = HANDLE + "/XXX-YYY-ZZZ";
  public static final String BARE_TARGET_DOI = PREFIX + "/111-222-333";
  public static final String TARGET_ID = DOI + PREFIX + "/111-222-333";
  public static final String BARE_TARGET_ALT_DOI = PREFIX + "/111-222-334";
  public static final String TARGET_ID_ALT = DOI + PREFIX + "/111-222-334";
  public static final String JOB_ID = HANDLE + "/444-555-666";
  public static final String AGENT_ID = HANDLE + "/777-888-999";

  public static final int TTL_DEFAULT = 86400;

  static {
    var mapper = new ObjectMapper().findAndRegisterModules();
    SimpleModule dateModule = new SimpleModule();
    dateModule.addSerializer(Date.class, new DateSerializer());
    dateModule.addDeserializer(Date.class, new DateDeserializer());
    dateModule.addSerializer(Instant.class, new InstantSerializer());
    dateModule.addDeserializer(Instant.class, new InstantDeserializer());
    mapper.registerModule(dateModule);
    mapper.setSerializationInclusion(Include.NON_NULL);
    MAPPER = mapper;
  }


  public static DateTimeFormatter givenDateTimeFormatter() {
    return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").withZone(ZoneOffset.UTC);
  }


  public static MasJobRequest givenMasJobRequest() {
    return givenMasJobRequest(MAS_ID, TARGET_ID);
  }

  public static MasJobRequest givenMasJobRequest(String masId, String targetId) {
    return new MasJobRequest(
        masId,
        targetId,
        false,
        AGENT_ID,
        MjrTargetType.DIGITAL_SPECIMEN
    );
  }

  public static MasTarget givenMasTarget() throws JsonProcessingException {
    return new MasTarget(
        givenDigitalSpecimen(TARGET_ID),
        JOB_ID,
        false
    );
  }

  public static MasTarget givenMasTarget(String targetId, String jobId) throws JsonProcessingException {
    return new MasTarget(
        givenDigitalSpecimen(targetId),
        jobId,
        false
    );
  }

  public static JsonNode givenDigitalSpecimen(String targetId) throws JsonProcessingException {
    return MAPPER.readTree(String.format("""
        {
            "@id":"%s",
            "@type": "ods:DigitalSpecimen",
            "ods:status": "Active",
            "ods:fdoType": "https://doi.org/21.T11148/894b1e6cad57e921764e",
            "ods:version": 1,
            "ods:midsLevel": 1,
            "dcterms:created": "2025-01-28T13:08:42.659Z",
            "dcterms:license": "http://creativecommons.org/licenses/by-nc/4.0/",
            "dcterms:modified": "2015/09/02",
            "ods:topicDiscipline": "Palaeontology",
            "ods:physicalSpecimenID": "https://data.biodiversitydata.nl/naturalis/specimen/ZMA.INS.1003070",
            "ods:normalisedPhysicalSpecimenID": "https://data.biodiversitydata.nl/naturalis/specimen/ZMA.INS.1003070"        }
        """, targetId));
  }

  public static JsonNode givenDigitalMedia(String targetId) throws JsonProcessingException {
    return MAPPER.readTree(String.format("""
        {
            "@id":"%s",
            "@type": "ods:DigitalMedia",
            "ods:status": "Active",
            "dcterms:modified": "2015/09/02",
            "dcterms:created": "2025-01-28T13:08:42.659Z",
            "ods:fdoType": "https://doi.org/21.T11148/bbad8c4e101e8af01115",
            "ac:accessURI":"https://media.com"
        }
        """, targetId));
  }

  public static JsonNode givenPostHandleResponse(int n) throws Exception {
    var baseNode = MAPPER.readTree("""
                {
                    "id": "https://doi.org/20.5000.1025/111-222-333",
                    "type": "handle",
                    "attributes": {
                        "10320/loc": "<locations/>",
                        "fdoProfile": "https://doi.org/21.T11148/64396cf36b976ad08267",
                        "fdoRecordLicense": "https://creativecommons.org/publicdomain/zero/1.0/",
                        "digitalObjectType": "https://doi.org/21.T11148/64396cf36b976ad08267",
                        "digitalObjectName": "FDO-test-basic-type",
                        "pid": "https://hdl.handle.net/TEST/Q4B-Y1C-DSR",
                        "pidIssuer": "https://ror.org/04wxnsj81",
                        "pidIssuerName": "DataCite",
                        "issuedForAgent": "https://ror.org/0566bfb96",
                        "issuedForAgentName": "Naturalis Biodiversity Center",
                        "pidRecordIssueDate": "2023-12-18T13:40:17.983Z",
                        "pidRecordIssueNumber": "1",
                        "structuralType": "digital",
                        "pidStatus": "TEST"
                    },
                    "links": {
                        "self": "https://hdl.handle.net/TEST/ABC-123-XYZ"
                    }
                }
        """);
    var list = Collections.nCopies(n, baseNode);
    var dataNode = MAPPER.createObjectNode().putArray("data").addAll(list);
    return MAPPER.createObjectNode().set("data", dataNode);
  }

  public static MachineAnnotationService givenMas(String id) {
    return givenMas(id, false, givenFiltersDigitalSpecimen());
  }

  public static MachineAnnotationService givenMas(String masId, boolean batching,
      OdsHasTargetDigitalObjectFilter filter) {
    return new MachineAnnotationService()
        .withId(masId)
        .withSchemaIdentifier(masId)
        .withType("ods:MachineAnnotationService")
        .withOdsFdoType("https://doi.org/21.T11148/894b1e6cad57e921764e")
        .withOdsStatus(OdsStatus.ACTIVE)
        .withSchemaVersion(1)
        .withSchemaName("A Machine Annotation Service")
        .withSchemaDescription("A fancy mas making all dreams come true")
        .withSchemaDateCreated(Date.from(CREATED))
        .withSchemaDateModified(Date.from(CREATED))
        .withSchemaCreator(new Agent().withType(Type.PROV_SOFTWARE_AGENT).withId(AGENT_ID))
        .withOdsContainerImage("public.ecr.aws/dissco/fancy-mas")
        .withOdsContainerTag("sha-54289")
        .withOdsHasTargetDigitalObjectFilter(filter)
        .withSchemaCreativeWorkStatus("Definitely production ready")
        .withSchemaCodeRepository("https://github.com/DiSSCo/fancy-mas")
        .withSchemaProgrammingLanguage("Java")
        .withOdsServiceAvailability("public")
        .withSchemaMaintainer(new Agent().withType(Type.SCHEMA_SOFTWARE_APPLICATION).withId(AGENT_ID))
        .withSchemaLicense("https://www.apache.org/licenses/LICENSE-2.0")
        .withSchemaContactPoint(new SchemaContactPoint().withSchemaEmail("dontmail@dissco.eu"))
        .withOdsSlaDocumentation("https://www.know.dissco.tech/no_sla")
        .withOdsTopicName(masId)
        .withOdsBatchingPermitted(batching)
        .withOdsTimeToLive(TTL_DEFAULT);
  }

  public static OdsHasTargetDigitalObjectFilter givenFiltersDigitalMedia() {
    return new OdsHasTargetDigitalObjectFilter()
        .withAdditionalProperty("$['ods:fdoType']",
            List.of("https://doi.org/21.T11148/bbad8c4e101e8af01115"));
  }

  public static OdsHasTargetDigitalObjectFilter givenFiltersDigitalSpecimen() {
    return new OdsHasTargetDigitalObjectFilter()
        .withAdditionalProperty("$['dcterms:license']",
            List.of("http://creativecommons.org/licenses/by/4.0/legalcode",
                "http://creativecommons.org/licenses/by-nc/4.0/"))
        .withAdditionalProperty("$['ods:topicDiscipline']", List.of("Palaeontology"))
        .withAdditionalProperty("$['ods:midsLevel']", List.of(0, 1));
  }


  private TestUtils() {
    // Utility Class
  }

}
