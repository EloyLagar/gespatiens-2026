package com.viller.gespatiens.reports.domain;

// RF8: "la aplicación debe controlar qué información está siendo modificada". Un informe en elaboración
// se puede redactar; uno cerrado es el documento entregado y no cambia hasta que la administración lo reabra
public enum ReportState {
    DRAFT,
    CLOSED
}
