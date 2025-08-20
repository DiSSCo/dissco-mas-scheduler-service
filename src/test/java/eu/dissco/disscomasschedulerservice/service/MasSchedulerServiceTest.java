package eu.dissco.disscomasschedulerservice.service;

import static eu.dissco.disscomasschedulerservice.TestUtils.AGENT_ID;
import static eu.dissco.disscomasschedulerservice.TestUtils.HANDLE;
import static eu.dissco.disscomasschedulerservice.TestUtils.JOB_ID;
import static eu.dissco.disscomasschedulerservice.TestUtils.MAPPER;
import static eu.dissco.disscomasschedulerservice.TestUtils.MAS_ID;
import static eu.dissco.disscomasschedulerservice.TestUtils.MAS_ID_ALT;
import static eu.dissco.disscomasschedulerservice.TestUtils.TARGET_ID;
import static eu.dissco.disscomasschedulerservice.TestUtils.TARGET_ID_ALT;
import static eu.dissco.disscomasschedulerservice.TestUtils.givenDigitalMedia;
import static eu.dissco.disscomasschedulerservice.TestUtils.givenDigitalSpecimen;
import static eu.dissco.disscomasschedulerservice.TestUtils.givenFiltersDigitalMedia;
import static eu.dissco.disscomasschedulerservice.TestUtils.givenFiltersDigitalSpecimen;
import static eu.dissco.disscomasschedulerservice.TestUtils.givenMas;
import static eu.dissco.disscomasschedulerservice.TestUtils.givenMasJobRequest;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.dissco.backend.schema.OdsHasTargetDigitalObjectFilter;
import eu.dissco.disscomasschedulerservice.Profiles;
import eu.dissco.disscomasschedulerservice.database.jooq.enums.MjrTargetType;
import eu.dissco.disscomasschedulerservice.domain.MasJobRequest;
import eu.dissco.disscomasschedulerservice.domain.MasTarget;
import eu.dissco.disscomasschedulerservice.exception.InvalidRequestException;
import eu.dissco.disscomasschedulerservice.exception.NotFoundException;
import eu.dissco.disscomasschedulerservice.repository.DigitalMediaRepository;
import eu.dissco.disscomasschedulerservice.repository.DigitalSpecimenRepository;
import eu.dissco.disscomasschedulerservice.repository.MasJobRecordRepository;
import eu.dissco.disscomasschedulerservice.repository.MasRepository;
import eu.dissco.disscomasschedulerservice.web.HandleComponent;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

@ExtendWith(MockitoExtension.class)
class MasSchedulerServiceTest {

  private MasSchedulerService masSchedulerService;
  @Mock
  private MasRepository masRepository;
  @Mock
  private MasJobRecordRepository masJobRecordRepository;
  @Mock
  private RabbitMqPublisherService publisherService;
  @Mock
  private HandleComponent handleComponent;
  @Mock
  private DigitalSpecimenRepository specimenRepository;
  @Mock
  private DigitalMediaRepository mediaRepository;
  @Mock
  private Environment environment;


  @BeforeEach
  void init() {
    masSchedulerService = new MasSchedulerService(masRepository, masJobRecordRepository,
        publisherService, handleComponent, specimenRepository, mediaRepository, environment);
  }

  @Test
  void testScheduleMas() throws Exception {
    // Given
    var masJobRequests = Set.of(
        givenMasJobRequest(),
        givenMasJobRequest(MAS_ID, TARGET_ID_ALT),
        givenMasJobRequest(MAS_ID_ALT, TARGET_ID),
        givenMasJobRequest(MAS_ID_ALT, TARGET_ID_ALT)
    );
    given(masRepository.getMasRecords(Set.of(MAS_ID, MAS_ID_ALT))).willReturn(
        List.of(givenMas(MAS_ID), givenMas(
            MAS_ID_ALT)));
    var handles = List.of(
        HANDLE + "/job-1",
        HANDLE + "/job-2",
        HANDLE + "/job-3",
        HANDLE + "/job-4"
    );
    given(handleComponent.postHandle(4)).willReturn(handles);
    given(specimenRepository.getSpecimens(anySet())).willReturn(Map.of(
        TARGET_ID, givenDigitalSpecimen(TARGET_ID),
        TARGET_ID_ALT, givenDigitalSpecimen(TARGET_ID_ALT)
    ));

    // When
    masSchedulerService.scheduleMass(masJobRequests);

    // Then
    then(publisherService).should(times(2)).publishMasJob(eq(MAS_ID), any());
    then(publisherService).should(times(2)).publishMasJob(eq(MAS_ID_ALT), any());
    then(masJobRecordRepository).should().createNewMasJobRecord(anyList());
  }

