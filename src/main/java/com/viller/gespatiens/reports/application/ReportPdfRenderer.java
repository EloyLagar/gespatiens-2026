package com.viller.gespatiens.reports.application;

import java.util.List;

// Puerto de salida: convierte informes en un PDF con el formato del centro. La implementación
// (OpenPdfReportRenderer) vive en infrastructure, para que la aplicación no dependa de la librería
public interface ReportPdfRenderer {
    // Varios informes van uno detrás de otro, cada uno empezando en página nueva (RF2)
    byte[] render(List<PrintableReport> reports);
}
