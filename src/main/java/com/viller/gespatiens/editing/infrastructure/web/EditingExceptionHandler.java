package com.viller.gespatiens.editing.infrastructure.web;

import com.viller.gespatiens.editing.domain.LockRaceException;
import com.viller.gespatiens.editing.domain.ResourceBeingEditedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// RF8: "lo está editando otra persona". Lo lanzan tanto el controlador de bloqueos como los guardados de los
// informes y del diario, así que va en un handler propio y no en el de un módulo
@RestControllerAdvice
public class EditingExceptionHandler {

    // Además del mensaje, quién y desde cuándo, para que la pantalla lo muestre en su idioma
    @ExceptionHandler(ResourceBeingEditedException.class)
    public ProblemDetail handleBeingEdited(ResourceBeingEditedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setProperty("beingEditedBy", ex.getHolderName());
        problem.setProperty("since", ex.getSince() == null ? null : ex.getSince().toString());
        return problem;
    }

    @ExceptionHandler(LockRaceException.class)
    public ProblemDetail handleRace(LockRaceException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }
}
