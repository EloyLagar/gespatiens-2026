package com.viller.gespatiens.reports.domain;

// RF8: un informe cerrado es el documento entregado. Para corregirlo, la administración lo reabre
public class ReportClosedException extends RuntimeException {

    public ReportClosedException() {
        super("El informe está cerrado: la administración tiene que reabrirlo para modificarlo");
    }
}
