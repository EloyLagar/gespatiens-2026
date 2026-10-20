package com.viller.gespatiens.shared.infrastructure.web;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

// Errores comunes a todos los módulos. Las excepciones propias de cada módulo
// se traducen en su propio handler (UserExceptionHandler, PatientExceptionHandler...)
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Invariantes del dominio rotas (email inválido, campos vacíos, DNI incorrecto...)
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleInvalidData(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // JSON mal formado, valor de enum inexistente, fecha con formato incorrecto...
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "El cuerpo de la petición no es válido: revisa el formato del JSON, "
                        + "los valores de los enums y que las fechas sigan el formato yyyy-MM-dd");
    }

    // Parámetro de la URL con un valor que no encaja: un id que no es un UUID, ?status=OTRO...
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Valor no válido para el parámetro " + ex.getName() + ": " + ex.getValue());
    }

    // Una restricción unique de la BD ha saltado aunque el caso de uso ya lo comprobaba:
    // pasa si dos peticiones simultáneas intentan, p.ej., quedarse con el mismo número de residente
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Conflicto con datos existentes. Vuelve a intentarlo");
    }
}
