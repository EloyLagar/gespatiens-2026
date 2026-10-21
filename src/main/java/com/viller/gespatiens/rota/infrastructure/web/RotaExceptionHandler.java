package com.viller.gespatiens.rota.infrastructure.web;

import com.viller.gespatiens.rota.domain.SwapNotAllowedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Traduce las excepciones del cuadrante a respuestas HTTP. Los errores genéricos (400) están en GlobalExceptionHandler
@RestControllerAdvice
public class RotaExceptionHandler {

    // El cambio de turno no se puede hacer: ya está resuelto, el cuadrante ha cambiado, no es tuyo...
    @ExceptionHandler(SwapNotAllowedException.class)
    public ProblemDetail handleSwap(SwapNotAllowedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }
}
