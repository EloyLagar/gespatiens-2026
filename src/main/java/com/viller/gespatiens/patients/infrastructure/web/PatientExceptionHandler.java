package com.viller.gespatiens.patients.infrastructure.web;

import com.viller.gespatiens.patients.domain.CenterFullException;
import com.viller.gespatiens.patients.domain.PatientAlreadyRegisteredException;
import com.viller.gespatiens.patients.domain.VisitCodeAlreadyInUseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Traduce las excepciones de dominio de residentes a respuestas HTTP.
// Los errores genéricos (400) están en GlobalExceptionHandler
@RestControllerAdvice
public class PatientExceptionHandler {

    @ExceptionHandler({PatientAlreadyRegisteredException.class, VisitCodeAlreadyInUseException.class,
            CenterFullException.class})
    public ProblemDetail handleConflict(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }
}
