package com.viller.gespatiens.diary.infrastructure.web;

import com.viller.gespatiens.diary.domain.ReductionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Traduce las excepciones de dominio del diario a respuestas HTTP.
// Los errores genéricos (400) están en GlobalExceptionHandler
@RestControllerAdvice
public class DiaryExceptionHandler {

    @ExceptionHandler(ReductionNotFoundException.class)
    public ProblemDetail handleNotFound(ReductionNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}
