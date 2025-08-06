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
import static eu.dissco.disscomasschedulerservice.TestUtils.givenMas;
import static eu.dissco.disscomasschedulerservice.TestUtils.givenMasJobRequest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import eu.dissco.disscomasschedulerservice.domain.MasJobRequest;
import eu.dissco.disscomasschedulerservice.domain.MasTarget;
import eu.dissco.disscomasschedulerservice.repository.MasJobRecordRepository;
import eu.dissco.disscomasschedulerservice.repository.MasRepository;
import eu.dissco.disscomasschedulerservice.web.HandleComponent;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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


  @BeforeEach
  void init() {
    masSchedulerService = new MasSchedulerService(masRepository, masJobRecordRepository, publisherService, handleComponent);
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
    given(masRepository.getMasRecords(Set.of(MAS_ID, MAS_ID_ALT))).willReturn(List.of(givenMas(MAS_ID), givenMas(
        MAS_ID_ALT)));
    var handles = List.of(
        HANDLE + "/job-1",
        HANDLE + "/job-2",
        HANDLE + "/job-3",
        HANDLE + "/job-4"
    );
    given(handleComponent.postHandle(4)).willReturn(handles);

    // When
    masSchedulerService.scheduleMass(masJobRequests);

    // Then
    then(publisherService).should(times(2)).publishMasJob(eq(MAS_ID), any());
    then(publisherService).should(times(2)).publishMasJob(eq(MAS_ID_ALT), any());
    then(masJobRecordRepository).should().createNewMasJobRecord(anyList());
  }

  @Test
  void testScheduleMasDoesntComply() throws Exception {
    // Given
    var masRequest = new MasJobRequest(
        MAS_ID,
        MAPPER.readTree("""
             {
            "@id":"https://doi.org/20.5000.1025/111-222-333",
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
            "ods:topicDiscipline": "Botany"
        }"""), false, AGENT_ID);
    given(masRepository.getMasRecords(Set.of(MAS_ID))).willReturn(List.of(givenMas(MAS_ID)));

    // When
    masSchedulerService.scheduleMass(Set.of(masRequest));

    // Then
    then(handleComponent).shouldHaveNoInteractions();
    then(masJobRecordRepository).shouldHaveNoInteractions();
    then(publisherService).shouldHaveNoInteractions();
  }

  @Test
  void testScheduleBatchingDoesntComply() throws Exception {
    // Given
    var masRequest = new MasJobRequest(
        MAS_ID,
        givenDigitalSpecimen(TARGET_ID), true, AGENT_ID);
    given(masRepository.getMasRecords(Set.of(MAS_ID))).willReturn(List.of(givenMas(MAS_ID)));

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
        MAS_ID, givenDigitalMedia(TARGET_ID), false, AGENT_ID
    );
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




}
