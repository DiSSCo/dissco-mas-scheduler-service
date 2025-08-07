package eu.dissco.disscomasschedulerservice.controller;

import static org.assertj.core.api.Assertions.assertThat;

import eu.dissco.disscomasschedulerservice.exception.InvalidRequestException;
import eu.dissco.disscomasschedulerservice.exception.PidCreationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class RestResponseEntityExceptionHandlerTest {

  private RestResponseEntityExceptionHandler exceptionHandler;

  @Test
  void testPidCreationExceptionMessage() {
    // Given

    // When
    var result = exceptionHandler.handlePidCreationException(new PidCreationException(""));

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
  }

  @Test
  void testIllegalArgumentException() {
    // Given

    // When
    var result = exceptionHandler.handleException(new InvalidRequestException(""));

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

}
