package com.viller.gespatiens.reports.domain;

// RF8: como SectionConflictException, pero con la cabecera del informe
public class HeaderConflictException extends RuntimeException {

    public HeaderConflictException() {
        super("Otra persona ha modificado la cabecera del informe mientras la editabas. "
                + "Recarga el informe antes de guardar");
    }
}