  @ParameterizedTest
  @MethodSource("provideFilters")
  void testScheduleMasDoesntComply(OdsHasTargetDigitalObjectFilter filters) throws Exception {
    // Given
    var digitalSpecimen = MAPPER.readTree("""
            {
              "@id": "https://doi.org/20.5000.1025/111-222-333",
              "@type": "ods:DigitalSpecimen",
              "ods:version": 1,
              "ods:status": "Active",
              "dcterms:modified": "2015/09/02",
              "dcterms:created": "2025-01-28T13:08:42.659Z",
              "ods:fdoType": "https://doi.org/21.T11148/894b1e6cad57e921764e",
              "ods:midsLevel": 1,
              "ods:normalisedPhysicalSpecimenID": "https://data.biodiversitydata.nl/naturalis/specimen/ZMA.INS.1003070",
              "ods:physicalSpecimenID": "https://data.biodiversitydata.nl/naturalis/specimen/ZMA.INS.1003070",
              "dcterms:license": "http://creativecommons.org/licenses/by-nc/4.0/",
              "ods:topicDiscipline": "Botany",
              "ods:hasEvents": [
                {
                  "eco:protocolDescriptions":["Butterfly net"],
                  "ods:hasLocation": {
                    "dwc:country": "Scotland"
                  }
                }
              ]
            }
        """);
    given(specimenRepository.getSpecimens(anySet())).willReturn(Map.of(
        TARGET_ID, digitalSpecimen
    ));
    var masRequest = new MasJobRequest(
        MAS_ID,
        TARGET_ID, false, AGENT_ID, MjrTargetType.DIGITAL_SPECIMEN);
    given(masRepository.getMasRecords(Set.of(MAS_ID))).willReturn(
        List.of(givenMas(MAS_ID, false, filters)));

    // When
    masSchedulerService.scheduleMass(Set.of(masRequest));

    // Then
    then(handleComponent).shouldHaveNoInteractions();
    then(masJobRecordRepository).shouldHaveNoInteractions();
    then(publisherService).shouldHaveNoInteractions();
  }

  @Test
  void testScheduleMasDoesntComplyWeb() throws Exception {
    // Given
    var filters = givenFiltersDigitalSpecimen();
    given(environment.matchesProfiles(Profiles.WEB)).willReturn(true);
    var digitalSpecimen = MAPPER.readTree("""
            {
              "@id": "https://doi.org/20.5000.1025/111-222-333",
              "@type": "ods:DigitalSpecimen",
              "ods:version": 1,
              "ods:status": "Active",
              "dcterms:modified": "2015/09/02",
              "dcterms:created": "2025-01-28T13:08:42.659Z",
              "ods:fdoType": "https://doi.org/21.T11148/894b1e6cad57e921764e",
              "ods:midsLevel": 1,
              "ods:normalisedPhysicalSpecimenID": "https://data.biodiversitydata.nl/naturalis/specimen/ZMA.INS.1003070",
              "ods:physicalSpecimenID": "https://data.biodiversitydata.nl/naturalis/specimen/ZMA.INS.1003070",
              "ods:topicDiscipline": "Botany",
              "ods:hasEvents": [
                {
                  "eco:protocolDescriptions":["Butterfly net"],
                  "ods:hasLocation": {
                    "dwc:country": "Scotland"
                  }
                }
              ]
            }
        """);
    given(specimenRepository.getSpecimens(anySet())).willReturn(Map.of(
        TARGET_ID, digitalSpecimen
    ));
    var masRequest = new MasJobRequest(
        MAS_ID,
        TARGET_ID, false, AGENT_ID, MjrTargetType.DIGITAL_SPECIMEN);
    given(masRepository.getMasRecords(Set.of(MAS_ID))).willReturn(
        List.of(givenMas(MAS_ID, false, filters)));

    // When / Then
    assertThrows(InvalidRequestException.class,
        () -> masSchedulerService.scheduleMass(Set.of(masRequest)));
  }

