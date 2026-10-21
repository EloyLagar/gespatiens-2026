package com.viller.gespatiens.reports.infrastructure.web;

// DTO de entrada de los informes
public final class ReportRequests {

    private ReportRequests() {
    }

    // version: la que tenía el apartado cuando se abrió para editarlo (0 si estaba vacío). Texto vacío lo vacía
    public record SectionRequest(String text, int version) {
    }

    // Lo que se escribe a mano en la cabecera; las fechas salen de la ficha. nip solo cuenta en el de media
    // estancia y exitReason en el final. version: la que tenía la cabecera cuando se abrió para editarla
    public record HeaderRequest(String requestNumber, String referenceCenter, String nip, String exitReason,
                                int version) {
    }
}
