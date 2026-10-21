package com.viller.gespatiens.reports.domain;

// Cerrar y reabrir informes es cosa de la administración ("fecha y firma de la Dirección")
public class ReportNotManageableException extends RuntimeException {

    public ReportNotManageableException() {
        super("Solo la administración puede cerrar o reabrir informes");
    }
}
