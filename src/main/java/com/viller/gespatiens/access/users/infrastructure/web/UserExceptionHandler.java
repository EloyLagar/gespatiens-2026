package com.viller.gespatiens.access.users.infrastructure.web;

import com.viller.gespatiens.access.users.domain.EmailAlreadyRegisteredException;
import com.viller.gespatiens.access.users.domain.PhoneNumberAlreadyRegisteredException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Traduce las excepciones de dominio de usuarios a respuestas HTTP (formato estándar ProblemDetail, RFC 9457).
// Los errores genéricos (400) están en GlobalExceptionHandler
@RestControllerAdvice
public class UserExceptionHandler {

    @ExceptionHandler({EmailAlreadyRegisteredException.class, PhoneNumberAlreadyRegisteredException.class})
    public ProblemDetail handleDuplicate(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }
}
