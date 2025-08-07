package eu.dissco.disscomasschedulerservice.controller;

import eu.dissco.disscomasschedulerservice.Profiles;
import eu.dissco.disscomasschedulerservice.domain.ExceptionResponseWrapper;
import eu.dissco.disscomasschedulerservice.exception.InvalidRequestException;
import eu.dissco.disscomasschedulerservice.exception.PidCreationException;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
@Profile(Profiles.WEB)
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

  @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
  @ExceptionHandler(PidCreationException.class)
  public ResponseEntity<ExceptionResponseWrapper> handlePidCreationException(PidCreationException e) {
    var exceptionResponse = new ExceptionResponseWrapper(
        HttpStatus.UNPROCESSABLE_ENTITY,
        "PidCreationException",
        e.getMessage()
    );
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(exceptionResponse);
  }

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(InvalidRequestException.class)
  public ResponseEntity<ExceptionResponseWrapper> handleException(InvalidRequestException e) {
    var exceptionResponse = new ExceptionResponseWrapper(
        HttpStatus.BAD_REQUEST,
        "Invalid request",
        e.getMessage()
    );
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exceptionResponse);
  }



}