  @Test
  void testScheduleTargetDoesntExistAsync() throws Exception {
    // Given
    var masRequest = givenMasJobRequest();
    given(masRepository.getMasRecords(Set.of(MAS_ID))).willReturn(List.of(givenMas(MAS_ID)));

    // When
    masSchedulerService.scheduleMass(Set.of(masRequest));

    // Then
    then(handleComponent).shouldHaveNoInteractions();
    then(masJobRecordRepository).shouldHaveNoInteractions();
    then(publisherService).shouldHaveNoInteractions();
  }

  @Test
  void testScheduleTargetDoesntExistWeb() {
    // Given
    var masRequest = givenMasJobRequest();
    given(masRepository.getMasRecords(Set.of(MAS_ID))).willReturn(List.of(givenMas(MAS_ID)));
    given(environment.matchesProfiles(Profiles.WEB)).willReturn(true);

    // When / then
    assertThrowsExactly(NotFoundException.class,
        () -> masSchedulerService.scheduleMass(Set.of(masRequest)));
  }

  @Test
  void testScheduleBatchingDoesntComplyAsync() throws Exception {
    // Given
    var masRequest = new MasJobRequest(
        MAS_ID, TARGET_ID, true, AGENT_ID, MjrTargetType.DIGITAL_SPECIMEN);
    given(masRepository.getMasRecords(Set.of(MAS_ID))).willReturn(List.of(givenMas(MAS_ID)));
    given(specimenRepository.getSpecimens(anySet())).willReturn(Map.of(
        TARGET_ID, givenDigitalSpecimen(TARGET_ID)
    ));
    given(environment.matchesProfiles(Profiles.WEB)).willReturn(false);

    // When
    masSchedulerService.scheduleMass(Set.of(masRequest));

    // Then
    then(handleComponent).shouldHaveNoInteractions();
    then(masJobRecordRepository).shouldHaveNoInteractions();
    then(publisherService).shouldHaveNoInteractions();
  }

  @Test
  void testScheduleBatchingDoesntComplyWeb() throws Exception {
    // Given
    var masRequest = new MasJobRequest(
        MAS_ID, TARGET_ID, true, AGENT_ID, MjrTargetType.DIGITAL_SPECIMEN);
    given(masRepository.getMasRecords(Set.of(MAS_ID))).willReturn(List.of(givenMas(MAS_ID)));
    given(specimenRepository.getSpecimens(anySet())).willReturn(Map.of(
        TARGET_ID, givenDigitalSpecimen(TARGET_ID)
    ));
    given(environment.matchesProfiles(Profiles.WEB)).willReturn(true);

    // When / then
    assertThrowsExactly(InvalidRequestException.class,
        () -> masSchedulerService.scheduleMass(Set.of(masRequest)));
  }

  @Test
  void testScheduleMasNotFoundWeb() {
    // Given
    var masRequest = new MasJobRequest(
        MAS_ID, TARGET_ID, true, AGENT_ID, MjrTargetType.DIGITAL_SPECIMEN);
    given(environment.matchesProfiles(Profiles.WEB)).willReturn(true);

    // When / then
    assertThrowsExactly(NotFoundException.class,
        () -> masSchedulerService.scheduleMass(Set.of(masRequest)));
  }

