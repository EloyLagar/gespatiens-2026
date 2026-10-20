package com.viller.gespatiens.interventions.infrastructure.web;

import com.viller.gespatiens.interventions.domain.InterventionNotFoundException;
import com.viller.gespatiens.interventions.domain.InterventionNotManageableException;
import com.viller.gespatiens.interventions.domain.NotATutorException;
import com.viller.gespatiens.interventions.domain.ResidentNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Traduce las excepciones de dominio de las intervenciones a respuestas HTTP.
// Los errores genéricos (400) están en GlobalExceptionHandler
@RestControllerAdvice
public class InterventionExceptionHandler {

    @ExceptionHandler({InterventionNotFoundException.class, ResidentNotFoundException.class})
    public ProblemDetail handleNotFound(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({NotATutorException.class, InterventionNotManageableException.class})
    public ProblemDetail handleForbidden(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }
}
