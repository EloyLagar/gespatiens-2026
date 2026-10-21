package com.viller.gespatiens.patients.infrastructure.web;

import com.viller.gespatiens.patients.domain.AuthorizedVisitorNotFoundException;
import com.viller.gespatiens.patients.domain.CenterFullException;
import com.viller.gespatiens.patients.domain.PatientAlreadyActiveException;
import com.viller.gespatiens.patients.domain.PatientAlreadyDischargedException;
import com.viller.gespatiens.patients.domain.PatientAlreadyRegisteredException;
import com.viller.gespatiens.patients.domain.PatientNotFoundException;
import com.viller.gespatiens.patients.domain.PatientStillActiveException;
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
            CenterFullException.class, PatientAlreadyDischargedException.class,
            PatientAlreadyActiveException.class, PatientStillActiveException.class})
    public ProblemDetail handleConflict(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler({PatientNotFoundException.class, AuthorizedVisitorNotFoundException.class})
    public ProblemDetail handleNotFound(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}