  @Test
  void testScheduleMasNotFoundAsync() throws Exception {
    // Given
    var masRequest = new MasJobRequest(
        MAS_ID, TARGET_ID, true, AGENT_ID, MjrTargetType.DIGITAL_SPECIMEN);
    given(specimenRepository.getSpecimens(anySet())).willReturn(Map.of(
        TARGET_ID, givenDigitalSpecimen(TARGET_ID)
    ));
    given(environment.matchesProfiles(Profiles.WEB)).willReturn(false);

    // When
    masSchedulerService.scheduleMass(Set.of(masRequest));

    // Then
    then(handleComponent).shouldHaveNoInteractions();
    then(masJobRecordRepository).shouldHaveNoInteractions();
    then(publisherService).shouldHaveNoInteractions();
  }

  @Test
  void testScheduleMediaMas() throws Exception {
    // Given
    var masRequest = new MasJobRequest(
        MAS_ID, TARGET_ID, false, AGENT_ID, MjrTargetType.MEDIA_OBJECT
    );
    given(mediaRepository.getMedia(anySet())).willReturn(Map.of(
        TARGET_ID, givenDigitalMedia(TARGET_ID)
    ));
    given(masRepository.getMasRecords(Set.of(MAS_ID))).willReturn(List.of(givenMas(MAS_ID)
        .withOdsHasTargetDigitalObjectFilter(givenFiltersDigitalMedia())));
    given(handleComponent.postHandle(1)).willReturn(List.of(JOB_ID));
    var expected = new MasTarget(
        givenDigitalMedia(TARGET_ID),
        JOB_ID,
        false
    );

    // When
    masSchedulerService.scheduleMass(Set.of(masRequest));

    // Then
    then(publisherService).should().publishMasJob(MAS_ID, expected);
    then(masJobRecordRepository).should().createNewMasJobRecord(anyList());
  }

  @Test
  void testScheduleMasFailed() throws Exception {
    // Given
    var masRequest = givenMasJobRequest();
    given(masRepository.getMasRecords(Set.of(MAS_ID))).willReturn(List.of(givenMas(MAS_ID)));
    given(specimenRepository.getSpecimens(anySet())).willReturn(Map.of(
        TARGET_ID, givenDigitalSpecimen(TARGET_ID)
    ));
    given(handleComponent.postHandle(1)).willReturn(List.of(JOB_ID));
    doThrow(JsonProcessingException.class).when(publisherService).publishMasJob(any(), any());

    // When
    masSchedulerService.scheduleMass(Set.of(masRequest));

    // Then
    then(masJobRecordRepository).should().markMasJobRecordsAsFailed(List.of(JOB_ID));
  }

  private static Stream<Arguments> provideFilters() {
    return Stream.of(
        Arguments.of(givenFiltersDigitalSpecimen()),
        Arguments.of(
            new OdsHasTargetDigitalObjectFilter()
                .withAdditionalProperty("$['ods:fdoType']", List.of("Some Test value"))),
        Arguments.of(new OdsHasTargetDigitalObjectFilter()
            .withAdditionalProperty("$['ods:format']", List.of("application/json"))),
        Arguments.of(new OdsHasTargetDigitalObjectFilter()
            .withAdditionalProperty(
                "$['ods:hasEvents'][*]['ods:hasLocation']['dwc:country']",
                List.of("The Netherlands", "Belgium"))),
        Arguments.of(new OdsHasTargetDigitalObjectFilter()
            .withAdditionalProperty(
                "$['ods:hasEvents'][*]['eco:protocolDescriptions']",
                List.of("Diopsis camera"))),
        Arguments.of(new OdsHasTargetDigitalObjectFilter()
            .withAdditionalProperty("$['ods:hasEvents'][*]['dwc:city']",
                List.of("Rotterdam", "Amsterdam"))),
        Arguments.of(new OdsHasTargetDigitalObjectFilter()
            .withAdditionalProperty("$['omg:someRandomNonExistingKey']", List.of("Nothing")))
    );
  }


}
