package com.viller.gespatiens.notices.infrastructure.web;

import com.viller.gespatiens.notices.domain.NoticeNotFoundException;
import com.viller.gespatiens.notices.domain.NoticeNotManageableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Traduce las excepciones de dominio de avisos a respuestas HTTP.
// Los errores genéricos (400) están en GlobalExceptionHandler
@RestControllerAdvice
public class NoticeExceptionHandler {

    @ExceptionHandler(NoticeNotFoundException.class)
    public ProblemDetail handleNotFound(NoticeNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(NoticeNotManageableException.class)
    public ProblemDetail handleForbidden(NoticeNotManageableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }
}
