package com.viller.gespatiens.diary.infrastructure.web;

import com.viller.gespatiens.diary.domain.DiaryConflictException;
import com.viller.gespatiens.diary.domain.DiaryEntryNotFoundException;
import com.viller.gespatiens.diary.domain.ReductionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Traduce las excepciones de dominio del diario a respuestas HTTP.
// Los errores genéricos (400) están en GlobalExceptionHandler
@RestControllerAdvice
public class DiaryExceptionHandler {

    @ExceptionHandler({ReductionNotFoundException.class, DiaryEntryNotFoundException.class})
    public ProblemDetail handleNotFound(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // RF8: otra persona lo guardó mientras se editaba
    @ExceptionHandler(DiaryConflictException.class)
    public ProblemDetail handleConflict(DiaryConflictException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }
}
