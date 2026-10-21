package com.viller.gespatiens.reports.domain;

// Los dos informes que pide el centro (memoria, figuras 9 y 10)
public enum ReportType {
    MID_STAY("Informe de media estancia"),
    FINAL("Informe final de programa");

    private final String title;

    ReportType(String title) {
        this.title = title;
    }

    // El título del documento tal como lo imprime el centro
    public String title() {
        return title;
    }
}
