package com.viller.gespatiens.interventions.domain;

// La memoria: las intervenciones "únicamente podrán generar los tutores de cada usuario"
public class NotATutorException extends RuntimeException {

    public NotATutorException() {
        super("Solo los tutores del residente pueden registrar sus intervenciones");
    }
}
