package com.viller.gespatiens.reports.infrastructure.web;

import com.viller.gespatiens.reports.domain.AreaNotWritableException;
import com.viller.gespatiens.reports.domain.HeaderConflictException;
import com.viller.gespatiens.reports.domain.ReportClosedException;
import com.viller.gespatiens.reports.domain.ReportNotFoundException;
import com.viller.gespatiens.reports.domain.ReportNotManageableException;
import com.viller.gespatiens.reports.domain.ResidentNotFoundException;
import com.viller.gespatiens.reports.domain.SectionConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Traduce las excepciones de dominio de los informes a respuestas HTTP.
// Los errores genéricos (400) están en GlobalExceptionHandler
@RestControllerAdvice
public class ReportExceptionHandler {

    @ExceptionHandler({ReportNotFoundException.class, ResidentNotFoundException.class})
    public ProblemDetail handleNotFound(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({AreaNotWritableException.class, ReportNotManageableException.class})
    public ProblemDetail handleForbidden(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler({ReportClosedException.class, HeaderConflictException.class})
    public ProblemDetail handleConflict(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    // RF8: además del mensaje, lo que hay ahora en el apartado (text null si lo vaciaron) y su versión,
    // para que quien editaba compare y decida si guarda la suya encima
    @ExceptionHandler(SectionConflictException.class)
    public ProblemDetail handleSectionConflict(SectionConflictException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setProperty("section", ex.getSection().name());
        problem.setProperty("currentText", ex.getCurrent().map(entry -> entry.text()).orElse(null));
        problem.setProperty("currentVersion", ex.getCurrent().map(entry -> entry.version()).orElse(0));
        return problem;
    }
}
